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
import org.eclipse.tractusx.bpdm.common.dto.saas.BusinessPartnerSaas
import org.eclipse.tractusx.bpdm.common.dto.saas.ReferenceDataLookupRequestSaas
import org.eclipse.tractusx.bpdm.gate.config.SaasConfigProperties
import org.eclipse.tractusx.bpdm.gate.config.TypeMatchConfigProperties
import org.eclipse.tractusx.bpdm.gate.dto.BusinessPartnerCandidateDto
import org.eclipse.tractusx.bpdm.gate.dto.response.LsaType
import org.eclipse.tractusx.bpdm.gate.dto.response.TypeMatchResponse
import org.springframework.stereotype.Service

@Service
class TypeMatchingService(
    private val saasClient: SaasClient,
    private val saasLookupMappingService: SaasLookupMappingService,
    private val typeMatchConfigProperties: TypeMatchConfigProperties,
    private val saasConfigProperties: SaasConfigProperties
) {
    private val logger = KotlinLogging.logger { }

    /**
     * For a candidate DTO determines its likely LSA type
     * Currently can only distinguish between legal entity or nothing
     */
    fun determineCandidateType(partner: BusinessPartnerCandidateDto): TypeMatchResponse {
        val request = ReferenceDataLookupRequestSaas(
            matchingThreshold = typeMatchConfigProperties.legalEntityThreshold,
            pageSize = 1,
            page = 0,
            maxCandidates = 1,
            businessPartner = saasLookupMappingService.toCdq(partner)
        )
        val response = saasClient.lookUpReferenceData(request)

        val bestOverall = response.values.maxOfOrNull { it.matchingProfile.matchingScores.overall.value } ?: 0f

        return if (bestOverall >= typeMatchConfigProperties.legalEntityThreshold)
            TypeMatchResponse(bestOverall, LsaType.LegalEntity)
        else

            TypeMatchResponse(1f - bestOverall, LsaType.None)

    }

    /**
     * Partitions the input into legal entities and sites for a given collection.
     * Filters out any addresses with a warning since they can't be parents
     */
    fun partitionIntoParentTypes(parents: Collection<BusinessPartnerSaas>): Pair<Collection<BusinessPartnerSaas>, Collection<BusinessPartnerSaas>> {
        val typeGroups = parents.groupBy { determineType(it) }

        typeGroups.keys
            .filterNot { it == LsaType.LegalEntity || it == LsaType.Site }
            .forEach { invalidGroup ->
                typeGroups[invalidGroup]!!.forEach {
                    logger.warn { "Business Partner with ID ${it.id} is parent with invalid type $invalidGroup" }
                }
            }

        return Pair(typeGroups[LsaType.LegalEntity] ?: emptyList(), typeGroups[LsaType.Site] ?: emptyList())
    }

    fun determineType(partner: BusinessPartnerSaas): LsaType {
        if (partner.types.isEmpty()) {
            logger.warn { "Partner with ID ${partner.id} does not have any type" }
            return LsaType.None
        }

        if (partner.types.size > 1) {
            logger.warn { "Partner with ID ${partner.id} has several types" }
        }

        val partnerType = partner.types.first()
        return when (partnerType.technicalKey) {
            saasConfigProperties.legalEntityType -> LsaType.LegalEntity
            saasConfigProperties.siteType -> LsaType.Site
            saasConfigProperties.addressType -> LsaType.Address
            else -> LsaType.None
        }
    }
}