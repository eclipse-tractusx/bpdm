package com.catenax.gpdm.util

import com.catenax.gpdm.component.cdq.config.CdqIdentifierConfigProperties
import com.catenax.gpdm.component.cdq.dto.BusinessPartnerCdq
import com.catenax.gpdm.component.cdq.dto.BusinessPartnerCollectionCdq
import com.catenax.gpdm.dto.response.BusinessPartnerResponse
import com.catenax.gpdm.dto.response.BusinessPartnerSearchResponse
import com.catenax.gpdm.dto.response.PageResponse
import com.catenax.gpdm.dto.response.SyncResponse
import com.catenax.gpdm.entity.SyncStatus
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions
import org.springframework.stereotype.Component
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
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
                    WHERE table_schema='${BPDM_DB_SCHEMA_NAME}'
                    AND table_name NOT IN ('flyway_schema_history') 
                LOOP 
                    EXECUTE format('TRUNCATE TABLE ${BPDM_DB_SCHEMA_NAME}.%I CONTINUE IDENTITY CASCADE;', table_names.table_name);
                END LOOP;
            END $$;
        """.trimIndent()
        ).executeUpdate()

        em.transaction.commit()
    }

    fun startSyncAndAwaitSuccess(client: WebTestClient, syncPath: String): SyncResponse {
        client.post().uri(syncPath)
            .exchange()
            .expectStatus()
            .is2xxSuccessful

        //check for async import to finish several times
        val timeOutAt = Instant.now().plusMillis(ASYNC_TIMEOUT_IN_MS)
        var syncResponse: SyncResponse
        do{
            Thread.sleep(ASYNC_CHECK_INTERVAL_IN_MS)

            syncResponse = client.get().uri(syncPath)
                .exchange()
                .expectStatus()
                .is2xxSuccessful
                .returnResult<SyncResponse>()
                .responseBody
                .blockFirst()!!

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
        val importCollection = BusinessPartnerCollectionCdq(
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

        return client
            .get()
            .uri(EndpointValues.CATENA_BUSINESS_PARTNER_PATH)
            .exchange().expectStatus().isOk
            .returnResult<PageResponse<BusinessPartnerSearchResponse>>()
            .responseBody
            .blockFirst()!!
    }

    fun extractCdqId(it: BusinessPartnerResponse) = it.identifiers.find { id -> id.type.technicalKey == cdqIdentifierConfigProperties.typeKey }!!.value
}