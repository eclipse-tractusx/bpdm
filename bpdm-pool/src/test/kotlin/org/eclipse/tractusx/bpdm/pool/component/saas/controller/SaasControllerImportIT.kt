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

package org.eclipse.tractusx.bpdm.pool.component.saas.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.stubbing.Scenario
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.request.AddressPartnerBpnSearchRequest
import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.request.SiteBpnSearchRequest
import org.eclipse.tractusx.bpdm.common.dto.response.*
import org.eclipse.tractusx.bpdm.common.dto.saas.*
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolClientImpl
import org.eclipse.tractusx.bpdm.pool.component.saas.config.SaasAdapterConfigProperties
import org.eclipse.tractusx.bpdm.pool.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.pool.repository.ImportEntryRepository
import org.eclipse.tractusx.bpdm.pool.util.*
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
import java.time.Instant


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class])
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class SaasControllerImportIT @Autowired constructor(
    private val webTestClient: WebTestClient,
    bpnConfigProperties: BpnConfigProperties,
    private val objectMapper: ObjectMapper,
    private val testHelpers: TestHelpers,
    private val saasAdapterConfigProperties: SaasAdapterConfigProperties,
    private val importEntryRepository: ImportEntryRepository,
    private val poolClient: PoolClientImpl
) {
    companion object {
        @RegisterExtension
        var wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("bpdm.saas.host") { wireMockServer.baseUrl() }
        }
    }

    private val legalEntityImportId1 = "Legal Entity Import ID 1"
    private val legalEntityImportId2 = "Legal Entity Import ID 2"
    private val legalEntityImportId3 = "Legal Entity Import ID 3"

    private val siteImportId1 = "Site Import ID 1"
    private val siteImportId2 = "Site Import ID 2"
    private val siteImportId3 = "Site Import ID 3"

    private val addressImportId1 = "Address Import ID 1"
    private val addressImportId2 = "Address Import ID 2"
    private val addressImportId3 = "Address Import ID 3"

    private val importReadyLegalEntity1 = SaasValues.legalEntity1.copy(externalId = legalEntityImportId1)
    private val importReadyLegalEntity2 = SaasValues.legalEntity2.copy(externalId = legalEntityImportId2)
    private val importReadyLegalEntity3 = SaasValues.legalEntity3.copy(externalId = legalEntityImportId3)

    private val importReadySite1 = SaasValues.site1.copy(externalId = siteImportId1)
    private val importReadySite2 = SaasValues.site2.copy(externalId = siteImportId2)
    private val importReadySite3 = SaasValues.site3.copy(externalId = siteImportId3)

    private val importReadyAddress1 = SaasValues.addressPartner1.copy(externalId = addressImportId1)
    private val importReadyAddress2 = SaasValues.addressPartner2.copy(externalId = addressImportId2)
    private val importReadyAddress3 = SaasValues.addressPartner3.copy(externalId = addressImportId3)

    private val idTypeBpn = TypeKeyNameUrlSaas(saasAdapterConfigProperties.bpnKey, bpnConfigProperties.name, "")
    private val issuerBpn = TypeKeyNameUrlSaas(name = bpnConfigProperties.agencyName)
    private val statusBpn = TypeKeyNameSaas("UNKNOWN", "Unknown")

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
        testHelpers.createTestMetadata()
        wireMockServer.resetAll()
    }

    /**
     * Given new partners of type legal entity in SaaS
     * When Import from SaaS
     * Then partners imported
     */
    @Test
    fun `import new legal entity partners`() {

        val partnersToImport = listOf(
            importReadyLegalEntity1,
            importReadyLegalEntity2,
            importReadyLegalEntity3
        )

        val bpns = importPartners(partnersToImport)

        val partnersExpected = listOf(
            ResponseValues.legalEntity1,
            ResponseValues.legalEntity2,
            ResponseValues.legalEntity3
        )

        val actualPartners = getLegalEntities(bpns)

        assertLegalEntityResponseEquals(actualPartners, partnersExpected)
    }

    /**
     * Given new partners of type site in SaaS
     * When Import from SaaS
     * Then partners imported
     */
    @Test
    fun `import new site partners`() {

        //Import the given parent legal entities first
        val legalEntityParents = listOf(
            importReadyLegalEntity1,
            importReadyLegalEntity2
        )

        val bpnLs = importPartners(legalEntityParents)
        val bpnL1 = bpnLs[0]
        val bpnL2 = bpnLs[1]

        //Now import sites
        val sitesToImport = listOf(
            importReadySite1.copyWithParent(legalEntityImportId1),
            importReadySite2.copyWithParent(legalEntityImportId2),
            importReadySite3.copyWithParent(legalEntityImportId3) //not created yet
        )

        val bpnSs = importPartners(sitesToImport, legalEntityParents.plus(importReadyLegalEntity3))
        val createdParentBpn = getImportedBpns(listOf(importReadyLegalEntity3)).single()

        //Assert actual created sites with expected
        val expectedSites = listOf(
            ResponseValues.site1,
            ResponseValues.site2,
            ResponseValues.site3.copy(bpnLegalEntity = createdParentBpn)
        )

        val actualSites = getSites(bpnSs).content
        testHelpers.assertRecursively(actualSites).ignoringFieldsMatchingRegexes(".*bpn").isEqualTo(expectedSites)
    }

    /**
     * Given new partners of type address in SaaS
     * When Import from SaaS
     * Then partners imported
     */
    @Test
    fun `import new address partners`() {

        //Import the given parent legal entities first
        val legalEntityParents = listOf(
            importReadyLegalEntity1,
            importReadyLegalEntity2
        )

        val bpnLs = importPartners(legalEntityParents)
        val bpnL2 = bpnLs[1]

        //Import Parent Site
        val importedSiteParent = importReadySite1.copyWithParent(importReadyLegalEntity1.externalId!!)

        val importedSiteBpn = importPartners(listOf(importedSiteParent), listOf(importReadyLegalEntity1)).single()

        val notImportedSiteParent = importReadySite3.copyWithParent(importReadyLegalEntity3.externalId!!)

        //Import addresses
        val addressesToImport = listOf(
            importReadyAddress1.copyWithParent(importedSiteParent.externalId!!),
            importReadyAddress2.copyWithParent(importReadyLegalEntity2.externalId!!),
            importReadyAddress3.copyWithParent(notImportedSiteParent.externalId!!) //doesn't exist yet
        )

        val addressBpns = importPartners(
            addressesToImport,
            listOf(importedSiteParent, importReadyLegalEntity2, notImportedSiteParent),
            listOf(importReadyLegalEntity3)
        )

        val notImportedSiteParentBpn = getImportedBpns(listOf(notImportedSiteParent)).single()

        //Assert actual with expected
        val actual = getAddresses(addressBpns).content
        val expected = listOf(
            ResponseValues.addressPartner1,
            ResponseValues.addressPartner2,
            ResponseValues.addressPartner3
        )

        testHelpers.assertRecursively(actual).ignoringFieldsMatchingRegexes(".*bpn").isEqualTo(expected)
    }

    /**
     * Given new partners
     * When Import partners multiple times
     * Then no duplicate partners
     */
    @Test
    fun importPartnersMultipleTimes() {
        val partnersToImport = listOf(
            importReadyLegalEntity1,
            importReadyLegalEntity2,
            importReadyLegalEntity3
        )

        val partnersExpected = listOf(
            ResponseValues.legalEntity1,
            ResponseValues.legalEntity2,
            ResponseValues.legalEntity3
        )

        //Import partner first time and check whether successfully imported
        val bpnsFirst = importPartners(partnersToImport)
        val actualFirst = getLegalEntities(bpnsFirst)
        assertLegalEntityResponseEquals(actualFirst, partnersExpected)

        //Import partner second time and check for no duplicates or other changes
        val bpnsSecond = importPartners(partnersToImport)
        val actualSecond = getLegalEntities(bpnsSecond)
        testHelpers.assertRecursively(bpnsFirst).isEqualTo(bpnsSecond)
        assertLegalEntityResponseEquals(actualSecond, partnersExpected)
    }

    /**
     * Given new partners in SaaS
     * When import with pagination
     * Then partners imported
     */
    @Test
    fun importNewPartnersWithPagination() {
        val expectedPartners = listOf(
            ResponseValues.legalEntity1,
            ResponseValues.legalEntity2,
            ResponseValues.legalEntity3
        )

        val page1 = PagedResponseSaas(
            1,
            null,
            importReadyLegalEntity2.id,
            1,
            listOf(importReadyLegalEntity1)
        )

        wireMockServer.stubFor(
            get(urlPathMatching(saasAdapterConfigProperties.readBusinessPartnerUrl))
                .withQueryParam("featuresOn", equalTo("USE_NEXT_START_AFTER"))
                .withQueryParam("startAfter", absent())
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(page1))
                )
        )

        val page2 = PagedResponseSaas(
            1,
            null,
            importReadyLegalEntity3.id,
            1,
            listOf(importReadyLegalEntity2)
        )

        wireMockServer.stubFor(
            get(urlPathMatching(saasAdapterConfigProperties.readBusinessPartnerUrl))
                .withQueryParam("featuresOn", equalTo("USE_NEXT_START_AFTER"))
                .withQueryParam("startAfter", equalTo(page1.nextStartAfter))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(page2))
                )
        )

        val page3 = PagedResponseSaas(
            1,
            null,
            null,
            1,
            listOf(importReadyLegalEntity3)
        )

        wireMockServer.stubFor(
            get(urlPathMatching(saasAdapterConfigProperties.readBusinessPartnerUrl))
                .withQueryParam("featuresOn", equalTo("USE_NEXT_START_AFTER"))
                .withQueryParam("startAfter", equalTo(page2.nextStartAfter))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(page3))
                )
        )

        testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.SAAS_SYNCH_PATH)
        val bpns = getImportedBpns(listOf(importReadyLegalEntity1, importReadyLegalEntity2, importReadyLegalEntity3))
        val actual = getLegalEntities(bpns)

        assertLegalEntityResponseEquals(actual, expectedPartners)
    }

    /*
    Given paginated import of new legal entities in error state
    When resume import
    Then remaining legal entities imported
     */
    @Test
    fun `resume paginated on error`() {

        val page1 = PagedResponseSaas(
            1,
            null,
            importReadyLegalEntity2.id,
            1,
            listOf(importReadyLegalEntity1)
        )

        val page2 = PagedResponseSaas(
            1,
            null,
            importReadyLegalEntity3.id,
            1,
            listOf(importReadyLegalEntity2)
        )

        val page3 = PagedResponseSaas(
            1,
            null,
            null,
            1,
            listOf(importReadyLegalEntity3)
        )

        //Create mock response that lead to error on second page and on second attempt normal behaviour
        val scenarioName = "Import Error Scenario"
        val secondTryState = "Second Try"

        wireMockServer.stubFor(
            get(urlPathMatching(saasAdapterConfigProperties.readBusinessPartnerUrl))
                .inScenario(scenarioName)
                .whenScenarioStateIs(Scenario.STARTED)
                .withQueryParam("featuresOn", equalTo("USE_NEXT_START_AFTER"))
                .withQueryParam("startAfter", absent())
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(page1))
                )
        )

        wireMockServer.stubFor(
            get(urlPathMatching(saasAdapterConfigProperties.readBusinessPartnerUrl))
                .inScenario(scenarioName)
                .whenScenarioStateIs(Scenario.STARTED)
                .withQueryParam("featuresOn", equalTo("USE_NEXT_START_AFTER"))
                .withQueryParam("startAfter", equalTo(page1.nextStartAfter))
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo(secondTryState)
        )

        wireMockServer.stubFor(
            get(urlPathMatching(saasAdapterConfigProperties.readBusinessPartnerUrl))
                .inScenario(scenarioName)
                .whenScenarioStateIs(secondTryState)
                .withQueryParam("featuresOn", equalTo("USE_NEXT_START_AFTER"))
                .withQueryParam("startAfter", equalTo(page1.nextStartAfter))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(page2))
                )
        )

        wireMockServer.stubFor(
            get(urlPathMatching(saasAdapterConfigProperties.readBusinessPartnerUrl))
                .inScenario(scenarioName)
                .whenScenarioStateIs(secondTryState)
                .withQueryParam("featuresOn", equalTo("USE_NEXT_START_AFTER"))
                .withQueryParam("startAfter", equalTo(page2.nextStartAfter))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(page3))
                )
        )

        //First one should fail
        testHelpers.startSyncAndAwaitError(webTestClient, EndpointValues.SAAS_SYNCH_PATH)
        //Second one should go through
        testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.SAAS_SYNCH_PATH)

        //Check whether all legal entities have been created
        val expectedPartners = listOf(
            ResponseValues.legalEntity1,
            ResponseValues.legalEntity2,
            ResponseValues.legalEntity3
        )
        val bpns = getImportedBpns(listOf(importReadyLegalEntity1, importReadyLegalEntity2, importReadyLegalEntity3))
        val actual = getLegalEntities(bpns)

        assertLegalEntityResponseEquals(actual, expectedPartners)
    }

    /**
     * Given updates on legal entities which have been import before
     * When import partners
     * Then partners are updated
     */
    @Test
    fun `update modified legal entities`() {
        //First create legal entities with BPN in the system
        val partnersToImport = listOf(
            importReadyLegalEntity1,
            importReadyLegalEntity2,
            importReadyLegalEntity3
        )

        val bpns = importPartners(partnersToImport)
        val bpnL1 = bpns[0]
        val bpnL2 = bpns[1]
        val bpnL3 = bpns[2]

        //Next swap BPN and externalId (Import ID) to simulate update of each field value
        val partnersToUpdate = listOf(
            importReadyLegalEntity1.copyWithBpn(bpnL2).copy(externalId = importReadyLegalEntity2.externalId),
            importReadyLegalEntity2.copyWithBpn(bpnL3).copy(externalId = importReadyLegalEntity3.externalId),
            importReadyLegalEntity3.copy(externalId = importReadyLegalEntity1.externalId) //Only set external ID here to simulate a record which has no BPN yet
        )

        importPartners(partnersToUpdate)

        //assert actual is expected
        val expectedPartners = listOf(
            ResponseValues.legalEntity1.copy(bpn = bpnL2),
            ResponseValues.legalEntity2.copy(bpn = bpnL3),
            ResponseValues.legalEntity3.copy(bpn = bpnL1)
        )
        val actual = getLegalEntities(bpns)
        testHelpers.assertRecursively(actual).ignoringFieldsMatchingRegexes(".*currentness").isEqualTo(expectedPartners)
    }

    /**
     * Given updated partners of type site in SaaS
     * When Import from SaaS
     * Then partners updated
     */
    @Test
    fun `import updated site partners`() {
        //First create sites
        val sitesToCreate = listOf(
            importReadySite1.copyWithParent(importReadyLegalEntity1.externalId!!),
            importReadySite2.copyWithParent(importReadyLegalEntity2.externalId!!),
            importReadySite3.copyWithParent(importReadyLegalEntity3.externalId!!)
        )

        val parents = listOf(importReadyLegalEntity1, importReadyLegalEntity2, importReadyLegalEntity3)

        val bpns = importPartners(sitesToCreate, parents)
        val bpnS1 = bpns[0]
        val bpnS2 = bpns[1]
        val bpnS3 = bpns[2]

        val bpnLs = getImportedBpns(parents)
        val bpnL1 = bpnLs[0]
        val bpnL2 = bpnLs[1]
        val bpnL3 = bpnLs[2]

        //Next swap BPN and externalId (Import ID) to simulate update of each field value
        val partnersToUpdate = listOf(
            importReadySite1.copyWithBpn(bpnS2).copy(externalId = importReadySite2.externalId),
            importReadySite2.copyWithBpn(bpnS3).copy(externalId = importReadySite3.externalId),
            importReadySite3.copy(externalId = importReadySite1.externalId) //Only set external ID here to simulate a record which has no BPN yet
        )

        importPartners(partnersToUpdate)

        //assert actual is expected
        val expectedPartners = listOf(
            ResponseValues.site1.copy(bpn = bpnS2, bpnLegalEntity = bpnL2),
            ResponseValues.site2.copy(bpn = bpnS3, bpnLegalEntity = bpnL3),
            ResponseValues.site3.copy(bpn = bpnS1, bpnLegalEntity = bpnL1),
        )
        val actual = getSites(bpns).content
        testHelpers.assertRecursively(actual).isEqualTo(expectedPartners)
    }

    /**
     * Given updated partners of type address in SaaS
     * When Import from SaaS
     * Then partners imported
     */
    @Test
    fun `import updated address partners`() {
        //First create sites
        val addressesToCreate = listOf(
            importReadyAddress1.copyWithParent(importReadyLegalEntity1.externalId!!),
            importReadyAddress2.copyWithParent(importReadyLegalEntity2.externalId!!),
            importReadyAddress3.copyWithParent(importReadyLegalEntity3.externalId!!)
        )

        val parents = listOf(importReadyLegalEntity1, importReadyLegalEntity2, importReadyLegalEntity3)

        val bpns = importPartners(addressesToCreate, parents)
        val bpnA1 = bpns[0]
        val bpnA2 = bpns[1]
        val bpnA3 = bpns[2]

        val bpnLs = getImportedBpns(parents)
        val bpnL1 = bpnLs[0]
        val bpnL2 = bpnLs[1]
        val bpnL3 = bpnLs[2]

        //Next swap BPN and externalId (Import ID) to simulate update of each field value
        val partnersToUpdate = listOf(
            importReadyAddress1.copyWithBpn(bpnA2).copy(externalId = importReadyAddress2.externalId),
            importReadyAddress2.copyWithBpn(bpnA3).copy(externalId = importReadyAddress3.externalId),
            importReadyAddress3.copy(externalId = importReadyAddress1.externalId) //Only set external ID here to simulate a record which has no BPN yet
        )

        importPartners(partnersToUpdate)

        //assert actual is expected
        val expectedPartners = listOf(
            ResponseValues.addressPartner1.copy(bpn = bpnA2),
            ResponseValues.addressPartner2.copy(bpn = bpnA3),
            ResponseValues.addressPartner3.copy(bpn = bpnA1)
        )
        val actual = getAddresses(bpns).content
        testHelpers.assertRecursively(actual).isEqualTo(expectedPartners)
    }

    private fun assertLegalEntityResponseEquals(
        actualPartners: Collection<LegalEntityResponse>,
        expectedPartners: Collection<LegalEntityResponse>
    ) {
        assertThat(actualPartners)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*uuid", ".*bpn")
            .ignoringAllOverriddenEquals()
            .ignoringCollectionOrder()
            .ignoringFieldsOfTypes(Instant::class.java)
            .isEqualTo(expectedPartners)
    }

    private fun getImportedBpns(partners: List<BusinessPartnerSaas>): List<String> {
        val importEntries = importEntryRepository.findByImportIdentifierIn(partners.map { it.externalId!! })
        return partners.map { importEntries.find { entry -> entry.importIdentifier == it.externalId }?.bpn!! }
    }

    private fun getLegalEntities(bpns: Collection<String>): Collection<LegalEntityResponse> =
        poolClient.legalEntities().searchSites(bpns).body!!

    private fun getSites(bpns: Collection<String>): PageResponse<SiteResponse> =
        poolClient.sites().searchSites(SiteBpnSearchRequest(sites = bpns), PaginationRequest())

    private fun getAddresses(bpns: Collection<String>): PageResponse<LogisticAddressResponse> = poolClient.addresses().searchAddresses(AddressPartnerBpnSearchRequest(addresses = bpns), PaginationRequest())

    private fun importPartners(
        partnersToImport: List<BusinessPartnerSaas>,
        parents: Collection<BusinessPartnerSaas> = emptyList(),
        parentsOfParents: Collection<BusinessPartnerSaas> = emptyList()
    ): List<String> {
        wireMockServer.resetAll()

        val importScenario = "IMPORT"
        val queriedNewState = "QUERIED_NEW"
        val firstCallParentState = "FIRST_CALL_PARENTS"

        //Mock new SaaS business partners to import
        wireMockServer.stubFor(
            get(urlPathMatching(saasAdapterConfigProperties.readBusinessPartnerUrl))
                .inScenario(importScenario)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(partnersToImport.toPageResponse()))
                )
                .willSetStateTo(queriedNewState)
        )

        //Mock parents that should be returned first time
        wireMockServer.stubFor(
            get(urlPathMatching(saasAdapterConfigProperties.readBusinessPartnerUrl))
                .inScenario(importScenario)
                .whenScenarioStateIs(queriedNewState)
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(parents.toPageResponse()))
                )
                .willSetStateTo(firstCallParentState)
        )

        //Mock parents that should be returned second time
        wireMockServer.stubFor(
            get(urlPathMatching(saasAdapterConfigProperties.readBusinessPartnerUrl))
                .inScenario(importScenario)
                .whenScenarioStateIs(firstCallParentState)
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(parentsOfParents.toPageResponse()))
                )
        )

        //Start import
        testHelpers.startSyncAndAwaitSuccess(webTestClient, EndpointValues.SAAS_SYNCH_PATH)

        //return the BPNs of imported business partners in same order as given
        return getImportedBpns(partnersToImport)
    }

    private fun BusinessPartnerSaas.copyWithBpn(bpn: String) =
        copy(identifiers = identifiers.plus(IdentifierSaas(idTypeBpn, bpn, issuerBpn, statusBpn)))

    private fun BusinessPartnerSaas.copyWithParent(parentId: String) =
        copy(relations = listOf(SaasValues.parentRelation.copy(startNode = parentId)))

    private fun Collection<BusinessPartnerSaas>.toPageResponse() =
        PagedResponseSaas(size, null, null, size, this)

}