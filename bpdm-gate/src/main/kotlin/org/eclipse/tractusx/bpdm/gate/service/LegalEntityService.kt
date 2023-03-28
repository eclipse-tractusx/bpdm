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

package org.eclipse.tractusx.bpdm.gate.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.response.LegalEntityResponse
import org.eclipse.tractusx.bpdm.common.dto.response.LogisticAddressResponse
import org.eclipse.tractusx.bpdm.common.dto.saas.BusinessPartnerSaas
import org.eclipse.tractusx.bpdm.common.dto.saas.FetchResponse
import org.eclipse.tractusx.bpdm.common.exception.BpdmMappingException
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGateInputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGateOutput
import org.eclipse.tractusx.bpdm.gate.api.model.response.LsaType
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageOutputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntry
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository
import org.springframework.stereotype.Service

@Service
class LegalEntityService(
    private val saasRequestMappingService: SaasRequestMappingService,
    private val inputSaasMappingService: InputSaasMappingService,
    private val outputSaasMappingService: OutputSaasMappingService,
    private val saasClient: SaasClient,
    private val poolClient: PoolClient,
    private val changelogRepository: ChangelogRepository
) {

    private val logger = KotlinLogging.logger { }

    fun upsertLegalEntities(legalEntities: Collection<LegalEntityGateInputRequest>) {


        val legalEntitiesSaas = legalEntities.map { saasRequestMappingService.toSaasModel(it) }
        saasClient.upsertLegalEntities(legalEntitiesSaas)

        // create changelog entry if all goes well from saasClient
        legalEntities.forEach { legalEntity ->
            changelogRepository.save(ChangelogEntry(legalEntity.externalId, LsaType.LegalEntity))
        }
    }

    fun getLegalEntityByExternalId(externalId: String): LegalEntityGateInputResponse {
        val fetchResponse = saasClient.getBusinessPartner(externalId)

        when (fetchResponse.status) {
            FetchResponse.Status.OK -> return inputSaasMappingService.toInputLegalEntity(fetchResponse.businessPartner!!)
            FetchResponse.Status.NOT_FOUND -> throw BpdmNotFoundException("Legal Entity", externalId)
        }
    }

    fun getLegalEntities(limit: Int, startAfter: String?, externalIds: Collection<String>? = null): PageStartAfterResponse<LegalEntityGateInputResponse> {
        val partnerCollection = saasClient.getLegalEntities(limit, startAfter, externalIds)

        val validEntries = toValidLegalEntities(partnerCollection.values)

        return PageStartAfterResponse(
            total = partnerCollection.total,
            nextStartAfter = partnerCollection.nextStartAfter,
            content = validEntries,
            invalidEntries = partnerCollection.values.size - validEntries.size
        )
    }

    /**
     * Get legal entities by first fetching legal entities from "augmented business partners" in SaaS. Augmented business partners from SaaS should contain a BPN,
     * which is then used to fetch the data for the legal entities from the bpdm pool.
     */
    fun getLegalEntitiesOutput(externalIds: Collection<String>?, limit: Int, startAfter: String?): PageOutputResponse<LegalEntityGateOutput> {
        val partnerResponse = saasClient.getLegalEntities(limit = limit, startAfter = startAfter, externalIds = externalIds)
        val partners = partnerResponse.values

        val partnersWithExternalId = outputSaasMappingService.mapWithExternalId(partners)
        val augmentedPartnerResponse = saasClient.getAugmentedLegalEntities(externalIds = partnersWithExternalId.map { it.externalId })
        val partnersWithLocalBpn = outputSaasMappingService.mapWithLocalBpn(partnersWithExternalId, augmentedPartnerResponse.values)

        //Search entries in the pool with BPNs found in the local mirror
        val bpnSet = partnersWithLocalBpn.map { it.bpn }.toSet()
        val legalEntitiesByBpnMap = poolClient.searchLegalEntities(bpnSet).associateBy { it.bpn }
        val legalAddressesByBpnMap = poolClient.searchLegalAddresses(bpnSet).associateBy { it.bpnLegalEntity }

        if (bpnSet.size > legalEntitiesByBpnMap.size) {
            logger.warn { "Requested ${bpnSet.size} legal entities from pool, but only ${legalEntitiesByBpnMap.size} were found." }
        }
        if (bpnSet.size > legalAddressesByBpnMap.size) {
            logger.warn { "Requested ${bpnSet.size} legal addresses from pool, but only ${legalAddressesByBpnMap.size} were found." }
        }

        //Filter only legal entities which can be found with their legal address  in the Pool under the given local BPN
        val partnersWithPoolBpn = partnersWithLocalBpn.filter { legalEntitiesByBpnMap[it.bpn] != null && legalAddressesByBpnMap[it.bpn] != null }
        val bpnByExternalIdMap = partnersWithPoolBpn.associate { Pair(it.externalId, it.bpn) }

        //Evaluate the sharing status of the legal entities
        val sharingStatus = outputSaasMappingService.evaluateSharingStatus(partners, partnersWithLocalBpn, partnersWithPoolBpn)

        val validLegalEntities = sharingStatus.validExternalIds.map { externalId ->
            val bpn = bpnByExternalIdMap[externalId]!!
            val legalEntity = legalEntitiesByBpnMap[bpn]!!
            val legalAddress = legalAddressesByBpnMap[bpn]!!
            toLegalEntityOutput(externalId, legalEntity, legalAddress)
        }

        return PageOutputResponse(
            total = partnerResponse.total,
            nextStartAfter = partnerResponse.nextStartAfter,
            content = validLegalEntities,
            invalidEntries = partners.size - sharingStatus.validExternalIds.size, // difference between all entries from SaaS and valid content
            pending = sharingStatus.pendingExternalIds,
            errors = sharingStatus.errors,
        )
    }

    fun toLegalEntityOutput(externalId: String, legalEntity: LegalEntityResponse, legalAddress: LogisticAddressResponse): LegalEntityGateOutput =
        LegalEntityGateOutput(
            legalEntity = legalEntity,
            legalAddress = legalAddress,
            externalId = externalId
        )

    private fun toValidLegalEntities(partners: Collection<BusinessPartnerSaas>): Collection<LegalEntityGateInputResponse> {
        return partners.mapNotNull {
            val logMessageStart =
                "SaaS business partner for legal entity with ID ${it.id ?: "Unknown"}"

            try {
                if (it.addresses.size > 1) {
                    logger.warn { "$logMessageStart has multiple legal addresses." }
                }

                inputSaasMappingService.toInputLegalEntity(it)
            } catch (e: BpdmMappingException) {
                logger.warn { "$logMessageStart will be ignored: ${e.message}" }
                null
            }
        }
    }
}