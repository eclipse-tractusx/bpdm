/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.gate.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.IBaseStateDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.exception.BpdmNullMappingException
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.SharableRelationType
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.config.GoldenRecordTaskConfigProperties
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.entity.generic.*
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.*
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.bpdm.gate.util.BusinessPartnerCopyUtil
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.*
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntitySearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.request.SiteSearchRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.reflect.KProperty

@Service
class GoldenRecordUpdateBatchService(
    private val goldenRecordUpdateService: GoldenRecordUpdateChunkService
){
    private val logger = KotlinLogging.logger { }

    fun updateOutputOnGoldenRecordChange(){
        logger.info { "Update Business Partner Output based on Golden Record Updates from the Pool..." }

        var totalLegalEntitiesUpdated = 0
        var totalSitesUpdated = 0
        var totalAddressesUpdated = 0
        do {
            val stats = goldenRecordUpdateService.updateFromNextChunk()

            totalLegalEntitiesUpdated += stats.updatedLegalEntities
            totalSitesUpdated += stats.updatedSites
            totalAddressesUpdated += stats.updatedAddresses
        }while (stats.foundChangelogEntries != 0)

        logger.debug { "In total updated '$totalLegalEntitiesUpdated' legal entities, '$totalSitesUpdated' sites and '$totalAddressesUpdated' addresses." }
    }
}

