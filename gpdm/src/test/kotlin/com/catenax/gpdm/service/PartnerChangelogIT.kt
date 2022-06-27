package com.catenax.gpdm.service

import com.catenax.gpdm.Application
import com.catenax.gpdm.dto.ChangelogEntryDto
import com.catenax.gpdm.dto.response.ChangelogEntryResponse
import com.catenax.gpdm.dto.response.PageResponse
import com.catenax.gpdm.entity.ChangelogType
import com.catenax.gpdm.util.CdqValues
import com.catenax.gpdm.util.EndpointValues
import com.catenax.gpdm.util.PostgreSQLContextInitializer
import com.catenax.gpdm.util.TestHelpers
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
import org.springframework.test.web.reactive.server.returnResult

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class])
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class PartnerChangelogIT @Autowired constructor(
    val partnerChangelogService: PartnerChangelogService,
    val testHelpers: TestHelpers,
    val webTestClient: WebTestClient
) {

    companion object {
        @RegisterExtension
        var wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.cdq.host") { wireMockServer.baseUrl() }
        }
    }

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
    }

    /**
     * Given no partners in db
     * When importing partners and then updating partners
     * Then create and update changelog entries are created
     */
    @Test
    fun `create changelog entries`() {
        val partnersToImport = listOf(
            CdqValues.businessPartner1,
            CdqValues.businessPartner2
        )

        // import partner first
        testHelpers.importAndGetResponse(partnersToImport, webTestClient, wireMockServer)

        // prepare modified partner to import
        val modifiedPartnersToImport = listOf(
            CdqValues.businessPartner3.copy(id = CdqValues.businessPartner1.id)
        )

        // import updated partners from CDQ
        val importedBusinessPartners = testHelpers.importAndGetResponse(modifiedPartnersToImport, webTestClient, wireMockServer)

        // check changelog entries were created
        // first business partner was created and updated
        importedBusinessPartners.content
            .single { testHelpers.extractCdqId(it.businessPartner) == CdqValues.businessPartner1.id }
            .businessPartner.bpn
            .let { bpnBusinessPartner1 -> retrieveChangelog(bpnBusinessPartner1).content }
            .let { changelogEntries ->
                assertThat(changelogEntries)
                    .filteredOn { it.changelogType == ChangelogType.CREATE }
                    .hasSize(1)
                assertThat(changelogEntries)
                    .filteredOn { it.changelogType == ChangelogType.UPDATE }
                    .hasSize(1)
            }

        // second business partner was only created
        importedBusinessPartners.content
            .single { testHelpers.extractCdqId(it.businessPartner) == CdqValues.businessPartner2.id }
            .businessPartner.bpn
            .let { bpnBusinessPartner1 -> retrieveChangelog(bpnBusinessPartner1).content }
            .let { changelogEntries ->
                assertThat(changelogEntries)
                    .filteredOn { it.changelogType == ChangelogType.CREATE }
                    .hasSize(1)
                assertThat(changelogEntries)
                    .filteredOn { it.changelogType == ChangelogType.UPDATE }
                    .isEmpty()
            }
    }

    /**
     * Given some business partners imported
     * When trying to retrieve changelog entries using a nonexistent bpn
     * Then a "not found" response is sent
     */
    @Test
    fun `get changelog entries by nonexistent bpn`() {
        // import partners
        val partnersToImport = listOf(
            CdqValues.businessPartner1,
            CdqValues.businessPartner2
        )
        testHelpers.importAndGetResponse(partnersToImport, webTestClient, wireMockServer)

        val bpn = "NONEXISTENT_BPN"
        webTestClient.get()
            .uri(EndpointValues.CATENA_BUSINESS_PARTNER_PATH + "/${bpn}" + EndpointValues.CATENA_CHANGELOG_PATH_POSTFIX)
            .exchange().expectStatus().isNotFound
    }

    /**
     * Given some changelog entries in db
     * When changelog entries retrieved paginated
     * Then correct changelog page retrieved
     */
    @Test
    fun `get changelog entries starting after id via service`() {
        val startId = partnerChangelogService.createChangelogEntry(ChangelogEntryDto("testBpn1", ChangelogType.CREATE)).id
        partnerChangelogService.createChangelogEntry(ChangelogEntryDto("testBpn1", ChangelogType.UPDATE))
        partnerChangelogService.createChangelogEntry(ChangelogEntryDto("testBpn2", ChangelogType.CREATE))

        val changelogEntryPage = partnerChangelogService.getChangelogEntriesStartingAfterId(startId = startId, pageIndex = 1, pageSize = 1)
        assertThat(changelogEntryPage.totalElements).isEqualTo(2)
        assertThat(changelogEntryPage.content.size).isEqualTo(1)
        assertThat(changelogEntryPage.content[0].bpn).isEqualTo("testBpn2")
    }

    private fun retrieveChangelog(bpn: String) = webTestClient
        .get()
        .uri(EndpointValues.CATENA_BUSINESS_PARTNER_PATH + "/${bpn}" + EndpointValues.CATENA_CHANGELOG_PATH_POSTFIX)
        .exchange().expectStatus().isOk
        .returnResult<PageResponse<ChangelogEntryResponse>>()
        .responseBody
        .blockFirst()!!
}