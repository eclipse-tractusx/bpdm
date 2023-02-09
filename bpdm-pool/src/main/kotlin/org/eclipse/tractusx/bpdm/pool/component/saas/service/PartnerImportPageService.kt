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
import org.eclipse.tractusx.bpdm.pool.component.saas.config.SaasAdapterConfigProperties
import org.eclipse.tractusx.bpdm.pool.component.saas.dto.*
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
    private val adapterProperties: SaasAdapterConfigProperties,
    private val metadataService: MetadataService,
    private val mappingService: SaasToRequestMapper,
    private val businessPartnerBuildService: BusinessPartnerBuildService,
    private val importEntryRepository: ImportEntryRepository,
    private val saasClient: SaasClient
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

        addMissingImportEntries(validPartners)
        val (partnersWithBpn, partnersWithoutBpn) = partitionHasImportEntry(validPartners)

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

        val legalEntityImportEntries = createdLegalEntities.mapNotNull { if (it.index != null) ImportEntry(it.index, it.bpn) else null }
        val siteImportEntries = createdSites.mapNotNull { if (it.index != null) ImportEntry(it.index, it.bpn) else null }
        val addressImportEntries = createdAddresses.mapNotNull { if (it.index != null) ImportEntry(it.index, it.bpn) else null }

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

        addMissingImportEntries(validParents)
        val (parentsWithBpn, parentsWithoutBpn) = partitionHasImportEntry(validParents)

        val (newLegalEntities, newSites, _) = createPartners(parentsWithoutBpn)

        val parentIdToBpn = newLegalEntities.map { Pair(it.index, it.bpn) }
            .plus(newSites.map { Pair(it.index, it.bpn) })
            .plus(parentsWithBpn.map { Pair(it.partner.externalId!!, it.bpn) })
            .toMap()


        return childrenWithParentId.mapNotNull { childWithParentId ->
            val parentBpn = parentIdToBpn[childWithParentId.parentId]
            if (parentBpn != null) {
                BusinessPartnerWithParentBpn(childWithParentId.partner, parentBpn)
            } else {
                logger.warn { "Can not resolve parent with Import-ID ${childWithParentId.parentId} for SaaS record with ID ${childWithParentId.partner.id}" }
                null
            }
        }
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

    private fun partitionHasBpn(partners: Collection<BusinessPartnerSaas>): Pair<Collection<BusinessPartnerWithBpn>, Collection<BusinessPartnerSaas>> {
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

    private fun addMissingImportEntries(partners: Collection<BusinessPartnerSaas>) {
        val (hasBpn, _) = partitionHasBpn(partners)
        val partnersByImportId = hasBpn.associateBy { it.partner.externalId!! }
        val existingImportIds = importEntryRepository.findByImportIdentifierIn(partnersByImportId.keys).map { it.importIdentifier }.toSet()

        val missingPartners = partnersByImportId.minus(existingImportIds)
        val missingEntries = missingPartners.entries.map { ImportEntry(it.key, it.value.bpn) }

        importEntryRepository.saveAll(missingEntries)
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