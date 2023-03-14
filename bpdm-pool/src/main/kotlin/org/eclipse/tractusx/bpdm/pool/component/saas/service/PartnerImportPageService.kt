/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.component.saas.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.response.AddressPartnerResponse
import org.eclipse.tractusx.bpdm.common.dto.saas.BusinessPartnerSaas
import org.eclipse.tractusx.bpdm.common.dto.saas.ThoroughfareSaas
import org.eclipse.tractusx.bpdm.pool.api.dto.response.AddressPartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.api.dto.response.LegalEntityPartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.api.dto.response.SitePartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.component.saas.config.SaasAdapterConfigProperties
import org.eclipse.tractusx.bpdm.pool.component.saas.dto.*
import org.eclipse.tractusx.bpdm.pool.entity.ImportEntry
import org.eclipse.tractusx.bpdm.pool.repository.AddressPartnerRepository
import org.eclipse.tractusx.bpdm.pool.repository.ImportEntryRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService
import org.eclipse.tractusx.bpdm.pool.service.MetadataService
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class PartnerImportPageService(
    private val adapterProperties: SaasAdapterConfigProperties,
    private val metadataService: MetadataService,
    private val mappingService: SaasToRequestMapper,
    private val businessPartnerBuildService: BusinessPartnerBuildService,
    private val importEntryRepository: ImportEntryRepository,
    private val saasClient: SaasClient,
    private val legalEntityRepository: LegalEntityRepository,
    private val siteRepository: SiteRepository,
    private val addressPartnerRepository: AddressPartnerRepository
) {
    private val logger = KotlinLogging.logger { }

    private val validChildParentRelations = listOf(
        Pair(LsaType.ADDRESS, LsaType.SITE),
        Pair(LsaType.ADDRESS, LsaType.LEGAL_ENTITY),
        Pair(LsaType.SITE, LsaType.LEGAL_ENTITY)
    )

    @Transactional
    fun import(modifiedAfter: Instant, startAfter: String?): ImportResponsePage {
        logger.debug { "Import new business partner starting after ID '$startAfter'" }

        val partnerCollection = saasClient.readBusinessPartners(modifiedAfter, startAfter)

        logger.debug { "Received ${partnerCollection.values.size} to import from SaaS" }

        addNewMetadata(partnerCollection.values)

        val validPartners = partnerCollection.values.filter { isValid(it) }
        val (noBpn, validBpn) = partitionHasNoBpnAndValidBpn(validPartners)

        val (createdLegalEntities, createdSites, createdAddresses) = createPartners(noBpn)
        val (updatedLegalEntities, updatedSites, updatedAddresses) = updatePartners(validBpn)

        return ImportResponsePage(
            partnerCollection.total,
            partnerCollection.nextStartAfter,
            UpsertCollection(createdLegalEntities, updatedLegalEntities),
            UpsertCollection(createdSites, updatedSites),
            UpsertCollection(createdAddresses, updatedAddresses)
        )
    }

    private fun addNewMetadata(partners: Collection<BusinessPartnerSaas>){
        partners
            .flatMap { it.identifiers.mapNotNull { id -> if (id.status?.technicalKey == null) null else id.status } }
            .associateBy { it.technicalKey }
            .minus(metadataService.getIdentifierStati(Pageable.unpaged()).content.map { it.technicalKey }.toSet())
            .values
            .map { mappingService.toRequest(it) }
            .forEach { metadataService.createIdentifierStatus(it) }

        partners
            .flatMap { it.identifiers.mapNotNull { id -> if (id.type?.technicalKey == null) null else id.type } }
            .associateBy { it.technicalKey }
            .minus(metadataService.getIdentifierTypes(Pageable.unpaged()).content.map { it.technicalKey }.toSet())
            .values
            .map { mappingService.toRequest(it) }
            .forEach { metadataService.createIdentifierType(it) }

        partners
            .flatMap { it.identifiers.mapNotNull { id -> if (id.issuingBody?.technicalKey == null) null else id.issuingBody } }
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

    private fun createPartners(partners: Collection<BusinessPartnerSaas>):
            Triple<Collection<LegalEntityPartnerCreateResponse>, Collection<SitePartnerCreateResponse>, Collection<AddressPartnerCreateResponse>> {
        val (legalEntitiesSaas, sitesSaas, addressesSaas) = partitionIntoLSA(partners) { it.extractLsaType() }

        val partnersWithParent = determineParents(sitesSaas + addressesSaas)
        val (_, sitesWithParent, addressesWithParent) = partitionIntoLSA(partnersWithParent) { it.partner.extractLsaType() }

        val legalEntities = legalEntitiesSaas.mapNotNull { mappingService.toLegalEntityCreateRequestOrNull(it) }
        val sites = sitesWithParent.mapNotNull { mappingService.toSiteCreateRequestOrNull(it) }
        val addresses = addressesWithParent.mapNotNull { mappingService.toAddressCreateRequestOrNull(it) }

        val createdLegalEntities = if (legalEntities.isNotEmpty()) businessPartnerBuildService.createLegalEntities(legalEntities) else emptyList()
        val createdSites = if (sites.isNotEmpty()) businessPartnerBuildService.createSites(sites) else emptyList()
        val createdAddresses = if (addresses.isNotEmpty()) businessPartnerBuildService.createAddresses(addresses) else emptyList()

        val legalEntityImportEntries = createdLegalEntities.mapNotNull { if (it.index != null) ImportEntry(it.index!!, it.bpn) else null }
        val siteImportEntries = createdSites.mapNotNull { if (it.index != null) ImportEntry(it.index!!, it.bpn) else null }
        val addressImportEntries = createdAddresses.mapNotNull { if (it.index != null) ImportEntry(it.index!!, it.bpn) else null }

        importEntryRepository.saveAll(legalEntityImportEntries + siteImportEntries + addressImportEntries)

        return Triple(createdLegalEntities, createdSites, createdAddresses)
    }

    private fun updatePartners(partners: Collection<BusinessPartnerWithBpn>):
            Triple<Collection<LegalEntityPartnerCreateResponse>, Collection<SitePartnerCreateResponse>, Collection<AddressPartnerResponse>> {
        val (legalEntitiesSaas, sitesSaas, addressesSaas) = partitionIntoLSA(partners) { it.partner.extractLsaType() }

        val legalEntities = legalEntitiesSaas.mapNotNull { mappingService.toLegalEntityUpdateRequestOrNull(it) }
        val sites = sitesSaas.mapNotNull { mappingService.toSiteUpdateRequestOrNull(it) }
        val addresses = addressesSaas.mapNotNull { mappingService.toAddressUpdateRequestOrNull(it) }

        val createdLegalEntities = if (legalEntities.isNotEmpty()) businessPartnerBuildService.updateLegalEntities(legalEntities) else emptyList()
        val createdSites = if (sites.isNotEmpty()) businessPartnerBuildService.updateSites(sites) else emptyList()
        val createdAddresses = if (addresses.isNotEmpty()) businessPartnerBuildService.updateAddresses(addresses) else emptyList()

        return Triple(createdLegalEntities, createdSites, createdAddresses)
    }

    private fun determineParents(children: Collection<BusinessPartnerSaas>): Collection<BusinessPartnerWithParentBpn> {
        if (children.isEmpty()) return emptyList()

        val childrenWithParentId = determineParentId(children)

        val parents = saasClient.readBusinessPartnersByExternalIds(childrenWithParentId.map { it.parentId }).values
        val validParents = filterValidRelations(parents.filter { isValid(it) }, childrenWithParentId)

        val parentsByImportId = determineParentBPNs(validParents).associateBy { it.partner.externalId!! }

        return childrenWithParentId.mapNotNull { childWithParentId ->
            val parent = parentsByImportId[childWithParentId.parentId]
            if (parent != null) {
                BusinessPartnerWithParentBpn(childWithParentId.partner, parent.bpn)
            } else {
                logger.warn { "Can not resolve parent with Import-ID ${childWithParentId.parentId} for SaaS record with ID ${childWithParentId.partner.id}" }
                null
            }
        }
    }

    private fun determineParentBPNs(parents: Collection<BusinessPartnerSaas>): Collection<BusinessPartnerWithBpn>{
        val parentByImportId = parents.associateBy { it.externalId!! }

        val (parentsWithoutBpn, parentsWithBpn) = partitionHasNoBpnAndValidBpn(parents)

        //create missing parents in the Pool
        val (newLegalEntities, newSites, _) = createPartners(parentsWithoutBpn)

        val createdParents = newLegalEntities.map { Pair(parentByImportId[it.index], it.bpn) }
            .plus(newSites.map { Pair(parentByImportId[it.index], it.bpn) })
            .filter { (parent, _) -> parent != null }
            .map { BusinessPartnerWithBpn(it.first!!, it.second) }

        return parentsWithBpn + createdParents
    }

    private fun determineParentId(children: Collection<BusinessPartnerSaas>): Collection<BusinessPartnerWithParentId> {
        return children.mapNotNull { child ->
            val parentId = child.relations.firstOrNull { id -> id.type?.technicalKey == adapterProperties.parentRelationType }?.startNode
            if (parentId != null) {
                BusinessPartnerWithParentId(child, parentId)
            } else {
                logger.warn { "Can not resolve parent SaaS record for child with ID ${child.id}: Record contains no parent ID" }
                null
            }
        }
    }

    private fun isValid(partner: BusinessPartnerSaas): Boolean {
        if (partner.addresses.any { address -> address.thoroughfares.any { thoroughfare -> thoroughfare.value == null } }) {
            logger.warn { "SaaS Partner with id ${partner.id} is invalid: Contains thoroughfare without ${ThoroughfareSaas::value.name} field specified." }
            return false
        }

        if (partner.externalId == null) {
            logger.warn { "SaaS record with ID ${partner.id} has no external ID that can be used as import ID" }
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

    private fun BusinessPartnerSaas.extractLsaType(): LsaType {
        val validTypes = types.mapNotNull {
            when (it.technicalKey) {
                adapterProperties.legalEntityType -> LsaType.LEGAL_ENTITY
                adapterProperties.siteType -> LsaType.SITE
                adapterProperties.addressType -> LsaType.ADDRESS
                else -> null
            }
        }

        if (validTypes.isEmpty()) {
            logger.warn { "SaaS Business partner with id $id does not contain any LSA type. Assume Legal Entity" }
            return LsaType.LEGAL_ENTITY
        }

        val type = validTypes.first()

        if (validTypes.size > 1) {
            logger.warn { "SaaS Business partner with id $id contains more than one LSA type. Taking first encountered type $type" }
        }

        return type
    }

    /**
     * Partition business partner collection into records for which no BPN can be retrieved and records which have a BPN that also is found in the Pool
     * BPN is determined in two priorities:
     *      1. Try to get BPN from import entry
     *      2. Try to get BPN from record in identifiers and check whether this BPN actually exists in the Pool (valid BPN)
     * If we encounter valid BPNs with no import entry we create an import entry for it (as self correction logic)
     * Optionally, depending on the configuration we either ignore records with invalid BPNs or we treat them as having no BPN
     */
    private fun partitionHasNoBpnAndValidBpn(partners: Collection<BusinessPartnerSaas>): Pair<Collection<BusinessPartnerSaas>, Collection<BusinessPartnerWithBpn>>{
        //search BPN in import entry based on CX-Pool identifier
        val (withEntry, withoutEntry) = partitionHasImportEntry(partners)
        //if no entry has been found look for BPN in identifiers of records
        val (hasBpnIdentifier, hasNoBpnIdentifier) = partitionContainsBpnIdentifier(withoutEntry)
        //if BPN is identifiers but no import record exists, check whether the BPN is known to the BPDM Pool
        val (bpnFound, bpnMissing) = partitionBpnFound(hasBpnIdentifier)

        val consequence = if(adapterProperties.treatInvalidBpnAsNew) "Record will be treated as having no BPN." else "Record will be ignored."
        bpnMissing.forEach {
            logger.warn { "Business partner with Id ${it.partner.externalId} contains BPN ${it.bpn} but such BPN can't be found in the Pool." + consequence }
        }

        val hasValidBpn = withEntry + bpnFound
        val hasNoBpn = if(adapterProperties.treatInvalidBpnAsNew) hasNoBpnIdentifier + bpnMissing.map { it.partner } else hasNoBpnIdentifier

        //Create missing import entries for records which have known BPNs
        importEntryRepository.saveAll(bpnFound.map { ImportEntry(it.partner.externalId!!, it.bpn) })

        return Pair(hasNoBpn, hasValidBpn)
    }

    private fun partitionContainsBpnIdentifier(partners: Collection<BusinessPartnerSaas>): Pair<Collection<BusinessPartnerWithBpn>, Collection<BusinessPartnerSaas>> {
        val (hasBpn, nopBpn) = partners
            .map { Pair(it, it.extractId(adapterProperties.bpnKey)) }
            .partition { (_, bpn) -> bpn != null }
        return Pair(
            hasBpn.map { (partner, id) -> BusinessPartnerWithBpn(partner, id!!) },
            nopBpn.map { (partner, _) -> partner })
    }

    private fun partitionHasImportEntry(partners: Collection<BusinessPartnerSaas>): Pair<Collection<BusinessPartnerWithBpn>, Collection<BusinessPartnerSaas>> {
        val importEntries = importEntryRepository.findByImportIdentifierIn(partners.map { it.externalId!! })
            .associateBy { it.importIdentifier }

        val (hasImportedBpn, noBpn) = partners
            .map { Pair(it, importEntries[it.externalId]?.bpn) }
            .partition { (_, bpn) -> bpn != null }

        return Pair(
            hasImportedBpn.map { (partner, bpn) -> BusinessPartnerWithBpn(partner, bpn!!) },
            noBpn.map { (partner, _) -> partner }
        )
    }

    private fun partitionBpnFound(partners: Collection<BusinessPartnerWithBpn>): Pair<Collection<BusinessPartnerWithBpn>, Collection<BusinessPartnerWithBpn>> {

        val partnersByBpn = partners.associateBy { it.bpn }
        val (bpnLs, bpnSs, bpnAs) = partitionLSA(partnersByBpn.keys)

        val foundBpnLs = legalEntityRepository.findDistinctByBpnIn(bpnLs).map { it.bpn }
        val foundBpnSs = siteRepository.findDistinctByBpnIn(bpnSs).map { it.bpn }
        val foundBpnAs = addressPartnerRepository.findDistinctByBpnIn(bpnAs).map { it.bpn }

        val foundBpns = foundBpnLs + foundBpnSs + foundBpnAs
        val bpnMissing = partnersByBpn - foundBpns.toSet()
        val bpnExists = partnersByBpn - bpnMissing.keys

        return Pair(bpnExists.values, bpnMissing.values)
    }

    private fun partitionLSA(bpns: Collection<String>): Triple<Collection<String>, Collection<String>, Collection<String>>{
        val bpnLs = mutableListOf<String>()
        val bpnSs = mutableListOf<String>()
        val bpnAs = mutableListOf<String>()

        bpns.map { it.uppercase()}.forEach {
            when(it.take(4))
            {
                "BPNL" -> bpnLs.add(it)
                "BPNS" -> bpnSs.add(it)
                "BPNA" -> bpnAs.add(it)
                else -> logger.warn { "Encountered non-valid BPN: $it" }
            }
        }

        return Triple(bpnLs, bpnSs, bpnAs)
    }

    private fun filterValidRelations(
        parents: Collection<BusinessPartnerSaas>,
        children: Collection<BusinessPartnerWithParentId>
    ): Collection<BusinessPartnerSaas> {
        val parentIdToChild = children.associateBy { it.parentId }

        return parents.filter { parent ->
            val parentType = parent.extractLsaType()
            val childType = parentIdToChild[parent.externalId]?.partner?.extractLsaType()

            if (childType != null) {
                val childParentRelation = Pair(childType, parentType)
                validChildParentRelations.contains(childParentRelation)
            } else {
                false
            }
        }
    }

    private fun BusinessPartnerSaas.extractId(idKey: String): String? =
        identifiers.find { it.type?.technicalKey == idKey }?.value

}