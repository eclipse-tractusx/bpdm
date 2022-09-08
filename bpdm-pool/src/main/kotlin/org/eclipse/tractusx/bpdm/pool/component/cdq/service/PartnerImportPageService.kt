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
import org.eclipse.tractusx.bpdm.common.dto.cdq.*
import org.eclipse.tractusx.bpdm.common.dto.response.AddressPartnerResponse
import org.eclipse.tractusx.bpdm.pool.component.cdq.config.CdqAdapterConfigProperties
import org.eclipse.tractusx.bpdm.pool.component.cdq.config.CdqIdentifierConfigProperties
import org.eclipse.tractusx.bpdm.pool.component.cdq.dto.BusinessPartnerWithParent
import org.eclipse.tractusx.bpdm.pool.component.cdq.dto.ImportResponsePage
import org.eclipse.tractusx.bpdm.pool.component.cdq.dto.UpsertCollection
import org.eclipse.tractusx.bpdm.pool.dto.response.AddressPartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.LegalEntityPartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.SitePartnerCreateResponse
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerBuildService
import org.eclipse.tractusx.bpdm.pool.service.MetadataService
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Instant
import java.time.format.DateTimeFormatter

@Service
class PartnerImportPageService(
    private val webClient: WebClient,
    private val adapterProperties: CdqAdapterConfigProperties,
    cdqIdConfigProperties: CdqIdentifierConfigProperties,
    private val metadataService: MetadataService,
    private val mappingService: CdqToRequestMapper,
    private val businessPartnerBuildService: BusinessPartnerBuildService
) {
    private val logger = KotlinLogging.logger { }

    private val readBusinessPartnersEndpoint = "${adapterProperties.exchangeApi}/storages/${adapterProperties.storage}/businesspartners"
    private val fetchBusinessPartnerBatchEndpoint = "${adapterProperties.referenceApi}/businesspartners/fetch-batch"

    private val cdqIdentifierType = TypeKeyNameUrlCdq(cdqIdConfigProperties.typeKey, cdqIdConfigProperties.typeName, "")
    private val cdqIdentifierStatus = TypeKeyNameCdq(cdqIdConfigProperties.statusImportedKey, cdqIdConfigProperties.statusImportedName)
    private val cdqIssuer = TypeKeyNameUrlCdq(cdqIdConfigProperties.issuerKey, cdqIdConfigProperties.issuerName, "")

    private val validTypes = setOf(adapterProperties.legalEntityType, adapterProperties.siteType, adapterProperties.addressType)

    @Transactional
    fun import(modifiedAfter: Instant, startAfter: String?): ImportResponsePage {
        logger.debug { "Import new business partner starting after ID '$startAfter'" }

        val partnerCollection = webClient
            .get()
            .uri { builder ->
                builder
                    .path(readBusinessPartnersEndpoint)
                    .queryParam("modifiedAfter", toModifiedAfterFormat(modifiedAfter))
                    .queryParam("limit", adapterProperties.importLimit)
                    .queryParam("datasource", adapterProperties.datasource)
                    .queryParam("featuresOn", "USE_NEXT_START_AFTER", "FETCH_RELATIONS")
                if (startAfter != null) builder.queryParam("startAfter", startAfter)
                builder.build()
            }
            .retrieve()
            .bodyToMono<PagedResponseCdq<BusinessPartnerCdq>>()
            .block()!!

        logger.debug { "Received ${partnerCollection.values.size} to import from CDQ" }

        addNewMetadata(partnerCollection.values)

        val validPartners = partnerCollection.values.filter { isValid(it) }

        val partnersToUpsert = validPartners.map { addCdqIdentifier(it) }

        val (hasBpn, hasNoBpn) = partnersToUpsert.partition { it.identifiers.any { id -> id.type?.technicalKey == adapterProperties.bpnKey } }

        val (createdLegalEntities, createdSites, createdAddresses) = createPartners(hasNoBpn)
        val (updatedLegalEntities, updatedSites, updatedAddresses) = updatePartners(hasBpn)

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

    private fun createPartners(partners: Collection<BusinessPartnerCdq>):
            Triple<Collection<LegalEntityPartnerCreateResponse>, Collection<SitePartnerCreateResponse>, Collection<AddressPartnerCreateResponse>> {
        val (legalEntitiesCdq, sitesCdq, addressesCdq) = partitionIntoLSA(partners)

        val sitesWithParent = matchParentBpns(sitesCdq)
        val addressesWithParent = matchParentBpns(addressesCdq)

        val legalEntities = legalEntitiesCdq.map { mappingService.toLegalEntityCreateRequest(it) }
        val sites = sitesWithParent.map { mappingService.toSiteCreateRequest(it.partner, it.parentBpn) }
        val addresses = addressesWithParent.map { mappingService.toAddressCreateRequest(it.partner, it.parentBpn) }

        val createdLegalEntities = if (legalEntities.isNotEmpty()) businessPartnerBuildService.createLegalEntities(legalEntities) else emptyList()
        val createdSites = if (sites.isNotEmpty()) businessPartnerBuildService.createSites(sites) else emptyList()
        val createdAddresses = if (addresses.isNotEmpty()) businessPartnerBuildService.createAddresses(addresses) else emptyList()

        return Triple(createdLegalEntities, createdSites, createdAddresses)
    }

    private fun updatePartners(partners: Collection<BusinessPartnerCdq>):
            Triple<Collection<LegalEntityPartnerCreateResponse>, Collection<SitePartnerCreateResponse>, Collection<AddressPartnerResponse>> {
        val (legalEntitiesCdq, sitesCdq, addressesCdq) = partitionIntoLSA(partners)

        val legalEntities = legalEntitiesCdq.map { mappingService.toLegalEntityUpdateRequest(it) }
        val sites = sitesCdq.map { mappingService.toSiteUpdateRequest(it) }
        val addresses = addressesCdq.map { mappingService.toAddressUpdateRequest(it) }

        val createdLegalEntities = if (legalEntities.isNotEmpty()) businessPartnerBuildService.updateLegalEntities(legalEntities) else emptyList()
        val createdSites = if (sites.isNotEmpty()) businessPartnerBuildService.updateSites(sites) else emptyList()
        val createdAddresses = if (addresses.isNotEmpty()) businessPartnerBuildService.updateAddresses(addresses) else emptyList()

        return Triple(createdLegalEntities, createdSites, createdAddresses)
    }

    private fun matchParentBpns(partners: Collection<BusinessPartnerCdq>): Collection<BusinessPartnerWithParent> {
        val parentIdGroups = partners
            .groupBy { it.relations.firstOrNull { id -> id.type.technicalKey == adapterProperties.parentRelationType }?.startNode }
            .filter { it.key != null }


        if (parentIdGroups.isEmpty())
            return emptyList()

        val requestBody = FetchBatchRequest(parentIdGroups.keys.mapNotNull { it })

        val batchRecords = webClient
            .post()
            .uri(fetchBusinessPartnerBatchEndpoint)
            .body(BodyInserters.fromValue(requestBody))
            .retrieve()
            .bodyToMono<Collection<FetchBatchRecord>>()
            .block()!!

        return batchRecords
            .flatMap { record -> parentIdGroups[record.cdqId]?.map { child -> Pair(record.businessPartner, child) } ?: emptyList() }
            .map { (parent, child) -> Pair(extractId(parent.identifiers, adapterProperties.bpnKey), child) }
            .filter { (parentBpn, _) -> parentBpn != null }
            .map { (parentBpn, child) -> BusinessPartnerWithParent(child, parentBpn!!) }
    }

    private fun toModifiedAfterFormat(dateTime: Instant): String {
        return DateTimeFormatter.ISO_INSTANT.format(dateTime)
    }

    private fun isValid(partner: BusinessPartnerCdq): Boolean {
        if (partner.addresses.any { address -> address.thoroughfares.any { thoroughfare -> thoroughfare.value == null } }) {
            logger.warn { "CDQ Partner with id ${partner.id} is invalid: Contains thoroughfare without ${ThoroughfareCdq::value.name} field specified." }
            return false
        }

        return true
    }

    private fun addCdqIdentifier(partner: BusinessPartnerCdq) =
        partner.copy(
            identifiers = partner.identifiers.plus(
                IdentifierCdq(
                    cdqIdentifierType,
                    partner.id!!,
                    cdqIssuer,
                    cdqIdentifierStatus
                )
            )
        )

    private fun extractId(identifiers: Collection<IdentifierCdq>, idType: String): String? {
        return identifiers.find { it.type?.technicalKey == idType }?.value
    }

    private fun partitionIntoLSA(partners: Collection<BusinessPartnerCdq>):
            Triple<Collection<BusinessPartnerCdq>, Collection<BusinessPartnerCdq>, Collection<BusinessPartnerCdq>> {
        val lsaGroups = partners.groupBy { extractLsaTypes(it) }.filter { it.key != null }

        val legalEntities = lsaGroups[adapterProperties.legalEntityType] ?: emptyList()
        val sites = lsaGroups[adapterProperties.siteType] ?: emptyList()
        val addresses = lsaGroups[adapterProperties.addressType] ?: emptyList()

        return Triple(legalEntities, sites, addresses)
    }

    private fun extractLsaTypes(partner: BusinessPartnerCdq): String? {
        val validTypes = partner.types.map { it.technicalKey }.intersect(validTypes)

        if (validTypes.isEmpty()) {
            logger.warn { "CDQ Business partner with id ${partner.id} does not contain any LSA type. Partner will be ignored" }
            return null
        }

        val type = validTypes.first()

        if (validTypes.size > 1) {
            logger.warn { "CDQ Business partner with id ${partner.id} contains more than one LSA type. Taking first encountered type $type" }
        }

        return type
    }


}