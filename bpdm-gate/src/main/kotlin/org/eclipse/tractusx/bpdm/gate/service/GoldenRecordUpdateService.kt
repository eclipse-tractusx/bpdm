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

import com.nimbusds.oauth2.sdk.id.Identifier
import jakarta.persistence.Column
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.dto.IBaseStateDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.gate.config.GoldenRecordTaskConfigProperties
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.entity.generic.*
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.bpdm.gate.util.BusinessPartnerComparisonUtil
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

@Service
class GoldenRecordUpdateService(
    private val poolClient: PoolApiClient,
    private val syncRecordService: SyncRecordService,
    private val taskConfigProperties: GoldenRecordTaskConfigProperties,
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val changelogRepository: ChangelogRepository,
    private val copyUtil: BusinessPartnerCopyUtil,
    private val compareUtil: BusinessPartnerComparisonUtil
) {

    private val logger = KotlinLogging.logger { }

    @Transactional
    fun updateOutputOnGoldenRecordChange(){
        logger.info { "Update Business Partner Output based on Golden Record Updates from the Pool..." }

        val syncRecord = syncRecordService.setSynchronizationStart(SyncTypeDb.POOL_TO_GATE_OUTPUT)

        val changelogSearchRequest = ChangelogSearchRequest(timestampAfter = syncRecord.fromTime)
        val pageRequest = PaginationRequest(0, taskConfigProperties.creation.fromPool.batchSize)
        val poolChangelogEntries = poolClient.changelogs.getChangelogEntries(changelogSearchRequest, pageRequest)

        val changelogByType = poolChangelogEntries.content.groupBy { it.businessPartnerType }
        val changedBpnAs = changelogByType[BusinessPartnerType.ADDRESS]?.map { it.bpn } ?: emptyList()
        val changedBpnSs = changelogByType[BusinessPartnerType.SITE]?.map { it.bpn } ?: emptyList()
        val changedBpnLs = changelogByType[BusinessPartnerType.LEGAL_ENTITY]?.map { it.bpn } ?: emptyList()

        logger.debug { "Found ${changedBpnLs.size} BPNL, ${changedBpnSs.size} BPNS and ${changedBpnAs.size} BPNA entries." }

        updateLegalEntities(changedBpnLs)
        updateSites(changedBpnSs)
        updateAddresses(changedBpnAs)

        syncRecordService.setSynchronizationSuccess(SyncTypeDb.POOL_TO_GATE_OUTPUT)
    }

    private fun updateLegalEntities(changedBpnLs: Collection<String>){
        val businessPartnersToUpdate = businessPartnerRepository.findByStageAndBpnLIn(StageType.Output, changedBpnLs)

        logger.debug { "Found ${businessPartnersToUpdate.size} business partners with matching BPNL to update." }

        val bpnLsToQuery = businessPartnersToUpdate.mapNotNull { it.bpnL }

        val searchRequest = LegalEntitySearchRequest(bpnLs = bpnLsToQuery)
        val legalEntities = if(searchRequest.bpnLs.isNotEmpty())
                poolClient.legalEntities.getLegalEntities(searchRequest, PaginationRequest(size = searchRequest.bpnLs.size)).content
            else
                emptyList()


        val legalEntitiesByBpn = legalEntities.associateBy { it.legalEntity.bpnl }

        val updatedPartners = businessPartnersToUpdate.mapNotNull { partner ->
            legalEntitiesByBpn[partner.bpnL]?.legalEntity?.let { legalEntity -> partner.apply { update(partner, legalEntity) } }
        }

        logger.debug { "Updating ${updatedPartners.size} business partners from legal entities" }

        val changedPartners = filterChanged(updatedPartners)

        businessPartnerRepository.saveAll(changedPartners)

        val changelogs = changedPartners.map { ChangelogEntryDb(it.externalId, it.associatedOwnerBpnl, ChangelogType.UPDATE, StageType.Output) }
        changelogRepository.saveAll(changelogs)

        logger.debug { "Actual values changed of ${changedPartners.size} business partners from legal entities" }
    }

    private fun updateSites(changedSiteBpns: Collection<String>){
        val businessPartnersToUpdate = businessPartnerRepository.findByStageAndBpnSIn(StageType.Output, changedSiteBpns)

        logger.debug { "Found ${businessPartnersToUpdate.size} business partners with matching BPNS to update." }

        val siteBpnsToQuery = businessPartnersToUpdate.mapNotNull { it.bpnS }

        val searchRequest = SiteSearchRequest(siteBpns = siteBpnsToQuery)
        val sites = if(searchRequest.siteBpns.isNotEmpty())
            poolClient.sites.getSites(searchRequest, PaginationRequest(size = searchRequest.siteBpns.size)).content
        else
            emptyList()

        val sitesByBpn = sites.associateBy { it.site.bpns }


        val updatedPartners = businessPartnersToUpdate.mapNotNull { partner ->
            sitesByBpn[partner.bpnS]?.site?.let { site -> partner.apply { update(partner, site) } }
        }

        logger.debug { "Updating ${updatedPartners.size} business partners from sites" }


        val changedPartners = filterChanged(updatedPartners)

        businessPartnerRepository.saveAll(changedPartners)

        val changelogs = changedPartners.map { ChangelogEntryDb(it.externalId, it.associatedOwnerBpnl, ChangelogType.UPDATE, StageType.Output) }
        changelogRepository.saveAll(changelogs)

        logger.debug { "Actual values changed of ${changedPartners.size} business partners from sites" }
    }

    private fun updateAddresses(changedAddressBpns: Collection<String>){
        val businessPartnersToUpdate = businessPartnerRepository.findByStageAndBpnAIn(StageType.Output, changedAddressBpns)

        logger.debug { "Found ${businessPartnersToUpdate.size} business partners with matching BPNA to update." }

        val addressBpnsToQuery = businessPartnersToUpdate.mapNotNull { it.bpnA }

        val searchRequest = AddressSearchRequest(addressBpns = addressBpnsToQuery)
        val addresses = if(searchRequest.addressBpns.isNotEmpty())
            poolClient.addresses.getAddresses(searchRequest, PaginationRequest(size = searchRequest.addressBpns.size)).content
        else
            emptyList()

        val addressesByBpn = addresses.associateBy { it.bpna }

        val updatedPartners = businessPartnersToUpdate.mapNotNull { partner ->
            addressesByBpn[partner.bpnA]?.let { address -> partner.apply { update(partner, address) } }
        }

        logger.debug { "Updating ${updatedPartners.size} business partners from logistic addresses" }

        val changedPartners = filterChanged(updatedPartners)

        businessPartnerRepository.saveAll(changedPartners)

        val changelogs = changedPartners.map { ChangelogEntryDb(it.externalId, it.associatedOwnerBpnl, ChangelogType.UPDATE, StageType.Output) }
        changelogRepository.saveAll(changelogs)

        logger.debug { "Actual values changed of ${changedPartners.size} business partners from logistic addresses" }
    }

    private fun update(businessPartner: BusinessPartnerDb, legalEntity: LegalEntityVerboseDto){
        updateIdentifiers(businessPartner.identifiers, legalEntity.identifiers.map(::toEntity), BusinessPartnerType.LEGAL_ENTITY)
        updateStates(businessPartner.states, legalEntity.states, BusinessPartnerType.LEGAL_ENTITY)
        businessPartner.legalName = legalEntity.legalName
        businessPartner.legalForm = legalEntity.legalForm
        businessPartner.legalEntityConfidence?.let { update(it,  legalEntity.confidenceCriteria) }
    }

    private fun update(businessPartner: BusinessPartnerDb, site: SiteVerboseDto){
        updateStates(businessPartner.states, site.states, BusinessPartnerType.SITE)
        businessPartner.siteName = site.name
        businessPartner.siteConfidence?.let { update(it,  site.confidenceCriteria) }
    }

    private fun update(businessPartner: BusinessPartnerDb, address: LogisticAddressVerboseDto){
        updateIdentifiers(businessPartner.identifiers, address.identifiers.map(::toEntity), BusinessPartnerType.ADDRESS)
        updateStates(businessPartner.states, address.states, BusinessPartnerType.ADDRESS)
        businessPartner.addressName = address.name
        businessPartner.postalAddress.physicalPostalAddress = address.physicalPostalAddress.toEntity()
        businessPartner.postalAddress.alternativePostalAddress = address.alternativePostalAddress?.toEntity()
        businessPartner.addressConfidence?.let { update(it,  address.confidenceCriteria) }
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
            door = door
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

    private fun emptyBusinessPartner(externalId: String) =
        BusinessPartnerDb(
            externalId = externalId,
            stage = StageType.Output,
            legalEntityConfidence = null,
            siteConfidence = null,
            addressConfidence = null,
            postalAddress = PostalAddressDb()
        )

    private fun filterChanged(partners: Collection<BusinessPartnerDb>): Collection<BusinessPartnerDb>{
        return partners.associateWith { partner -> emptyBusinessPartner(partner.externalId).apply { copyUtil.copyValues( partner, this) }  }
            .filter { (partner, copy) -> compareUtil.hasChanges(partner, copy) }
            .keys
    }

}