@Service
class GoldenRecordUpdateChunkService(
    private val poolClient: PoolApiClient,
    private val syncRecordService: SyncRecordService,
    private val taskConfigProperties: GoldenRecordTaskConfigProperties,
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val copyUtil: BusinessPartnerCopyUtil,
    private val businessPartnerService: BusinessPartnerService
) {

    private val logger = KotlinLogging.logger { }

    private val placeholderTime = OffsetDateTime.of(2025, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC).toInstant()


    @Transactional
    fun updateFromNextChunk(): UpdateStats{
        logger.info { "Update next chunk of Business Partner Output based on Golden Record Updates from the Pool..." }

        val syncRecord = syncRecordService.setSynchronizationStart(SyncTypeDb.POOL_TO_GATE_OUTPUT)

        val changelogSearchRequest = ChangelogSearchRequest(timestampAfter = syncRecord.fromTime)
        val pageRequest = PaginationRequest(0, taskConfigProperties.creation.fromPool.batchSize)
        val poolChangelogEntries = poolClient.changelogs.getChangelogEntries(changelogSearchRequest, pageRequest)

        val changelogByType = poolChangelogEntries.content.groupBy { it.businessPartnerType }
        val changedBpnAs = changelogByType[BusinessPartnerType.ADDRESS]?.map { it.bpn } ?: emptyList()
        val changedBpnSs = changelogByType[BusinessPartnerType.SITE]?.map { it.bpn } ?: emptyList()
        val changedBpnLs = changelogByType[BusinessPartnerType.LEGAL_ENTITY]?.map { it.bpn } ?: emptyList()

        logger.debug { "Found ${changedBpnLs.size} BPNL, ${changedBpnSs.size} BPNS and ${changedBpnAs.size} BPNA entries." }

        val updatedLegalEntities = updateLegalEntities(changedBpnLs).size
        val updatedSites = updateSites(changedBpnSs).size
        val updatedAddresses = updateAddresses(changedBpnAs).size

        syncRecordService.setSynchronizationSuccess(SyncTypeDb.POOL_TO_GATE_OUTPUT)

        logger.debug { "Updated '$updatedLegalEntities' legal entities, '$updatedSites' sites and '$updatedAddresses' addresses." }

        return UpdateStats(poolChangelogEntries.content.size, updatedLegalEntities, updatedSites, updatedAddresses)
    }

    fun updateAgainstPool(businessPartners: List<BusinessPartnerDb>){
        updateLegalEntitiesByReference(businessPartners).size
        updateSitesByReference(businessPartners.filter { it.bpnS != null }).size
        updateAddressesByReference(businessPartners).size
    }

    private fun updateLegalEntities(changedBpnLs: Collection<String>): List<BusinessPartnerService.UpsertResult> {
        val referencingBusinessPartners = businessPartnerRepository.findByStageAndBpnLIn(StageType.Output, changedBpnLs)
        val businessPartnersToUpdate = referencingBusinessPartners.filter { it.sharingState.sharingStateType == SharingStateType.Success }
        return updateLegalEntitiesByReference(businessPartnersToUpdate)
    }

    private fun updateLegalEntitiesByReference(businessPartnersToUpdate: Collection<BusinessPartnerDb>): List<BusinessPartnerService.UpsertResult> {
        val bpnLsToQuery = businessPartnersToUpdate.mapNotNull { it.bpnL }.toSet()

        val searchRequest = LegalEntitySearchRequest(bpnLs = bpnLsToQuery.toList())
        val legalEntities = if(searchRequest.bpnLs.isNotEmpty())
            poolClient.legalEntities.getLegalEntities(searchRequest, PaginationRequest(size = searchRequest.bpnLs.size)).content.map { it.legalEntity }
        else
            emptyList()

        val legalEntitiesByBpn = legalEntities.associateBy { it.bpnl }

        return businessPartnersToUpdate.mapNotNull { output ->
            val legalEntity = legalEntitiesByBpn[output.bpnL!!] ?: return@mapNotNull null
            val upsertData = legalEntity.toUpsertData(output)
            businessPartnerService.updateBusinessPartnerOutput(output, upsertData)
        }
    }

    private fun updateSites(changedSiteBpns: Collection<String>): List<BusinessPartnerService.UpsertResult> {
        val referencingBusinessPartners = businessPartnerRepository.findByStageAndBpnSIn(StageType.Output, changedSiteBpns)
        val businessPartnersToUpdate = referencingBusinessPartners.filter { it.sharingState.sharingStateType == SharingStateType.Success }
        return updateSitesByReference(businessPartnersToUpdate)
    }

    private fun updateSitesByReference(businessPartnersToUpdate: Collection<BusinessPartnerDb>): List<BusinessPartnerService.UpsertResult> {
        logger.debug { "Found ${businessPartnersToUpdate.size} business partners with matching BPNS to update." }

        val siteBpnsToQuery = businessPartnersToUpdate.mapNotNull { it.bpnS }.toSet()
        val searchRequest = SiteSearchRequest(siteBpns = siteBpnsToQuery.toList())
        val sites = if(searchRequest.siteBpns.isNotEmpty())
            poolClient.sites.getSites(searchRequest, PaginationRequest(size = searchRequest.siteBpns.size)).content.map { it.site }
        else
            emptyList()

        val sitesByBpn = sites.associateBy { it.bpns }


        return businessPartnersToUpdate.mapNotNull { output ->
            val site = sitesByBpn[output.bpnS!!] ?: return@mapNotNull null
            val upsertData = site.toUpsertData(output)
            businessPartnerService.updateBusinessPartnerOutput(output, upsertData)
        }
    }

    private fun updateAddresses(changedAddressBpns: Collection<String>): List<BusinessPartnerService.UpsertResult> {
        val referencingBusinessPartners = businessPartnerRepository.findByStageAndBpnAIn(StageType.Output, changedAddressBpns)
        val businessPartnersToUpdate = referencingBusinessPartners.filter { it.sharingState.sharingStateType == SharingStateType.Success }
        return updateAddressesByReference(businessPartnersToUpdate)
    }

    private fun updateAddressesByReference(businessPartnersToUpdate: Collection<BusinessPartnerDb>): List<BusinessPartnerService.UpsertResult> {
        logger.debug { "Found ${businessPartnersToUpdate.size} business partners with matching BPNA to update." }

        val addressBpnsToQuery = businessPartnersToUpdate.mapNotNull { it.bpnA }.toSet()

        val searchRequest = AddressSearchRequest(addressBpns = addressBpnsToQuery.toList())
        val addresses = if(searchRequest.addressBpns.isNotEmpty())
            poolClient.addresses.getAddresses(searchRequest, PaginationRequest(size = searchRequest.addressBpns.size)).content
        else
            emptyList()

        val addressesByBpn = addresses.associateBy { it.bpna }

        return businessPartnersToUpdate.mapNotNull { output ->
            val address = addressesByBpn[output.bpnA!!] ?: return@mapNotNull null
            val upsertData = address.toUpsertData(output)
            businessPartnerService.updateBusinessPartnerOutput(output, upsertData)
        }
    }

    private fun LegalEntityVerboseDto.toUpsertData(existingOutput: BusinessPartnerDb): OutputUpsertData {
        val copy = BusinessPartnerDb.createEmpty(existingOutput.sharingState, existingOutput.stage)
        copyUtil.copyValues(existingOutput, copy)
        update(copy, this)

        return copy.toUpsertData()
    }

    private fun SiteVerboseDto.toUpsertData(existingOutput: BusinessPartnerDb): OutputUpsertData {
        val copy = BusinessPartnerDb.createEmpty(existingOutput.sharingState, existingOutput.stage)
        copyUtil.copyValues(existingOutput, copy)
        update(copy, this)

        return copy.toUpsertData()
    }

    private fun LogisticAddressVerboseDto.toUpsertData(existingOutput: BusinessPartnerDb): OutputUpsertData {
        val copy = BusinessPartnerDb.createEmpty(existingOutput.sharingState, existingOutput.stage)
        copyUtil.copyValues(existingOutput, copy)
        return update(copy, this)
    }

    private fun BusinessPartnerDb.toUpsertData(): OutputUpsertData {
        return OutputUpsertData(
            nameParts = nameParts,
            identifiers = identifiers.map { it.toUpsertData() },
            states = states.map { it.toUpsertData() },
            roles = roles.toList(),
            isOwnCompanyData = isOwnCompanyData,
            legalEntityBpn = bpnL ?: throw createMappingException(BusinessPartnerDb::bpnL, id),
            siteBpn = bpnS,
            addressBpn = bpnA ?: throw createMappingException(BusinessPartnerDb::bpnA, id),
            legalName = legalName,
            legalForm = legalForm,
            shortName = shortName,
            siteName = siteName,
            addressName = addressName,
            addressType = postalAddress.addressType ?: throw createMappingException(PostalAddressDb::addressType, id),
            physicalPostalAddress = postalAddress.physicalPostalAddress?.toUpsertData() ?: throw createMappingException(
                PostalAddressDb::physicalPostalAddress,
                id
            ),
            alternativePostalAddress = postalAddress.alternativePostalAddress?.toUpsertData(),
            legalEntityConfidence = legalEntityConfidence?.toUpsertData() ?: throw createMappingException(BusinessPartnerDb::legalEntityConfidence, id),
            siteConfidence = siteConfidence?.toUpsertData(),
            addressConfidence = addressConfidence?.toUpsertData() ?: throw createMappingException(BusinessPartnerDb::addressConfidence, id)
        )
    }

    private fun IdentifierDb.toUpsertData(): Identifier {
        return Identifier(type, value, issuingBody, businessPartnerType)
    }

    private fun StateDb.toUpsertData(): State {
        return State(validFrom = validFrom, validTo = validTo, type = type, businessPartnerType = businessPartnerTyp)
    }

    private fun RelationOutputDb.toUpsertData(): Relation {
        return Relation(relationType, sourceBpnL, targetBpnL)
    }

    private fun PhysicalPostalAddressDb.toUpsertData(): PhysicalPostalAddress {
        return PhysicalPostalAddress(
            geographicCoordinates = geographicCoordinates?.toUpsertData(),
            country = country ?: throw createMappingException(PhysicalPostalAddress::country),
            postalCode = postalCode,
            city = city ?: throw createMappingException(PhysicalPostalAddress::city),
            administrativeAreaLevel1 = administrativeAreaLevel1,
            administrativeAreaLevel2 = administrativeAreaLevel2,
            administrativeAreaLevel3 = administrativeAreaLevel3,
            district = district,
            companyPostalCode = companyPostalCode,
            industrialZone = industrialZone,
            building = building,
            floor = floor,
            door = door,
            street = street?.toUpsertData(),
            taxJurisdictionCode = taxJurisdictionCode
        )
    }

    private fun StreetDb.toUpsertData(): Street {
        return Street(
            name = name,
            houseNumber = houseNumber,
            houseNumberSupplement = houseNumberSupplement,
            milestone = milestone,
            direction = direction,
            namePrefix = namePrefix,
            additionalNamePrefix = additionalNamePrefix,
            nameSuffix = nameSuffix,
            additionalNameSuffix = additionalNameSuffix
        )
    }

    private fun AlternativePostalAddressDb.toUpsertData(): AlternativeAddress {
        return AlternativeAddress(
            geographicCoordinates = geographicCoordinates?.toUpsertData(),
            country = country ?: throw createMappingException(AlternativePostalAddressDb::country),
            administrativeAreaLevel1 = administrativeAreaLevel1,
            postalCode = postalCode,
            city = city ?: throw createMappingException(AlternativePostalAddressDb::city),
            deliveryServiceType = deliveryServiceType ?: throw createMappingException(AlternativePostalAddressDb::deliveryServiceType),
            deliveryServiceQualifier = deliveryServiceQualifier,
            deliveryServiceNumber = deliveryServiceNumber ?: throw createMappingException(AlternativePostalAddressDb::deliveryServiceNumber)
        )
    }

    private fun ConfidenceCriteriaDb.toUpsertData(): ConfidenceCriteria {
        return ConfidenceCriteria(
            sharedByOwner = sharedByOwner,
            checkedByExternalDataSource = checkedByExternalDataSource,
            numberOfSharingMembers = numberOfBusinessPartners,
            lastConfidenceCheckAt = lastConfidenceCheckAt,
            nextConfidenceCheckAt = nextConfidenceCheckAt,
            confidenceLevel = confidenceLevel
        )
    }

    private fun GeographicCoordinateDb.toUpsertData(): GeoCoordinate {
        return GeoCoordinate(longitude, latitude, altitude)
    }

    private fun update(businessPartner: BusinessPartnerDb, legalEntity: LegalEntityVerboseDto){
        updateIdentifiers(businessPartner.identifiers, legalEntity.identifiers.map(::toEntity), BusinessPartnerType.LEGAL_ENTITY)
        updateStates(businessPartner.states, legalEntity.states, BusinessPartnerType.LEGAL_ENTITY)
        businessPartner.legalName = legalEntity.legalName
        businessPartner.legalForm = legalEntity.legalForm
        businessPartner.shortName = legalEntity.legalShortName
        businessPartner.legalEntityConfidence?.let { update(it,  legalEntity.confidenceCriteria) }
    }

    private fun update(businessPartner: BusinessPartnerDb, site: SiteVerboseDto){
        updateStates(businessPartner.states, site.states, BusinessPartnerType.SITE)
        businessPartner.siteName = site.name
        businessPartner.siteConfidence?.let { update(it,  site.confidenceCriteria) }
    }

    private fun update(businessPartner: BusinessPartnerDb, address: LogisticAddressVerboseDto) : OutputUpsertData{
        updateIdentifiers(businessPartner.identifiers, address.identifiers.map(::toEntity), BusinessPartnerType.ADDRESS)
        updateStates(businessPartner.states, address.states, BusinessPartnerType.ADDRESS)
        businessPartner.addressName = address.name
        businessPartner.postalAddress.physicalPostalAddress = address.physicalPostalAddress.toEntity()
        businessPartner.postalAddress.alternativePostalAddress = address.alternativePostalAddress?.toEntity()
        businessPartner.addressConfidence?.let { update(it,  address.confidenceCriteria) }

        //Below code will be used when,
        //When addressType has been changed from LegalAddress to LegalAndSiteMainAddress
        //When addressType has been changed from AdditionalAddress to SiteMainAddress
        if (address.bpnSite != null && businessPartner.bpnS == null) {
            businessPartner.bpnS = address.bpnSite
            val searchRequest = SiteSearchRequest(siteBpns = listOf(address.bpnSite!!))
            val sites = if(searchRequest.siteBpns.isNotEmpty())
                poolClient.sites.getSites(searchRequest, PaginationRequest(size = searchRequest.siteBpns.size)).content.map { it.site }
            else
                emptyList()
            //as addressType has been updated, so there is no siteConfidence criteria.
            //fill the fake confidence to prevent the error which will update in further steps.
            businessPartner.siteConfidence = ConfidenceCriteriaDb(false,false,0, LocalDateTime.now(), LocalDateTime.now(),0)
            val sitesByBpn = sites.associateBy { it.bpns }
            val site = sitesByBpn[businessPartner.bpnS!!]
            val upsertData = site!!.toUpsertData(businessPartner)
            return upsertData
        }

        return businessPartner.toUpsertData()
    }

    private fun update(entity: ConfidenceCriteriaDb, poolDto: ConfidenceCriteriaDto){
        entity.sharedByOwner = poolDto.sharedByOwner
        entity.checkedByExternalDataSource = poolDto.checkedByExternalDataSource
        entity.numberOfBusinessPartners = poolDto.numberOfSharingMembers
        entity.lastConfidenceCheckAt = poolDto.lastConfidenceCheckAt
        entity.nextConfidenceCheckAt = poolDto.nextConfidenceCheckAt
        entity.confidenceLevel = poolDto.confidenceLevel
    }

    private fun updateIdentifiers(entities: MutableCollection<IdentifierDb>, updatedIdentifiers: Collection<IdentifierDb>, businessPartnerType: BusinessPartnerType){
        entities.removeIf{ it.businessPartnerType == businessPartnerType }
        entities.addAll(updatedIdentifiers)
    }

    private fun updateStates(entities: MutableCollection<StateDb>, poolDtos: Collection<IBaseStateDto>, businessPartnerType: BusinessPartnerType){
        entities.removeIf{ it.businessPartnerTyp == businessPartnerType }
        entities.addAll(poolDtos.map{ toEntity(it, businessPartnerType) })
    }

    private fun toEntity(poolDto: LegalEntityIdentifierVerboseDto) =
        IdentifierDb(
            poolDto.type,
            poolDto.value,
            poolDto.issuingBody,
            BusinessPartnerType.LEGAL_ENTITY
        )

    private fun toEntity(poolDto: AddressIdentifierVerboseDto) =
        IdentifierDb(
            type = poolDto.type,
            value = poolDto.value,
            issuingBody = null,
            businessPartnerType = BusinessPartnerType.ADDRESS
        )

    private fun toEntity(poolDto: IBaseStateDto, businessPartnerType: BusinessPartnerType) =
        StateDb(
            validFrom = poolDto.validFrom,
            validTo = poolDto.validTo,
            type = poolDto.type!!,
            businessPartnerTyp = businessPartnerType
        )

    private fun PhysicalPostalAddressVerboseDto.toEntity() =
        PhysicalPostalAddressDb(
            geographicCoordinates = geographicCoordinates?.let { GeographicCoordinateDb(it.latitude, it.longitude, it.altitude) },
            country = country,
            administrativeAreaLevel1 = administrativeAreaLevel1,
            administrativeAreaLevel2 = administrativeAreaLevel2,
            administrativeAreaLevel3 = administrativeAreaLevel3,
            postalCode = postalCode,
            city = city,
            district = district,
            street = street?.toEntity(),
            companyPostalCode = companyPostalCode,
            industrialZone = industrialZone,
            building = building,
            floor = floor,
            door = door,
            taxJurisdictionCode = taxJurisdictionCode
        )

    private fun StreetDto.toEntity() =
        StreetDb(
            name,
            houseNumber,
            houseNumberSupplement,
            milestone,
            direction,
            namePrefix,
            additionalNamePrefix,
            nameSuffix,
            additionalNameSuffix
        )

    private fun AlternativePostalAddressVerboseDto.toEntity() =
        AlternativePostalAddressDb(
            geographicCoordinates = geographicCoordinates?.let { GeographicCoordinateDb(it.latitude, it.longitude, it.altitude) },
            country = country,
            administrativeAreaLevel1 = administrativeAreaLevel1,
            postalCode = postalCode,
            city = city,
            deliveryServiceType = deliveryServiceType,
            deliveryServiceQualifier = deliveryServiceQualifier,
            deliveryServiceNumber = deliveryServiceNumber
        )

    private fun toEntity(relation: RelationVerboseDto) =
        RelationOutputDb(
            relationType = relation.type.toGateModel(),
            sourceBpnL = relation.businessPartnerSourceBpnl,
            targetBpnL = relation.businessPartnerTargetBpnl,
            updatedAt = placeholderTime
        )

    private fun RelationType.toGateModel() =
        when(this){
            RelationType.IsAlternativeHeadquarterFor -> SharableRelationType.IsAlternativeHeadquarterFor
        }

    private fun createMappingException(property: KProperty<*>, entityId: Long? = null): BpdmNullMappingException {
        return BpdmNullMappingException(BusinessPartnerDb::class, OutputUpsertData::class, property, entityId.toString())
    }

    data class UpdateStats(
        val foundChangelogEntries: Int,
        val updatedLegalEntities: Int,
        val updatedSites: Int,
        val updatedAddresses: Int
    )
}