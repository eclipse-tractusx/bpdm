/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.component.cdq.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.cdq.BusinessPartnerCdq
import org.eclipse.tractusx.bpdm.common.dto.cdq.ThoroughfareCdq
import org.eclipse.tractusx.bpdm.common.dto.cdq.TypeKeyNameCdq
import org.eclipse.tractusx.bpdm.common.dto.cdq.TypeKeyNameUrlCdq
import org.eclipse.tractusx.bpdm.common.dto.response.AddressPartnerResponse
import org.eclipse.tractusx.bpdm.pool.component.cdq.config.CdqAdapterConfigProperties
import org.eclipse.tractusx.bpdm.pool.component.cdq.config.CdqIdentifierConfigProperties
import org.eclipse.tractusx.bpdm.pool.component.cdq.dto.*
import org.eclipse.tractusx.bpdm.pool.dto.response.AddressPartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.LegalEntityPartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.SitePartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.entity.ImportEntry
import org.eclipse.tractusx.bpdm.pool.repository.ImportEntryRepository
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService
import org.eclipse.tractusx.bpdm.pool.service.MetadataService
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class PartnerImportPageService(
    private val adapterProperties: CdqAdapterConfigProperties,
    cdqIdConfigProperties: CdqIdentifierConfigProperties,
    private val metadataService: MetadataService,
    private val mappingService: CdqToRequestMapper,
    private val businessPartnerBuildService: BusinessPartnerBuildService,
    private val importEntryRepository: ImportEntryRepository,
    private val cdqClient: CdqClient
) {
    private val logger = KotlinLogging.logger { }

    private val cdqIdentifierType = TypeKeyNameUrlCdq(cdqIdConfigProperties.typeKey, cdqIdConfigProperties.typeName, "")
    private val cdqIdentifierStatus = TypeKeyNameCdq(cdqIdConfigProperties.statusImportedKey, cdqIdConfigProperties.statusImportedName)
    private val cdqIssuer = TypeKeyNameUrlCdq(cdqIdConfigProperties.issuerKey, cdqIdConfigProperties.issuerName, "")

    private val validChildParentRelations = listOf(
        Pair(LsaType.ADDRESS, LsaType.SITE),
        Pair(LsaType.ADDRESS, LsaType.LEGAL_ENTITY),
        Pair(LsaType.SITE, LsaType.LEGAL_ENTITY)
    )

    @Transactional
    fun import(modifiedAfter: Instant, startAfter: String?): ImportResponsePage {
        logger.debug { "Import new business partner starting after ID '$startAfter'" }

        val partnerCollection = cdqClient.readBusinessPartners(modifiedAfter, startAfter)

        logger.debug { "Received ${partnerCollection.values.size} to import from CDQ" }

        val asFetch = cdqClient.fetchBusinessPartnersInBatch(partnerCollection.values.mapNotNull { it.extractId(adapterProperties.importIdKey) })

        addNewMetadata(partnerCollection.values)

        val validPartners = partnerCollection.values.filter { isValid(it) }
        val partnersWithId = determineImportId(validPartners)

        val (partnersWithBpn, partnersWithoutBpn) = determineBpn(partnersWithId)

        val (createdLegalEntities, createdSites, createdAddresses) = createPartners(partnersWithoutBpn)
        val (updatedLegalEntities, updatedSites, updatedAddresses) = updatePartners(partnersWithBpn)

        return ImportResponsePage(
            partnerCollection.total,
            partnerCollection.nextStartAfter,
            UpsertCollection(createdLegalEntities, updatedLegalEntities),
            UpsertCollection(createdSites, updatedSites),
            UpsertCollection(createdAddresses, updatedAddresses)
        )
    }

    private fun addNewMetadata(partners: Collection<BusinessPartnerCdq>){
        partners
            .flatMap { it.identifiers.mapNotNull { id -> if (id.status?.technicalKey == null) null else id.status } }
            .plus(cdqIdentifierStatus)
            .associateBy { it.technicalKey }
            .minus(metadataService.getIdentifierStati(Pageable.unpaged()).content.map { it.technicalKey }.toSet())
            .values
            .map { mappingService.toRequest(it) }
            .forEach { metadataService.createIdentifierStatus(it) }

        partners
            .flatMap { it.identifiers.mapNotNull { id -> if (id.type?.technicalKey == null) null else id.type } }
            .plus(cdqIdentifierType)
            .associateBy { it.technicalKey }
            .minus(metadataService.getIdentifierTypes(Pageable.unpaged()).content.map { it.technicalKey }.toSet())
            .values
            .map { mappingService.toRequest(it) }
            .forEach { metadataService.createIdentifierType(it) }

        partners
            .flatMap { it.identifiers.mapNotNull { id -> if (id.issuingBody?.technicalKey == null) null else id.issuingBody } }
            .plus(cdqIssuer)
            .associateBy { it.technicalKey }
            .minus(metadataService.getIssuingBodies(Pageable.unpaged()).content.map { it.technicalKey }.toSet())
            .values
            .map { mappingService.toRequest(it) }
            .forEach { metadataService.createIssuingBody(it) }

        partners
            .filter { it.legalForm?.technicalKey != null }
            .map { it.legalForm!! to it }
            .associateBy { it.first.technicalKey }
            .minus(metadataService.getLegalForms(Pageable.unpaged()).content.map { it.technicalKey }.toSet())
            .values
            .map { mappingService.toRequest(it.first, it.second) }
            .forEach { metadataService.createLegalForm(it) }
    }

    private fun createPartners(partners: Collection<BusinessPartnerWithImportId>):
            Triple<Collection<LegalEntityPartnerCreateResponse>, Collection<SitePartnerCreateResponse>, Collection<AddressPartnerCreateResponse>> {
        val (legalEntitiesCdq, sitesCdq, addressesCdq) = partitionIntoLSA(partners) { it.partner.extractLsaType() }

        val partnersWithParent = determineParent(sitesCdq + addressesCdq)
        val (_, sitesWithParent, addressesWithParent) = partitionIntoLSA(partnersWithParent) { it.partner.extractLsaType() }

        val legalEntities = legalEntitiesCdq.map { mappingService.toLegalEntityCreateRequest(it) }
        val sites = sitesWithParent.map { mappingService.toSiteCreateRequest(it) }
        val addresses = addressesWithParent.map { mappingService.toAddressCreateRequest(it) }

        val createdLegalEntities = if (legalEntities.isNotEmpty()) businessPartnerBuildService.createLegalEntities(legalEntities) else emptyList()
        val createdSites = if (sites.isNotEmpty()) businessPartnerBuildService.createSites(sites) else emptyList()
        val createdAddresses = if (addresses.isNotEmpty()) businessPartnerBuildService.createAddresses(addresses) else emptyList()

        val legalEntityImportEntries = createdLegalEntities.mapNotNull { if (it.index != null) ImportEntry(it.index, it.bpn) else null }
        val siteImportEntries = createdSites.mapNotNull { if (it.index != null) ImportEntry(it.index, it.bpn) else null }
        val addressImportEntries = createdAddresses.mapNotNull { if (it.index != null) ImportEntry(it.index, it.bpn) else null }

        importEntryRepository.saveAll(legalEntityImportEntries + siteImportEntries + addressImportEntries)

        return Triple(createdLegalEntities, createdSites, createdAddresses)
    }

    private fun updatePartners(partners: Collection<BusinessPartnerWithBpn>):
            Triple<Collection<LegalEntityPartnerCreateResponse>, Collection<SitePartnerCreateResponse>, Collection<AddressPartnerResponse>> {
        val (legalEntitiesCdq, sitesCdq, addressesCdq) = partitionIntoLSA(partners) { it.partner.extractLsaType() }

        val legalEntities = legalEntitiesCdq.map { mappingService.toLegalEntityUpdateRequest(it) }
        val sites = sitesCdq.map { mappingService.toSiteUpdateRequest(it) }
        val addresses = addressesCdq.map { mappingService.toAddressUpdateRequest(it) }

        val createdLegalEntities = if (legalEntities.isNotEmpty()) businessPartnerBuildService.updateLegalEntities(legalEntities) else emptyList()
        val createdSites = if (sites.isNotEmpty()) businessPartnerBuildService.updateSites(sites) else emptyList()
        val createdAddresses = if (addresses.isNotEmpty()) businessPartnerBuildService.updateAddresses(addresses) else emptyList()

        return Triple(createdLegalEntities, createdSites, createdAddresses)
    }

    private fun determineParent(children: Collection<BusinessPartnerWithImportId>): Collection<BusinessPartnerWithParent> {
        if (children.isEmpty()) return emptyList()

        val childrenWithParentId = determineParentId(children)

        val parents = cdqClient.fetchBusinessPartnersInBatch(childrenWithParentId.map { it.parentId }).map { it.businessPartner }
        val parentsWithId = determineImportId(parents)
        val validParentsWithId = filterValidParents(parentsWithId, childrenWithParentId)

        val (parentsWithBpn, parentsWithoutBpn) = determineBpn(validParentsWithId)

        val (newLegalEntities, newSites, _) = createPartners(parentsWithoutBpn)

        val parentIdToBpn = newLegalEntities.map { Pair(it.index, it.bpn) }
            .plus(newSites.map { Pair(it.index, it.bpn) })
            .plus(parentsWithBpn.map { Pair(it.importId, it.bpn) })
            .toMap()


        return childrenWithParentId.mapNotNull { childWithParentId ->
            val parentBpn = parentIdToBpn[childWithParentId.parentId]
            if (parentBpn != null) {
                BusinessPartnerWithParent(childWithParentId.partner, childWithParentId.importId, parentBpn)
            } else {
                logger.warn { "Can not resolve parent with Import-ID ${childWithParentId.parentId} for CDQ record with ID ${childWithParentId.partner.id}" }
                null
            }
        }
    }

    private fun determineParentId(children: Collection<BusinessPartnerWithImportId>): Collection<BusinessPartnerWithParentId> {
        return children.mapNotNull { childWithId ->
            val parentId = childWithId.partner.relations.firstOrNull { id -> id.type?.technicalKey == adapterProperties.parentRelationType }?.startNode
            if (parentId != null) {
                BusinessPartnerWithParentId(childWithId.partner, childWithId.importId, parentId)
            } else {
                logger.warn { "Can not resolve parent CDQ record for child with ID ${childWithId.partner.id}: Record has no identifier of type ${adapterProperties.importIdKey}" }
                null
            }
        }
    }

    private fun isValid(partner: BusinessPartnerCdq): Boolean {
        if (partner.addresses.any { address -> address.thoroughfares.any { thoroughfare -> thoroughfare.value == null } }) {
            logger.warn { "CDQ Partner with id ${partner.id} is invalid: Contains thoroughfare without ${ThoroughfareCdq::value.name} field specified." }
            return false
        }

        return true
    }

    private fun <T> partitionIntoLSA(partners: Collection<T>, determineTypeFunction: (T) -> LsaType?):
            Triple<Collection<T>, Collection<T>, Collection<T>> {

        val lsaGroups = partners.groupBy { determineTypeFunction(it) }.filter { it.key != null }

        val legalEntities = lsaGroups[LsaType.LEGAL_ENTITY] ?: emptyList()
        val sites = lsaGroups[LsaType.SITE] ?: emptyList()
        val addresses = lsaGroups[LsaType.ADDRESS] ?: emptyList()

        return Triple(legalEntities, sites, addresses)
    }

    private fun BusinessPartnerCdq.extractLsaType(): LsaType? {
        val validTypes = types.mapNotNull {
            when (it.technicalKey) {
                adapterProperties.legalEntityType -> LsaType.LEGAL_ENTITY
                adapterProperties.siteType -> LsaType.SITE
                adapterProperties.addressType -> LsaType.ADDRESS
                else -> null
            }
        }

        if (validTypes.isEmpty()) {
            logger.warn { "CDQ Business partner with id $id does not contain any LSA type. Partner will be ignored" }
            return null
        }

        val type = validTypes.first()

        if (validTypes.size > 1) {
            logger.warn { "CDQ Business partner with id $id contains more than one LSA type. Taking first encountered type $type" }
        }

        return type
    }

    private fun determineBpn(partners: Collection<BusinessPartnerWithImportId>): Pair<Collection<BusinessPartnerWithBpn>, Collection<BusinessPartnerWithImportId>> {
        val (hasStorageBpn, noStorageBpn) = partners
            .map { Pair(it, it.partner.extractId(adapterProperties.bpnKey)) }
            .partition { (_, bpn) -> bpn != null }

        val (hasImportedBpn, noBpn) = determineImportedBpn(noStorageBpn.map { (partnerWithId, _) -> partnerWithId })

        return Pair(
            hasStorageBpn.map { (partnerWithId, id) -> BusinessPartnerWithBpn(partnerWithId.partner, partnerWithId.importId, id!!) } + hasImportedBpn,
            noBpn)
    }

    private fun determineImportedBpn(partners: Collection<BusinessPartnerWithImportId>): Pair<Collection<BusinessPartnerWithBpn>, Collection<BusinessPartnerWithImportId>> {
        val importEntries = importEntryRepository.findByImportIdentifierIn(partners.map { it.importId })
            .associateBy { it.importIdentifier }

        val (hasImportedBpn, noBpn) = partners
            .map { Pair(it, importEntries[it.importId]?.bpn) }
            .partition { (_, bpn) -> bpn != null }

        return Pair(
            hasImportedBpn.map { (partnerWithId, bpn) -> BusinessPartnerWithBpn(partnerWithId.partner, partnerWithId.importId, bpn!!) },
            noBpn.map { (partnerWithId, _) -> partnerWithId }
        )
    }

    private fun determineImportId(partners: Collection<BusinessPartnerCdq>): Collection<BusinessPartnerWithImportId> {
        return partners.mapNotNull { partner ->
            val importId = partner.extractId(adapterProperties.importIdKey)
            if (importId != null) {
                BusinessPartnerWithImportId(partner, importId)
            } else {
                logger.warn { "CDQ record with ID ${partner.id} has no import ID" }
                null
            }
        }
    }

    private fun filterValidParents(
        parents: Collection<BusinessPartnerWithImportId>,
        children: Collection<BusinessPartnerWithParentId>
    ): Collection<BusinessPartnerWithImportId> {
        val parentIdToChild = children.associateBy { it.parentId }

        return parents.filter { parent ->
            val parentType = parent.partner.extractLsaType()
            val childType = parentIdToChild[parent.importId]?.partner?.extractLsaType()

            val childParentRelation = Pair(childType, parentType)

            validChildParentRelations.contains(childParentRelation)
        }
    }

    private fun BusinessPartnerCdq.extractId(idKey: String): String? =
        identifiers.find { it.type?.technicalKey == idKey }?.value


}