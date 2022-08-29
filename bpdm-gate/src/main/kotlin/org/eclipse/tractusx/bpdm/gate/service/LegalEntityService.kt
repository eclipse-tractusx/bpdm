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
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.model.AddressType
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityGateInput
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityGateOutput
import org.eclipse.tractusx.bpdm.gate.dto.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.exception.CdqInvalidRecordException
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class LegalEntityService(
    private val cdqRequestMappingService: CdqRequestMappingService,
    private val inputCdqMappingService: InputCdqMappingService,
    private val outputCdqMappingService: OutputCdqMappingService,
    private val cdqClient: CdqClient
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

    fun getLegalEntitiesOutput(limit: Int, startAfter: String?, from: Instant?): PageStartAfterResponse<LegalEntityGateOutput> {
        val partnerCollection = cdqClient.getAugmentedLegalEntities(limit, startAfter, from)

        val validEntries = partnerCollection.values.filter { validateBusinessPartner(it.augmentedBusinessPartner!!) }

        return PageStartAfterResponse(
            total = partnerCollection.total,
            nextStartAfter = partnerCollection.nextStartAfter,
            content = validEntries.map { outputCdqMappingService.toOutput(it.augmentedBusinessPartner!!) },
            invalidEntries = partnerCollection.values.size - validEntries.size
        )
    }

    fun getLegalEntityByExternalIdOutput(externalId: String): LegalEntityGateOutput {
        val response = cdqClient.getAugmentedBusinessPartner(externalId)

        if (response.augmentedBusinessPartner == null) {
            throw BpdmNotFoundException("Legal Entity", externalId)
        }
        return toValidLegalEntityOutput(response.augmentedBusinessPartner!!)
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
        if (!partner.addresses.any { address -> address.types.any { type -> type.technicalKey == AddressType.LEGAL.name } }) {
            logger.warn { "CDQ business partner for legal entity with ${if (partner.id != null) "CDQ ID " + partner.id else "external id " + partner.externalId} does not have a legal address" }
            return false
        }

        return true
    }
}