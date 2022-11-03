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
import org.eclipse.tractusx.bpdm.common.dto.response.LegalAddressSearchResponse
import org.eclipse.tractusx.bpdm.common.dto.response.LegalEntityPartnerResponse
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.service.CdqMappings
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityGateInput
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityGateOutput
import org.eclipse.tractusx.bpdm.gate.dto.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.exception.CdqInvalidRecordException
import org.eclipse.tractusx.bpdm.gate.filterNotNullKeys
import org.eclipse.tractusx.bpdm.gate.filterNotNullValues
import org.springframework.stereotype.Service

@Service
class LegalEntityService(
    private val cdqRequestMappingService: CdqRequestMappingService,
    private val inputCdqMappingService: InputCdqMappingService,
    private val outputCdqMappingService: OutputCdqMappingService,
    private val cdqClient: CdqClient,
    private val poolClient: PoolClient
) {

    private val logger = KotlinLogging.logger { }

    fun upsertLegalEntities(legalEntities: Collection<LegalEntityGateInput>) {
        val legalEntitiesCdq = legalEntities.map { cdqRequestMappingService.toCdqModel(it) }
        cdqClient.upsertLegalEntities(legalEntitiesCdq)
    }

    fun getLegalEntityByExternalId(externalId: String): LegalEntityGateInput {
        val fetchResponse = cdqClient.getBusinessPartner(externalId)

        when (fetchResponse.status) {
            FetchResponse.Status.OK -> return toValidLegalEntityInput(fetchResponse.businessPartner!!)
            FetchResponse.Status.NOT_FOUND -> throw BpdmNotFoundException("Legal Entity", externalId)
        }
    }

    fun getLegalEntities(limit: Int, startAfter: String?): PageStartAfterResponse<LegalEntityGateInput> {
        val partnerCollection = cdqClient.getLegalEntities(limit, startAfter)

        val validEntries = partnerCollection.values.filter { validateBusinessPartner(it) }

        return PageStartAfterResponse(
            total = partnerCollection.total,
            nextStartAfter = partnerCollection.nextStartAfter,
            content = validEntries.map { inputCdqMappingService.toInputLegalEntity(it) },
            invalidEntries = partnerCollection.values.size - validEntries.size
        )
    }

    /**
     * Get legal entities by first fetching legal entities from "augmented business partners" in CDQ. Augmented business partners from CDQ should contain a BPN,
     * which is then used to fetch the data for the legal entities from the bpdm pool.
     */
    fun getLegalEntitiesOutput(externalIds: Collection<String>?, limit: Int, startAfter: String?): PageStartAfterResponse<LegalEntityGateOutput> {
        val partnerCollection = cdqClient.getAugmentedLegalEntities(limit = limit, startAfter = startAfter, externalIds = externalIds)

        val bpnToExternalIdMapNullable =
            partnerCollection.values.mapNotNull { it.augmentedBusinessPartner }.associateBy({ CdqMappings.findBpn(it.identifiers) }, { it.externalId })
        val numLegalEntitiesWithoutBpn = bpnToExternalIdMapNullable.filter { it.key == null }.size
        val numLegalEntitiesWithoutExternalId = bpnToExternalIdMapNullable.filter { it.value == null }.size

        if (numLegalEntitiesWithoutBpn > 0) {
            logger.warn { "Encountered $numLegalEntitiesWithoutBpn legal entities without BPN in CDQ. Can't retrieve data from pool for these." }
        }
        if (numLegalEntitiesWithoutExternalId > 0) {
            logger.warn { "Encountered $numLegalEntitiesWithoutExternalId legal entities without external id in CDQ." }
        }

        val bpnToExternalIdMap = bpnToExternalIdMapNullable.filterNotNullKeys().filterNotNullValues()

        val bpnLs = bpnToExternalIdMap.keys
        val legalEntities = poolClient.searchLegalEntities(bpnLs)
        val legalAddresses = poolClient.searchLegalAddresses(bpnLs)

        if (bpnLs.size > legalEntities.size) {
            logger.warn { "Requested ${bpnLs.size} legal entities from pool, but only ${legalEntities.size} were found." }
        }
        if (bpnLs.size > legalAddresses.size) {
            logger.warn { "Requested ${bpnLs.size} legal addresses from pool, but only ${legalAddresses.size} were found." }
        }

        val legalEntitiesOutput = toLegalEntitiesOutput(legalEntities, legalAddresses, bpnToExternalIdMap)

        return PageStartAfterResponse(
            total = partnerCollection.total,
            nextStartAfter = partnerCollection.nextStartAfter,
            content = legalEntitiesOutput,
            invalidEntries = partnerCollection.values.size - legalEntitiesOutput.size // difference of what gate can return to values in cdq
        )
    }

    private fun toLegalEntitiesOutput(
        legalEntities: Collection<LegalEntityPartnerResponse>,
        legalAddresses: Collection<LegalAddressSearchResponse>,
        bpnToExternalIdMap: Map<String, String>
    ): List<LegalEntityGateOutput> {
        val legalEntitiesByBpn = legalEntities.associateBy { it.bpn }
        val legalAddressesByBpn = legalAddresses.associateBy { it.legalEntity }
        return bpnToExternalIdMap.mapNotNull { toLegalEntityOutput(it.value, legalEntitiesByBpn[it.key], legalAddressesByBpn[it.key]) }
    }

    fun toLegalEntityOutput(externalId: String, legalEntity: LegalEntityPartnerResponse?, legalAddress: LegalAddressSearchResponse?): LegalEntityGateOutput? {
        if (legalEntity == null || legalAddress == null) {
            return null
        }

        return LegalEntityGateOutput(
            legalEntity = legalEntity.properties,
            legalAddress = legalAddress.legalAddress,
            bpn = legalEntity.bpn,
            externalId = externalId
        )
    }

    private fun toValidLegalEntityOutput(partner: BusinessPartnerCdq): LegalEntityGateOutput {
        if (!validateBusinessPartner(partner)) {
            throw CdqInvalidRecordException(partner.id)
        }
        return outputCdqMappingService.toOutput(partner)
    }

    private fun toValidLegalEntityInput(partner: BusinessPartnerCdq): LegalEntityGateInput {
        if (!validateBusinessPartner(partner)) {
            throw CdqInvalidRecordException(partner.id)
        }
        return inputCdqMappingService.toInputLegalEntity(partner)
    }

    private fun validateBusinessPartner(partner: BusinessPartnerCdq): Boolean {
        val logMessageStart =
            "CDQ business partner for legal entity with ${if (partner.id != null) "CDQ ID " + partner.id else "external id " + partner.externalId}"

        if (partner.addresses.size > 1) {
            logger.warn { "$logMessageStart has multiple legal addresses" }
        }
        if (partner.addresses.isEmpty()) {
            logger.warn { "$logMessageStart does not have a legal address" }
            return false
        }

        if (partner.addresses.first().thoroughfares.any { it.value == null }) {
            logger.warn { "$logMessageStart has legal address with empty thoroughfare values" }
            return false
        }

        return true
    }
}