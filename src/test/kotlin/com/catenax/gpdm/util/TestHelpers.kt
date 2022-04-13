package com.catenax.gpdm.util

import com.catenax.gpdm.dto.response.SyncResponse
import com.catenax.gpdm.entity.SyncStatus
import org.assertj.core.api.Assertions
import org.springframework.stereotype.Component
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory


@Component
class TestHelpers(
    entityManagerFactory: EntityManagerFactory
) {

    val em: EntityManager = entityManagerFactory.createEntityManager()

    fun truncateH2() {
        em.transaction.begin()

        em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate()

        em.createNativeQuery("truncate table localities").executeUpdate()
        em.createNativeQuery("truncate table postal_delivery_points").executeUpdate()
        em.createNativeQuery("truncate table premises").executeUpdate()
        em.createNativeQuery("truncate table thoroughfares").executeUpdate()
        em.createNativeQuery("truncate table post_codes").executeUpdate()
        em.createNativeQuery("truncate table administrative_areas").executeUpdate()
        em.createNativeQuery("truncate table address_contexts").executeUpdate()
        em.createNativeQuery("truncate table address_versions").executeUpdate()
        em.createNativeQuery("truncate table care_ofs").executeUpdate()
        em.createNativeQuery("truncate table addresses").executeUpdate()
        em.createNativeQuery("truncate table relations").executeUpdate()
        em.createNativeQuery("truncate table bank_account_trust_scores").executeUpdate()
        em.createNativeQuery("truncate table bank_accounts").executeUpdate()
        em.createNativeQuery("truncate table classifications").executeUpdate()
        em.createNativeQuery("truncate table business_stati").executeUpdate()
        em.createNativeQuery("truncate table names").executeUpdate()
        em.createNativeQuery("truncate table identifier_status").executeUpdate()
        em.createNativeQuery("truncate table identifier_types").executeUpdate()
        em.createNativeQuery("truncate table issuing_bodies").executeUpdate()
        em.createNativeQuery("truncate table identifiers").executeUpdate()
        em.createNativeQuery("truncate table legal_forms_legal_categories").executeUpdate()
        em.createNativeQuery("truncate table legal_form_categories").executeUpdate()
        em.createNativeQuery("truncate table legal_forms").executeUpdate()
        em.createNativeQuery("truncate table business_partner_types").executeUpdate()
        em.createNativeQuery("truncate table business_partners_roles").executeUpdate()
        em.createNativeQuery("truncate table business_partners").executeUpdate()
        em.createNativeQuery("truncate table configuration_entries").executeUpdate()

        em.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate()

        em.transaction.commit()
    }

    fun startSyncAndAwaitSuccess(client: WebTestClient, syncPath: String): SyncResponse {
        client.post().uri(syncPath)
            .exchange()
            .expectStatus()
            .is2xxSuccessful

        //Wait for the async import to finish
        Thread.sleep(1000)

        val syncResponse = client.get().uri(syncPath)
            .exchange()
            .expectStatus()
            .is2xxSuccessful
            .returnResult<SyncResponse>()
            .responseBody
            .blockFirst()!!

        Assertions.assertThat(syncResponse.status).isEqualTo(SyncStatus.SUCCESS)

        return syncResponse
    }
}