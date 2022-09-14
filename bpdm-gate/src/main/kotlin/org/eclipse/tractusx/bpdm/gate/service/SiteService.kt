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

package org.eclipse.tractusx.bpdm.gate.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.cdq.BusinessPartnerCdq
import org.eclipse.tractusx.bpdm.common.dto.cdq.FetchResponse
import org.eclipse.tractusx.bpdm.common.dto.response.MainAddressSearchResponse
import org.eclipse.tractusx.bpdm.common.dto.response.SitePartnerSearchResponse
import org.eclipse.tractusx.bpdm.common.dto.response.SiteResponse
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.service.CdqMappings
import org.eclipse.tractusx.bpdm.gate.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.gate.dto.SiteGateInput
import org.eclipse.tractusx.bpdm.gate.dto.SiteGateOutput
import org.eclipse.tractusx.bpdm.gate.dto.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.exception.CdqInvalidRecordException
import org.eclipse.tractusx.bpdm.gate.exception.CdqNonexistentParentException
import org.eclipse.tractusx.bpdm.gate.filterNotNullKeys
import org.eclipse.tractusx.bpdm.gate.filterNotNullValues
import org.springframework.stereotype.Service

@Service
class SiteService(
    private val cdqRequestMappingService: CdqRequestMappingService,
    private val inputCdqMappingService: InputCdqMappingService,
    private val cdqClient: CdqClient,
    private val poolClient: PoolClient,
    private val bpnConfigProperties: BpnConfigProperties
) {
    private val logger = KotlinLogging.logger { }

    fun getSites(limit: Int, startAfter: String?): PageStartAfterResponse<SiteGateInput> {
        val sitesPage = cdqClient.getSites(limit, startAfter)

        val validEntries = sitesPage.values.filter { validateSiteBusinessPartner(it) }

        return PageStartAfterResponse(
            total = sitesPage.total,
            nextStartAfter = sitesPage.nextStartAfter,
            content = validEntries.map { inputCdqMappingService.toInputSite(it) },
            invalidEntries = sitesPage.values.size - validEntries.size
        )
    }

    fun getSiteByExternalId(externalId: String): SiteGateInput {
        val fetchResponse = cdqClient.getBusinessPartner(externalId)

        when (fetchResponse.status) {
            FetchResponse.Status.OK -> return toValidSiteInput(fetchResponse.businessPartner!!)
            FetchResponse.Status.NOT_FOUND -> throw BpdmNotFoundException("Site", externalId)
        }
    }

    /**
     * Get sites by first fetching sites from "augmented business partners" in CDQ. Augmented business partners from CDQ should contain a BPN,
     * which is then used to fetch the data for the sites from the bpdm pool.
     */
    fun getSitesOutput(externalIds: Collection<String>?, limit: Int, startAfter: String?): PageStartAfterResponse<SiteGateOutput> {
        val partnerCollection = cdqClient.getAugmentedSites(limit = limit, startAfter = startAfter, externalIds = externalIds)

        val bpnToExternalIdMapNullable =
            partnerCollection.values.mapNotNull { it.augmentedBusinessPartner }.associateBy({ CdqMappings.findBpn(it.identifiers) }, { it.externalId })
        val numSitesWithoutBpn = bpnToExternalIdMapNullable.filter { it.key == null }.size
        val numSitesWithoutExternalId = bpnToExternalIdMapNullable.filter { it.value == null }.size

        if (numSitesWithoutBpn > 0) {
            logger.warn { "Encountered $numSitesWithoutBpn sites without BPN in CDQ. Can't retrieve data from pool for these." }
        }
        if (numSitesWithoutExternalId > 0) {
            logger.warn { "Encountered $numSitesWithoutExternalId sites without external id in CDQ." }
        }

        val bpnToExternalIdMap = bpnToExternalIdMapNullable.filterNotNullKeys().filterNotNullValues()

        val bpnSs = bpnToExternalIdMap.keys
        val sites = poolClient.searchSites(bpnSs)
        val mainAddresses = poolClient.searchMainAddresses(bpnSs)

        if (bpnSs.size > sites.size) {
            logger.warn { "Requested ${bpnSs.size} sites from pool, but only ${sites.size} were found." }
        }
        if (bpnSs.size > mainAddresses.size) {
            logger.warn { "Requested ${bpnSs.size} main addresses of sites from pool, but only ${mainAddresses.size} were found." }
        }

        val sitesOutput = toSitesOutput(sites, mainAddresses, bpnToExternalIdMap)

        return PageStartAfterResponse(
            total = partnerCollection.total,
            nextStartAfter = partnerCollection.nextStartAfter,
            content = sitesOutput,
            invalidEntries = partnerCollection.values.size - sitesOutput.size // difference of what gate can return to values in cdq
        )
    }

    private fun toSitesOutput(
        sites: Collection<SitePartnerSearchResponse>,
        mainAddresses: Collection<MainAddressSearchResponse>,
        bpnToExternalIdMap: Map<String, String>
    ): List<SiteGateOutput> {
        val sitesByBpn = sites.associateBy { it.site.bpn }
        val mainAddressesByBpn = mainAddresses.associateBy { it.site }
        return bpnToExternalIdMap.mapNotNull { toSiteOutput(it.value, sitesByBpn[it.key], mainAddressesByBpn[it.key]) }
    }

    fun toSiteOutput(externalId: String, site: SitePartnerSearchResponse?, mainAddress: MainAddressSearchResponse?): SiteGateOutput? {
        if (site == null || mainAddress == null) {
            return null
        }
        return SiteGateOutput(
            site = SiteResponse(
                name = site.site.name
            ),
            mainAddress = mainAddress.mainAddress,
            externalId = externalId,
            bpn = site.site.bpn,
            legalEntityBpn = site.bpnLegalEntity
        )
    }

    /**
     * Upsert sites by:
     *
     * - Retrieving parent legal entities to check whether they exist and since their identifiers are copied to site
     * - Upserting the sites
     * - Retrieving the old relations of the sites and deleting them
     * - Upserting the new relations
     */
    fun upsertSites(sites: Collection<SiteGateInput>) {
        val parentLegalEntitiesByExternalId = getParentLegalEntities(sites)

        doUpsertSites(sites, parentLegalEntitiesByExternalId)

        deleteRelationsOfSites(sites)

        upsertRelations(sites)
    }

    private fun upsertRelations(sites: Collection<SiteGateInput>) {
        val relations = sites.map {
            CdqClient.SiteLegalEntityRelation(
                siteExternalId = it.externalId,
                legalEntityExternalId = it.legalEntityExternalId
            )
        }.toList()
        cdqClient.upsertSiteRelations(relations)
    }

    private fun doUpsertSites(
        sites: Collection<SiteGateInput>,
        parentLegalEntitiesByExternalId: Map<String, BusinessPartnerCdq>
    ) {
        val sitesCdq = sites.map { toCdqModel(it, parentLegalEntitiesByExternalId[it.legalEntityExternalId]) }
        cdqClient.upsertSites(sitesCdq)
    }

    private fun deleteRelationsOfSites(sites: Collection<SiteGateInput>) {
        val sitesPage = cdqClient.getSites(externalIds = sites.map { it.externalId })
        val relationsToDelete = sitesPage.values.flatMap { it.relations }.map { CdqMappings.toRelationToDelete(it) }
        if (relationsToDelete.isNotEmpty()) {
            cdqClient.deleteRelations(relationsToDelete)
        }
    }

    private fun toValidSiteInput(partner: BusinessPartnerCdq): SiteGateInput {
        if (!validateSiteBusinessPartner(partner)) {
            throw CdqInvalidRecordException(partner.id)
        }
        return inputCdqMappingService.toInputSite(partner)
    }

    private fun validateSiteBusinessPartner(partner: BusinessPartnerCdq): Boolean {
        var valid = true
        val logMessageStart = "CDQ business partner for site with ${if (partner.id != null) "CDQ ID " + partner.id else "external id " + partner.externalId}"

        valid = valid && validateAddresses(partner, logMessageStart)
        valid = valid && validateLegalEntityParents(partner, logMessageStart)
        valid = valid && validateNames(partner, logMessageStart)

        return valid
    }

    private fun validateNames(partner: BusinessPartnerCdq, logMessageStart: String): Boolean {
        if (partner.names.size > 1) {
            logger.warn { "$logMessageStart has multiple names." }
        }
        if (partner.names.isEmpty()) {
            logger.warn { "$logMessageStart does not have a name." }
            return false
        }
        return true
    }

    private fun validateAddresses(partner: BusinessPartnerCdq, logMessageStart: String): Boolean {
        if (partner.addresses.size > 1) {
            logger.warn { "$logMessageStart has multiple main addresses" }
        }
        if (partner.addresses.isEmpty()) {
            logger.warn { "$logMessageStart does not have a main address" }
            return false
        }
        return true
    }

    private fun validateLegalEntityParents(partner: BusinessPartnerCdq, logMessageStart: String): Boolean {
        val numLegalEntityParents = inputCdqMappingService.toParentLegalEntityExternalIds(partner.relations).size
        if (numLegalEntityParents > 1) {
            logger.warn { "$logMessageStart has multiple parent legal entities." }
        }
        if (numLegalEntityParents == 0) {
            logger.warn { "$logMessageStart does not have a parent legal entity." }
            return false
        }
        return true
    }

    private fun getParentLegalEntities(sites: Collection<SiteGateInput>): Map<String, BusinessPartnerCdq> {
        val parentLegalEntityExternalIds = sites.map { it.legalEntityExternalId }.distinct().toList()
        val parentLegalEntitiesPage = cdqClient.getLegalEntities(externalIds = parentLegalEntityExternalIds)
        if (parentLegalEntitiesPage.limit < parentLegalEntityExternalIds.size) {
            // should not happen as long as configured upsert limit is lower than cdq's limit
            throw IllegalStateException("Could not fetch all parent legal entities in single request.")
        }
        return parentLegalEntitiesPage.values.associateBy { it.externalId!! }
    }

    private fun toCdqModel(site: SiteGateInput, parentLegalEntity: BusinessPartnerCdq?): BusinessPartnerCdq {
        if (parentLegalEntity == null) {
            throw CdqNonexistentParentException(site.legalEntityExternalId)
        }
        val siteCdq = cdqRequestMappingService.toCdqModel(site)
        val parentIdentifiersWithoutBpn = parentLegalEntity.identifiers.filter { it.type?.technicalKey != bpnConfigProperties.id }
        return siteCdq.copy(identifiers = siteCdq.identifiers.plus(parentIdentifiersWithoutBpn))
    }
}