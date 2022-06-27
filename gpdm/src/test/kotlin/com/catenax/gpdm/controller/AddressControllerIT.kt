package com.catenax.gpdm.controller

import com.catenax.gpdm.Application
import com.catenax.gpdm.dto.response.AddressResponse
import com.catenax.gpdm.dto.response.AddressWithReferenceResponse
import com.catenax.gpdm.dto.response.PageResponse
import com.catenax.gpdm.util.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class])
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class AddressControllerIT @Autowired constructor(
    val testHelpers: TestHelpers,
    val webTestClient: WebTestClient
) {
    companion object {
        @RegisterExtension
        val wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.cdq.host") { wireMockServer.baseUrl() }
        }
    }

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
    }

    /**
     * Given partners in db
     * When requesting an address by bpn-a
     * Then address is returned
     */
    @Test
    fun `get address by bpn-a`() {
        val partnersToImport = listOf(CdqValues.businessPartner1)
        val importedBusinessPartners = testHelpers.importAndGetResponse(partnersToImport, webTestClient, wireMockServer)

        val importedPartner = importedBusinessPartners.content.single().businessPartner
        importedPartner.bpn
            .let { bpn -> requestAddressesOfLegalEntity(bpn).content.single().bpn }
            .let { bpnAddress -> requestAddress(bpnAddress) }
            .let { addressResponse ->
                assertThat(addressResponse.bpnLegalEntity).isEqualTo(importedPartner.bpn)
            }
    }

    /**
     * Given partners in db
     * When requesting an address by non-existent bpn-a
     * Then a "not found" response is sent
     */
    @Test
    fun `get address by bpn-a, not found`() {
        val partnersToImport = listOf(CdqValues.businessPartner1)
        testHelpers.importAndGetResponse(partnersToImport, webTestClient, wireMockServer)

        webTestClient.get()
            .uri(EndpointValues.CATENA_ADDRESSES_PATH + "/NONEXISTENT_BPN")
            .exchange().expectStatus().isNotFound
    }

    private fun requestAddress(bpnAddress: String) =
        webTestClient.invokeGetEndpoint<AddressWithReferenceResponse>(EndpointValues.CATENA_ADDRESSES_PATH + "/${bpnAddress}")

    private fun requestAddressesOfLegalEntity(bpn: String) =
        webTestClient.invokeGetEndpoint<PageResponse<AddressResponse>>(EndpointValues.CATENA_BUSINESS_PARTNER_PATH + "/${bpn}" + EndpointValues.CATENA_ADDRESSES_PATH_POSTFIX)
}