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
import org.eclipse.tractusx.bpdm.common.dto.response.LegalAddressSearchResponse
import org.eclipse.tractusx.bpdm.common.dto.response.LegalEntityPartnerResponse
import org.eclipse.tractusx.bpdm.common.dto.saas.BusinessPartnerSaas
import org.eclipse.tractusx.bpdm.common.dto.saas.FetchResponse
import org.eclipse.tractusx.bpdm.common.dto.saas.isError
import org.eclipse.tractusx.bpdm.common.exception.BpdmMappingException
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityGateInputRequest
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityGateInputResponse
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityGateOutput
import org.eclipse.tractusx.bpdm.gate.dto.response.ErrorInfo
import org.eclipse.tractusx.bpdm.gate.dto.response.PageOutputResponse
import org.eclipse.tractusx.bpdm.gate.dto.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.exception.BusinessPartnerOutputError
import org.springframework.stereotype.Service

@Service
class LegalEntityService(
    private val saasRequestMappingService: SaasRequestMappingService,
    private val inputSaasMappingService: InputSaasMappingService,
    private val outputSaasMappingService: OutputSaasMappingService,
    private val saasClient: SaasClient,
    private val poolClient: PoolClient,
) {

    private val logger = KotlinLogging.logger { }

    fun upsertLegalEntities(legalEntities: Collection<LegalEntityGateInputRequest>) {
        val legalEntitiesSaas = legalEntities.map { saasRequestMappingService.toSaasModel(it) }
        saasClient.upsertLegalEntities(legalEntitiesSaas)
    }

    fun getLegalEntityByExternalId(externalId: String): LegalEntityGateInputResponse {
        val fetchResponse = saasClient.getBusinessPartner(externalId)

        when (fetchResponse.status) {
            FetchResponse.Status.OK -> return inputSaasMappingService.toInputLegalEntity(fetchResponse.businessPartner!!)
            FetchResponse.Status.NOT_FOUND -> throw BpdmNotFoundException("Legal Entity", externalId)
        }
    }

    fun getLegalEntities(limit: Int, startAfter: String?): PageStartAfterResponse<LegalEntityGateInputResponse> {
        val partnerCollection = saasClient.getLegalEntities(limit, startAfter)

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
        val augmentedPartnerResponse = saasClient.getAugmentedLegalEntities(limit = limit, startAfter = startAfter, externalIds = externalIds)
        val augmentedPartnerWrapperCollection = augmentedPartnerResponse.values

        val bpnByExternalIdMap = outputSaasMappingService.buildBpnByExternalIdMap(augmentedPartnerWrapperCollection)

        val bpnList = bpnByExternalIdMap.values.filterNotNull()
        val legalEntitiesByBpnMap = poolClient.searchLegalEntities(bpnList).associateBy { it.bpn }
        val legalAddressesByBpnMap = poolClient.searchLegalAddresses(bpnList).associateBy { it.legalEntity }

        if (bpnList.size > legalEntitiesByBpnMap.size) {
            logger.warn { "Requested ${bpnList.size} legal entities from pool, but only ${legalEntitiesByBpnMap.size} were found." }
        }
        if (bpnList.size > legalAddressesByBpnMap.size) {
            logger.warn { "Requested ${bpnList.size} legal addresses from pool, but only ${legalAddressesByBpnMap.size} were found." }
        }

        // We need the sharing status from BusinessPartnerSaas
        val partnerResponse = saasClient.getLegalEntities(externalIds = bpnByExternalIdMap.keys)
        val partnerByExternalIdMap = partnerResponse.values.associateBy { it.externalId }

        // We sort all the entries in one of 3 buckets: valid content, errors or still pending
        val validContent = mutableListOf<LegalEntityGateOutput>()
        val errors = mutableListOf<ErrorInfo<BusinessPartnerOutputError>>()
        val pendingExternalIds = mutableListOf<String>()

        for ((externalId, bpn) in bpnByExternalIdMap) {
            // Business partner sharing
            val partner = partnerByExternalIdMap[externalId]
            val sharingStatus = partner?.metadata?.sharingStatus
            val sharingStatusType = sharingStatus?.status

            if (sharingStatusType == null || sharingStatusType.isError()) {
                // ERROR: SharingProcessError
                errors.add(
                    outputSaasMappingService.buildErrorInfoSharingProcessError(externalId, sharingStatus))
            } else if (bpn != null) {
                val legalEntity = legalEntitiesByBpnMap[bpn]
                val legalAddress = legalAddressesByBpnMap[bpn]
                if (legalEntity != null && legalAddress != null) {
                    // OKAY: entry found in pool
                    validContent.add(
                        toLegalEntityOutput(externalId, legalEntity, legalAddress)
                    )
                } else {
                    // ERROR: BpnNotInPool
                    errors.add(
                        outputSaasMappingService.buildErrorInfoBpnNotInPool(externalId, bpn))
                }
            } else if (outputSaasMappingService.isSharingTimeoutReached(partner)) {
                // ERROR: SharingTimeout
                errors.add(
                    outputSaasMappingService.buildErrorInfoSharingTimeout(externalId, partner.lastModifiedAt)
                )
            } else {
                pendingExternalIds.add(externalId)
            }
        }

        return PageOutputResponse(
            total = augmentedPartnerResponse.total,
            nextStartAfter = augmentedPartnerResponse.nextStartAfter,
            content = validContent,
            invalidEntries = augmentedPartnerWrapperCollection.size - validContent.size, // difference between all entries from SaaS and valid content
            pending = pendingExternalIds,
            errors = errors,
        )
    }

    fun toLegalEntityOutput(externalId: String, legalEntity: LegalEntityPartnerResponse, legalAddress: LegalAddressSearchResponse): LegalEntityGateOutput =
        LegalEntityGateOutput(
            legalEntity = legalEntity.properties,
            legalAddress = legalAddress.legalAddress,
            bpn = legalEntity.bpn,
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