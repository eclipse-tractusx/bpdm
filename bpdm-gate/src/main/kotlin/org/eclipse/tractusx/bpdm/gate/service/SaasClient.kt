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

import com.fasterxml.jackson.databind.ObjectMapper
import org.eclipse.tractusx.bpdm.common.dto.saas.*
import org.eclipse.tractusx.bpdm.gate.config.SaasConfigProperties
import org.eclipse.tractusx.bpdm.gate.exception.SaasRequestException
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
class SaasClient(
    @Qualifier("saasClient")
    private val webClient: WebClient,
    private val saasConfigProperties: SaasConfigProperties,
    private val objectMapper: ObjectMapper
) {

    fun getAugmentedLegalEntities(limit: Int? = null, startAfter: String? = null, from: Instant? = null, externalIds: Collection<String>? = null) =
        getAugmentedBusinessPartners(limit, startAfter, from, externalIds, saasConfigProperties.legalEntityType)

    fun getAugmentedSites(limit: Int? = null, startAfter: String? = null, from: Instant? = null, externalIds: Collection<String>? = null) =
        getAugmentedBusinessPartners(limit, startAfter, from, externalIds, saasConfigProperties.siteType)

    fun getAugmentedAddresses(limit: Int? = null, startAfter: String? = null, from: Instant? = null, externalIds: Collection<String>? = null) =
        getAugmentedBusinessPartners(limit, startAfter, from, externalIds, saasConfigProperties.addressType)

    private fun getAugmentedBusinessPartners(
        limit: Int?,
        startAfter: String?,
        from: Instant?,
        externalIds: Collection<String>?,
        type: String
    ): PagedResponseSaas<AugmentedBusinessPartnerResponseSaas> {
        val partnerCollection = try {
            webClient
                .get()
                .uri { builder ->
                    builder
                        .path(saasConfigProperties.dataClinicApiUrl + "/augmentedbusinesspartners")
                        .queryParam("dataSourceId", saasConfigProperties.datasource)
                        .queryParam("typeTechnicalKeys", type)
                    if (limit != null) builder.queryParam("limit", limit)
                    if (startAfter != null) builder.queryParam("startAfter", startAfter)
                    if (from != null) builder.queryParam("from", from)
                    if (externalIds != null) builder.queryParam("externalIds", externalIds.joinToString(","))
                    builder.build()
                }
                .retrieve()
                .bodyToMono<PagedResponseSaas<AugmentedBusinessPartnerResponseSaas>>()
                .block()!!
        } catch (e: Exception) {
            throw SaasRequestException("Read augmented business partners request failed.", e)
        }
        return partnerCollection
    }

    fun deleteRelations(relations: Collection<DeleteRelationsRequestSaas.RelationToDeleteSaas>) {
        try {
            webClient
                .post()
                .uri(saasConfigProperties.dataExchangeApiUrl + DELETE_RELATIONS_PATH)
                .bodyValue(objectMapper.writeValueAsString(DeleteRelationsRequestSaas(relations)))
                .retrieve()
                .bodyToMono<DeleteRelationsResponseSaas>()
                .block()!!
        } catch (e: Exception) {
            throw SaasRequestException("Delete relations request failed.", e)
        }
    }

    fun upsertLegalEntities(legalEntities: List<BusinessPartnerSaas>) {
        return upsertBusinessPartners(legalEntities)
    }

    fun upsertSites(sites: Collection<BusinessPartnerSaas>) {
        return upsertBusinessPartners(sites)
    }

    fun upsertAddresses(addresses: Collection<BusinessPartnerSaas>) {
        return upsertBusinessPartners(addresses)
    }

    private fun upsertBusinessPartners(businessPartners: Collection<BusinessPartnerSaas>) {
        val upsertRequest =
            UpsertRequest(
                saasConfigProperties.datasource,
                businessPartners,
                listOf(UpsertRequest.SaasFeatures.UPSERT_BY_EXTERNAL_ID, UpsertRequest.SaasFeatures.API_ERROR_ON_FAILURES)
            )

        try {
            webClient
                .put()
                .uri(saasConfigProperties.dataExchangeApiUrl + BUSINESS_PARTNER_PATH)
                .bodyValue(objectMapper.writeValueAsString(upsertRequest))
                .retrieve()
                .bodyToMono<UpsertResponse>()
                .block()!!
        } catch (e: Exception) {
            throw SaasRequestException("Upsert business partners request failed.", e)
        }
    }

    fun getBusinessPartner(externalId: String): FetchResponse {
        val fetchRequest = FetchRequest(saasConfigProperties.datasource, externalId, featuresOn = listOf(FetchRequest.SaasFeatures.FETCH_RELATIONS))

        val fetchResponse = try {
            webClient
                .post()
                .uri(saasConfigProperties.dataExchangeApiUrl + FETCH_BUSINESS_PARTNER_PATH)
                .bodyValue(objectMapper.writeValueAsString(fetchRequest))
                .retrieve()
                .bodyToMono<FetchResponse>()
                .block()!!
        } catch (e: Exception) {
            throw SaasRequestException("Fetch business partners request failed.", e)
        }
        return fetchResponse
    }

    fun getLegalEntities(limit: Int? = null, startAfter: String? = null, externalIds: Collection<String>? = null) =
        getBusinessPartners(limit, startAfter, externalIds, saasConfigProperties.legalEntityType, listOf("USE_NEXT_START_AFTER"))

    fun getSites(limit: Int? = null, startAfter: String? = null, externalIds: Collection<String>? = null) =
        getBusinessPartners(limit, startAfter, externalIds, saasConfigProperties.siteType, listOf("USE_NEXT_START_AFTER", "FETCH_RELATIONS"))

    fun getAddresses(limit: Int? = null, startAfter: String? = null, externalIds: Collection<String>? = null) =
        getBusinessPartners(limit, startAfter, externalIds, saasConfigProperties.addressType, listOf("USE_NEXT_START_AFTER", "FETCH_RELATIONS"))

    fun lookUpReferenceData(lookupRequest: ReferenceDataLookupRequestSaas): ReferenceDataLookupResponseSaas {
        return try {
            webClient
                .post()
                .uri(saasConfigProperties.referenceDataApiUrl + LOOKUP_PATH)
                .bodyValue(objectMapper.writeValueAsString(lookupRequest))
                .retrieve()
                .bodyToMono<ReferenceDataLookupResponseSaas>()
                .block()!!
        } catch (e: Throwable) {
            throw SaasRequestException("Lookup reference data request failed.", e)
        }
    }

    fun getBusinessPartners(
        limit: Int? = null,
        startAfter: String? = null,
        externalIds: Collection<String>? = null,
        type: String? = null,
        featuresOn: Collection<String>? = null
    ): PagedResponseSaas<BusinessPartnerSaas> {
        val partnerCollection = try {
            webClient
                .get()
                .uri { builder ->
                    builder
                        .path(saasConfigProperties.dataExchangeApiUrl + BUSINESS_PARTNER_PATH)
                        .queryParam("dataSource", saasConfigProperties.datasource)
                    if (type != null) builder.queryParam("typeTechnicalKeys", type)
                    if (startAfter != null) builder.queryParam("startAfter", startAfter)
                    if (limit != null) builder.queryParam("limit", limit)
                    if (!featuresOn.isNullOrEmpty()) builder.queryParam("featuresOn", featuresOn.joinToString(","))
                    if (!externalIds.isNullOrEmpty()) builder.queryParam("externalId", externalIds.joinToString(","))
                    builder.build()
                }
                .retrieve()
                .bodyToMono<PagedResponseSaas<BusinessPartnerSaas>>()
                .block()!!
        } catch (e: Exception) {
            e.printStackTrace()
            throw SaasRequestException("Get business partners request failed.", e)
        }
        return partnerCollection
    }

    fun upsertSiteRelations(relations: Collection<SiteLegalEntityRelation>) {
        val relationsSaas = relations.map {
            RelationSaas(
                startNode = it.legalEntityExternalId,
                startNodeDataSource = saasConfigProperties.datasource,
                endNode = it.siteExternalId,
                endNodeDataSource = saasConfigProperties.datasource,
                type = TypeKeyNameSaas(technicalKey = RELATION_TYPE_KEY)
            )
        }.toList()
        upsertBusinessPartnerRelations(relationsSaas)
    }

    fun upsertAddressRelations(legalEntityRelations: Collection<AddressLegalEntityRelation>, siteRelations: Collection<AddressSiteRelation>) {
        val legalEntityRelationsSaas = legalEntityRelations.map {
            RelationSaas(
                startNode = it.legalEntityExternalId,
                startNodeDataSource = saasConfigProperties.datasource,
                endNode = it.addressExternalId,
                endNodeDataSource = saasConfigProperties.datasource,
                type = TypeKeyNameSaas(technicalKey = RELATION_TYPE_KEY)
            )
        }.toList()
        val siteRelationsSaas = siteRelations.map {
            RelationSaas(
                startNode = it.siteExternalId,
                startNodeDataSource = saasConfigProperties.datasource,
                endNode = it.addressExternalId,
                endNodeDataSource = saasConfigProperties.datasource,
                type = TypeKeyNameSaas(technicalKey = RELATION_TYPE_KEY)
            )
        }.toList()
        upsertBusinessPartnerRelations(legalEntityRelationsSaas.plus(siteRelationsSaas))
    }

    fun validateBusinessPartner(validationRequest: ValidationRequestSaas): ValidationResponseSaas {
        return try {
            webClient
                .post()
                .uri(saasConfigProperties.dataValidationApiUrl + VALIDATE_BUSINESS_PARTNER_PATH)
                .bodyValue(objectMapper.writeValueAsString(validationRequest))
                .retrieve()
                .bodyToMono<ValidationResponseSaas>()
                .block()!!
        } catch (e: Exception) {
            throw SaasRequestException("Validate business partner request failed.", e)
        }
    }

    private fun upsertBusinessPartnerRelations(relations: Collection<RelationSaas>) {
        val upsertRelationsRequest = UpsertRelationsRequestSaas(relations)
        val upsertResponse = try {
            webClient
                .put()
                .uri(saasConfigProperties.dataExchangeApiUrl + RELATIONS_PATH)
                .bodyValue(objectMapper.writeValueAsString(upsertRelationsRequest))
                .retrieve()
                .bodyToMono<UpsertRelationsResponseSaas>()
                .block()!!
        } catch (e: Exception) {
            throw SaasRequestException("Upsert business partner relations request failed.", e)
        }

        if (upsertResponse.failures.isNotEmpty() || upsertResponse.numberOfFailed > 0) {
            throw SaasRequestException("Upsert business partner relations request failed for some relations.")
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