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

package org.eclipse.tractusx.bpdm.pool.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions
import org.assertj.core.api.RecursiveComparisonAssert
import org.eclipse.tractusx.bpdm.common.dto.cdq.BusinessPartnerCdq
import org.eclipse.tractusx.bpdm.common.dto.cdq.PagedResponseCdq
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.component.cdq.config.CdqAdapterConfigProperties
import org.eclipse.tractusx.bpdm.pool.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.pool.dto.response.*
import org.eclipse.tractusx.bpdm.pool.entity.SyncStatus
import org.springframework.stereotype.Component
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Instant
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.eclipse.tractusx.bpdm.pool.exception.ErrorCode

private const val ASYNC_TIMEOUT_IN_MS: Long = 5 * 1000 //5 seconds
private const val ASYNC_CHECK_INTERVAL_IN_MS: Long = 200
private const val BPDM_DB_SCHEMA_NAME: String = "bpdm"

@Component
class TestHelpers(
    entityManagerFactory: EntityManagerFactory,
    private val objectMapper: ObjectMapper,
    private val cdqAdapterConfigProperties: CdqAdapterConfigProperties,
    private val bpnConfigProperties: BpnConfigProperties
) {

    val bpnLPattern = createBpnPattern(bpnConfigProperties.legalEntityChar)
    val bpnSPattern = createBpnPattern(bpnConfigProperties.siteChar)
    val bpnAPattern = createBpnPattern(bpnConfigProperties.addressChar)


    val em: EntityManager = entityManagerFactory.createEntityManager()

    fun truncateDbTables() {
        em.transaction.begin()

        em.createNativeQuery(
            """
            DO $$ DECLARE table_names RECORD;
            BEGIN
                FOR table_names IN SELECT table_name
                    FROM information_schema.tables
                    WHERE table_schema='$BPDM_DB_SCHEMA_NAME'
                    AND table_name NOT IN ('flyway_schema_history') 
                LOOP 
                    EXECUTE format('TRUNCATE TABLE $BPDM_DB_SCHEMA_NAME.%I CONTINUE IDENTITY CASCADE;', table_names.table_name);
                END LOOP;
            END $$;
        """.trimIndent()
        ).executeUpdate()

        em.transaction.commit()
    }


    /**
     * Creates legal entities, sites and addresses according to the given [partnerStructures]
     * Retains the order: All response objects will be in the same order as their request counterparts
     * Assumption: Legal entities, sites and addresses have unique indexes among them each
     */
    fun createBusinessPartnerStructure(
        partnerStructures: List<LegalEntityStructureRequest>,
        client: WebTestClient
    ): List<LegalEntityStructureResponse> {

        val legalEntities = client.invokePostEndpoint<LegalEntityPartnerCreateResponseWrapper>(
            EndpointValues.CATENA_LEGAL_ENTITY_PATH,
            partnerStructures.map { it.legalEntity })
            .entities
        val indexedLegalEntities = legalEntities.associateBy { it.index }

        val assignedSiteRequests =
            partnerStructures.flatMap { it.siteStructures.map { site -> site.site.copy(legalEntity = indexedLegalEntities[it.legalEntity.index]!!.bpn) } }
        val sitesWithErrorsResponse = client.invokePostEndpoint<SitePartnerCreateResponseWrapper>(EndpointValues.CATENA_SITES_PATH, assignedSiteRequests)
        val indexedSites = sitesWithErrorsResponse.entities.associateBy { it.index }

        val assignedSitelessAddresses =
            partnerStructures.flatMap { it.addresses.map { address -> address.copy(parent = indexedLegalEntities[it.legalEntity.index]!!.bpn) } }
        val assignedSiteAddresses =
            partnerStructures.flatMap { it.siteStructures }.flatMap { it.addresses.map { address -> address.copy(parent = indexedSites[it.site.index]!!.bpn) } }

        val addresses =
            client.invokePostEndpoint<AddressPartnerCreateResponseWrapper>(
                EndpointValues.CATENA_ADDRESSES_PATH,
                assignedSitelessAddresses + assignedSiteAddresses
            )
        val indexedAddresses = addresses.entities.associateBy { it.index }

        return partnerStructures.map { legalEntityStructure ->
            LegalEntityStructureResponse(
                legalEntity = indexedLegalEntities[legalEntityStructure.legalEntity.index]!!,
                siteStructures = legalEntityStructure.siteStructures.map { siteStructure ->
                    SiteStructureResponse(
                        site = indexedSites[siteStructure.site.index]!!,
                        addresses = siteStructure.addresses.map { indexedAddresses[it.index]!! }
                    )
                },
                addresses = legalEntityStructure.addresses.map { indexedAddresses[it.index]!! }
            )
        }
    }

    /**
     * Creates metadata needed for test data defined in the [RequestValues]
     */
    fun createTestMetadata(client: WebTestClient) {
        client.invokePostEndpointWithoutResponse(EndpointValues.CATENA_METADATA_LEGAL_FORM_PATH, RequestValues.legalForm1)
        client.invokePostEndpointWithoutResponse(EndpointValues.CATENA_METADATA_LEGAL_FORM_PATH, RequestValues.legalForm2)
        client.invokePostEndpointWithoutResponse(EndpointValues.CATENA_METADATA_LEGAL_FORM_PATH, RequestValues.legalForm3)

        client.invokePostEndpointWithoutResponse(EndpointValues.CATENA_METADATA_IDENTIFIER_TYPE_PATH, RequestValues.identifierType1)
        client.invokePostEndpointWithoutResponse(EndpointValues.CATENA_METADATA_IDENTIFIER_TYPE_PATH, RequestValues.identifierType2)
        client.invokePostEndpointWithoutResponse(EndpointValues.CATENA_METADATA_IDENTIFIER_TYPE_PATH, RequestValues.identifierType3)

        client.invokePostEndpointWithoutResponse(EndpointValues.CATENA_METADATA_ISSUING_BODY_PATH, RequestValues.issuingBody1)
        client.invokePostEndpointWithoutResponse(EndpointValues.CATENA_METADATA_ISSUING_BODY_PATH, RequestValues.issuingBody2)
        client.invokePostEndpointWithoutResponse(EndpointValues.CATENA_METADATA_ISSUING_BODY_PATH, RequestValues.issuingBody3)

        client.invokePostEndpointWithoutResponse(EndpointValues.CATENA_METADATA_IDENTIFIER_STATUS_PATH, RequestValues.identifierStatus1)
        client.invokePostEndpointWithoutResponse(EndpointValues.CATENA_METADATA_IDENTIFIER_STATUS_PATH, RequestValues.identifierStatus2)
        client.invokePostEndpointWithoutResponse(EndpointValues.CATENA_METADATA_IDENTIFIER_STATUS_PATH, RequestValues.identifierStatus3)
    }


    fun startSyncAndAwaitSuccess(client: WebTestClient, syncPath: String): SyncResponse {
        return startSyncAndAwaitResult(client, syncPath, SyncStatus.SUCCESS)
    }

    fun startSyncAndAwaitError(client: WebTestClient, syncPath: String): SyncResponse {
        return startSyncAndAwaitResult(client, syncPath, SyncStatus.ERROR)
    }

    private fun startSyncAndAwaitResult(client: WebTestClient, syncPath: String, status: SyncStatus): SyncResponse {
        client.invokePostEndpointWithoutResponse(syncPath)

        //check for async import to finish several times
        val timeOutAt = Instant.now().plusMillis(ASYNC_TIMEOUT_IN_MS)
        var syncResponse: SyncResponse
        do {
            Thread.sleep(ASYNC_CHECK_INTERVAL_IN_MS)

            syncResponse = client.invokeGetEndpoint(syncPath)

            if (syncResponse.status == status)
                break

        } while (Instant.now().isBefore(timeOutAt))

        Assertions.assertThat(syncResponse.status).isEqualTo(status)

        return syncResponse
    }

    fun importAndGetResponse(
        partnersToImport: Collection<BusinessPartnerCdq>,
        client: WebTestClient,
        wireMockServer: WireMockExtension
    ): PageResponse<LegalEntityMatchResponse> {
        val importCollection = PagedResponseCdq(
            partnersToImport.size,
            null,
            null,
            partnersToImport.size,
            partnersToImport
        )

        wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathMatching(cdqAdapterConfigProperties.readBusinessPartnerUrl)).willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(objectMapper.writeValueAsString(importCollection))
            )
        )

        startSyncAndAwaitSuccess(client, EndpointValues.CDQ_SYNCH_PATH)

        return client.invokeGetEndpoint(EndpointValues.CATENA_LEGAL_ENTITY_PATH)
    }

    fun <T> assertRecursively(actual: T): RecursiveComparisonAssert<*> {
        return Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringAllOverriddenEquals()
    }

    fun <ERROR : ErrorCode> assertErrorResponse(errorResponse: ErrorInfo<ERROR>, codeToCheck: ERROR, keyToCheck: String) {
        Assertions.assertThat(errorResponse.entityKey).isEqualTo(keyToCheck)
        Assertions.assertThat(errorResponse.errorCode).isEqualTo(codeToCheck)
    }

    private fun createBpnPattern(typeId: Char): String {
        return "${bpnConfigProperties.id}$typeId[${bpnConfigProperties.alphabet}]{${bpnConfigProperties.counterDigits + 2}}"
    }
}