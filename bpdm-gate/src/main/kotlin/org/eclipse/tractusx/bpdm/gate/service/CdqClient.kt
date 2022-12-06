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

import com.fasterxml.jackson.databind.ObjectMapper
import org.eclipse.tractusx.bpdm.common.dto.cdq.*
import org.eclipse.tractusx.bpdm.gate.config.CdqConfigProperties
import org.eclipse.tractusx.bpdm.gate.exception.CdqRequestException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Instant

private const val BUSINESS_PARTNER_PATH = "/businesspartners"
private const val FETCH_BUSINESS_PARTNER_PATH = "$BUSINESS_PARTNER_PATH/fetch"

private const val RELATIONS_PATH = "/relations"
private const val DELETE_RELATIONS_PATH = "$RELATIONS_PATH/delete"

private const val RELATION_TYPE_KEY = "PARENT"

private const val LOOKUP_PATH = "/businesspartners/lookup"

private const val VALIDATE_BUSINESS_PARTNER_PATH = "/businesspartners/validate"

@Service
class CdqClient(
    @Qualifier("cdqClient")
    private val webClient: WebClient,
    private val cdqConfigProperties: CdqConfigProperties,
    private val objectMapper: ObjectMapper
) {

    fun getAugmentedLegalEntities(limit: Int? = null, startAfter: String? = null, from: Instant? = null, externalIds: Collection<String>? = null) =
        getAugmentedBusinessPartners(limit, startAfter, from, externalIds, cdqConfigProperties.legalEntityType)

    fun getAugmentedSites(limit: Int? = null, startAfter: String? = null, from: Instant? = null, externalIds: Collection<String>? = null) =
        getAugmentedBusinessPartners(limit, startAfter, from, externalIds, cdqConfigProperties.siteType)

    fun getAugmentedAddresses(limit: Int? = null, startAfter: String? = null, from: Instant? = null, externalIds: Collection<String>? = null) =
        getAugmentedBusinessPartners(limit, startAfter, from, externalIds, cdqConfigProperties.addressType)

    private fun getAugmentedBusinessPartners(
        limit: Int?,
        startAfter: String?,
        from: Instant?,
        externalIds: Collection<String>?,
        type: String
    ): PagedResponseCdq<AugmentedBusinessPartnerResponseCdq> {
        val partnerCollection = try {
            webClient
                .get()
                .uri { builder ->
                    builder
                        .path(cdqConfigProperties.dataClinicApiUrl + "/augmentedbusinesspartners")
                        .queryParam("dataSourceId", cdqConfigProperties.datasource)
                        .queryParam("typeTechnicalKeys", type)
                    if (limit != null) builder.queryParam("limit", limit)
                    if (startAfter != null) builder.queryParam("startAfter", startAfter)
                    if (from != null) builder.queryParam("from", from)
                    if (externalIds != null) builder.queryParam("externalIds", externalIds.joinToString(","))
                    builder.build()
                }
                .retrieve()
                .bodyToMono<PagedResponseCdq<AugmentedBusinessPartnerResponseCdq>>()
                .block()!!
        } catch (e: Exception) {
            throw CdqRequestException("Read augmented business partners request failed.", e)
        }
        return partnerCollection
    }

    fun deleteRelations(relations: Collection<DeleteRelationsRequestCdq.RelationToDeleteCdq>) {
        try {
            webClient
                .post()
                .uri(cdqConfigProperties.dataExchangeApiUrl + DELETE_RELATIONS_PATH)
                .bodyValue(objectMapper.writeValueAsString(DeleteRelationsRequestCdq(relations)))
                .retrieve()
                .bodyToMono<DeleteRelationsResponseCdq>()
                .block()!!
        } catch (e: Exception) {
            throw CdqRequestException("Delete relations request failed.", e)
        }
    }

    fun upsertLegalEntities(legalEntities: List<BusinessPartnerCdq>) {
        return upsertBusinessPartners(legalEntities)
    }

    fun upsertSites(sites: Collection<BusinessPartnerCdq>) {
        return upsertBusinessPartners(sites)
    }

    fun upsertAddresses(addresses: Collection<BusinessPartnerCdq>) {
        return upsertBusinessPartners(addresses)
    }

    private fun upsertBusinessPartners(businessPartners: Collection<BusinessPartnerCdq>) {
        val upsertRequest =
            UpsertRequest(
                cdqConfigProperties.datasource,
                businessPartners,
                listOf(UpsertRequest.CdqFeatures.UPSERT_BY_EXTERNAL_ID, UpsertRequest.CdqFeatures.API_ERROR_ON_FAILURES)
            )

        try {
            webClient
                .put()
                .uri(cdqConfigProperties.dataExchangeApiUrl + BUSINESS_PARTNER_PATH)
                .bodyValue(objectMapper.writeValueAsString(upsertRequest))
                .retrieve()
                .bodyToMono<UpsertResponse>()
                .block()!!
        } catch (e: Exception) {
            throw CdqRequestException("Upsert business partners request failed.", e)
        }
    }

    fun getBusinessPartner(externalId: String): FetchResponse {
        val fetchRequest = FetchRequest(cdqConfigProperties.datasource, externalId, featuresOn = listOf(FetchRequest.CdqFeatures.FETCH_RELATIONS))

        val fetchResponse = try {
            webClient
                .post()
                .uri(cdqConfigProperties.dataExchangeApiUrl + FETCH_BUSINESS_PARTNER_PATH)
                .bodyValue(objectMapper.writeValueAsString(fetchRequest))
                .retrieve()
                .bodyToMono<FetchResponse>()
                .block()!!
        } catch (e: Exception) {
            throw CdqRequestException("Fetch business partners request failed.", e)
        }
        return fetchResponse
    }

    fun getLegalEntities(limit: Int? = null, startAfter: String? = null, externalIds: List<String>? = null) =
        getBusinessPartners(limit, startAfter, externalIds, cdqConfigProperties.legalEntityType, listOf("USE_NEXT_START_AFTER"))

    fun getSites(limit: Int? = null, startAfter: String? = null, externalIds: List<String>? = null) =
        getBusinessPartners(limit, startAfter, externalIds, cdqConfigProperties.siteType, listOf("USE_NEXT_START_AFTER", "FETCH_RELATIONS"))

    fun getAddresses(limit: Int? = null, startAfter: String? = null, externalIds: List<String>? = null) =
        getBusinessPartners(limit, startAfter, externalIds, cdqConfigProperties.addressType, listOf("USE_NEXT_START_AFTER", "FETCH_RELATIONS"))

    fun lookUpReferenceData(lookupRequest: ReferenceDataLookupRequestCdq): ReferenceDataLookupResponseCdq {
        return try {
            webClient
                .post()
                .uri(cdqConfigProperties.referenceDataApiUrl + LOOKUP_PATH)
                .bodyValue(objectMapper.writeValueAsString(lookupRequest))
                .retrieve()
                .bodyToMono<ReferenceDataLookupResponseCdq>()
                .block()!!
        } catch (e: Throwable) {
            throw CdqRequestException("Lookup reference data request failed.", e)
        }
    }

    fun getBusinessPartners(
        limit: Int? = null,
        startAfter: String? = null,
        externalIds: Collection<String>? = null,
        type: String? = null,
        featuresOn: Collection<String>? = null
    ): PagedResponseCdq<BusinessPartnerCdq> {
        val partnerCollection = try {
            webClient
                .get()
                .uri { builder ->
                    builder
                        .path(cdqConfigProperties.dataExchangeApiUrl + BUSINESS_PARTNER_PATH)
                        .queryParam("dataSource", cdqConfigProperties.datasource)
                    if (type != null) builder.queryParam("typeTechnicalKeys", type)
                    if (startAfter != null) builder.queryParam("startAfter", startAfter)
                    if (limit != null) builder.queryParam("limit", limit)
                    if (!featuresOn.isNullOrEmpty()) builder.queryParam("featuresOn", featuresOn.joinToString(","))
                    if (!externalIds.isNullOrEmpty()) builder.queryParam("externalId", externalIds.joinToString(","))
                    builder.build()
                }
                .retrieve()
                .bodyToMono<PagedResponseCdq<BusinessPartnerCdq>>()
                .block()!!
        } catch (e: Exception) {
            e.printStackTrace()
            throw CdqRequestException("Get business partners request failed.", e)
        }
        return partnerCollection
    }

    fun upsertSiteRelations(relations: Collection<SiteLegalEntityRelation>) {
        val relationsCdq = relations.map {
            RelationCdq(
                startNode = it.legalEntityExternalId,
                startNodeDataSource = cdqConfigProperties.datasource,
                endNode = it.siteExternalId,
                endNodeDataSource = cdqConfigProperties.datasource,
                type = TypeKeyNameCdq(technicalKey = RELATION_TYPE_KEY)
            )
        }.toList()
        upsertBusinessPartnerRelations(relationsCdq)
    }

    fun upsertAddressRelations(legalEntityRelations: Collection<AddressLegalEntityRelation>, siteRelations: Collection<AddressSiteRelation>) {
        val legalEntityRelationsCdq = legalEntityRelations.map {
            RelationCdq(
                startNode = it.legalEntityExternalId,
                startNodeDataSource = cdqConfigProperties.datasource,
                endNode = it.addressExternalId,
                endNodeDataSource = cdqConfigProperties.datasource,
                type = TypeKeyNameCdq(technicalKey = RELATION_TYPE_KEY)
            )
        }.toList()
        val siteRelationsCdq = siteRelations.map {
            RelationCdq(
                startNode = it.siteExternalId,
                startNodeDataSource = cdqConfigProperties.datasource,
                endNode = it.addressExternalId,
                endNodeDataSource = cdqConfigProperties.datasource,
                type = TypeKeyNameCdq(technicalKey = RELATION_TYPE_KEY)
            )
        }.toList()
        upsertBusinessPartnerRelations(legalEntityRelationsCdq.plus(siteRelationsCdq))
    }

    fun validateBusinessPartner(validationRequest: ValidationRequestCdq): ValidationResponseCdq {
        return try {
            webClient
                .post()
                .uri(cdqConfigProperties.dataValidationApiUrl + VALIDATE_BUSINESS_PARTNER_PATH)
                .bodyValue(objectMapper.writeValueAsString(validationRequest))
                .retrieve()
                .bodyToMono<ValidationResponseCdq>()
                .block()!!
        } catch (e: Exception) {
            throw CdqRequestException("Validate business partner request failed.", e)
        }
    }

    private fun upsertBusinessPartnerRelations(relations: Collection<RelationCdq>) {
        val upsertRelationsRequest = UpsertRelationsRequestCdq(relations)
        val upsertResponse = try {
            webClient
                .put()
                .uri(cdqConfigProperties.dataExchangeApiUrl + RELATIONS_PATH)
                .bodyValue(objectMapper.writeValueAsString(upsertRelationsRequest))
                .retrieve()
                .bodyToMono<UpsertRelationsResponseCdq>()
                .block()!!
        } catch (e: Exception) {
            throw CdqRequestException("Upsert business partner relations request failed.", e)
        }

        if (upsertResponse.failures.isNotEmpty() || upsertResponse.numberOfFailed > 0) {
            throw CdqRequestException("Upsert business partner relations request failed for some relations.")
        }
    }

    data class SiteLegalEntityRelation(
        val siteExternalId: String,
        val legalEntityExternalId: String
    )

    data class AddressLegalEntityRelation(
        val addressExternalId: String,
        val legalEntityExternalId: String
    )

    data class AddressSiteRelation(
        val addressExternalId: String,
        val siteExternalId: String
    )
}