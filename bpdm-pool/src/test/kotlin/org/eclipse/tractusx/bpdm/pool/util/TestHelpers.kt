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

package org.eclipse.tractusx.bpdm.pool.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.cdq.BusinessPartnerCdq
import org.eclipse.tractusx.bpdm.common.dto.cdq.PagedResponseCdq
import org.eclipse.tractusx.bpdm.pool.component.cdq.config.CdqIdentifierConfigProperties
import org.eclipse.tractusx.bpdm.pool.dto.response.BusinessPartnerResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.BusinessPartnerSearchResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.SyncResponse
import org.eclipse.tractusx.bpdm.pool.entity.SyncStatus
import org.springframework.stereotype.Component
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Instant
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory

private const val ASYNC_TIMEOUT_IN_MS: Long = 5 * 1000 //5 seconds
private const val ASYNC_CHECK_INTERVAL_IN_MS: Long = 200
private const val BPDM_DB_SCHEMA_NAME: String = "bpdm"

@Component
class TestHelpers(
    entityManagerFactory: EntityManagerFactory,
    val objectMapper: ObjectMapper,
    val cdqIdentifierConfigProperties: CdqIdentifierConfigProperties
) {

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


    fun startSyncAndAwaitSuccess(client: WebTestClient, syncPath: String): SyncResponse {
        client.invokePostEndpointWithoutResponse(syncPath)

        //check for async import to finish several times
        val timeOutAt = Instant.now().plusMillis(ASYNC_TIMEOUT_IN_MS)
        var syncResponse: SyncResponse
        do{
            Thread.sleep(ASYNC_CHECK_INTERVAL_IN_MS)

            syncResponse = client.invokeGetEndpoint(syncPath)

            if (syncResponse.status == SyncStatus.SUCCESS)
                break

        } while (Instant.now().isBefore(timeOutAt))

        Assertions.assertThat(syncResponse.status).isEqualTo(SyncStatus.SUCCESS)

        return syncResponse
    }

    fun importAndGetResponse(
        partnersToImport: Collection<BusinessPartnerCdq>,
        client: WebTestClient,
        wireMockServer: WireMockExtension
    ): PageResponse<BusinessPartnerSearchResponse> {
        val importCollection = PagedResponseCdq(
            partnersToImport.size,
            null,
            null,
            partnersToImport.size,
            partnersToImport
        )

        wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathMatching(EndpointValues.CDQ_MOCK_BUSINESS_PARTNER_PATH)).willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(objectMapper.writeValueAsString(importCollection))
            )
        )

        startSyncAndAwaitSuccess(client, EndpointValues.CDQ_SYNCH_PATH)

        return client.invokeGetEndpoint(EndpointValues.CATENA_BUSINESS_PARTNER_PATH)
    }

    fun extractCdqId(it: BusinessPartnerResponse) = it.identifiers.find { id -> id.type.technicalKey == cdqIdentifierConfigProperties.typeKey }!!.value
}