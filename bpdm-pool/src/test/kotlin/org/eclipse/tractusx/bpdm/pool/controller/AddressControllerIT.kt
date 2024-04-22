/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.controller


import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.Application
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierTypeDto
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.AddressSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.*
import org.eclipse.tractusx.bpdm.pool.util.*
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerNonVerboseValues
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerNonVerboseValues.addressIdentifier
import org.eclipse.tractusx.bpdm.test.testdata.pool.BusinessPartnerVerboseValues
import org.eclipse.tractusx.bpdm.test.testdata.pool.LegalEntityStructureRequest
import org.eclipse.tractusx.bpdm.test.testdata.pool.SiteStructureRequest
import org.eclipse.tractusx.bpdm.test.util.AssertHelpers
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
import org.eclipse.tractusx.bpdm.test.util.PoolDataHelpers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [Application::class, TestHelpers::class]
)
@ActiveProfiles("test-no-auth")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class AddressControllerIT @Autowired constructor(
    val testHelpers: TestHelpers,
    val dbTestHelpers: DbTestHelpers,
    val assertHelpers: AssertHelpers,
    val poolDataHelpers: PoolDataHelpers,
    val poolClient: PoolApiClient
) {

    @BeforeEach
    fun beforeEach() {
        dbTestHelpers.truncateDbTables()
        poolDataHelpers.createPoolMetadata()
    }

    /**
     * Given partners in db
     * When requesting an address by bpn-a
     * Then address is returned
     */
    @Test
    fun `get address by bpn-a`() {
        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate1,
                    addresses = listOf(BusinessPartnerNonVerboseValues.addressPartnerCreate2)
                )
            )
        )

        val importedPartner = createdStructures.single().legalEntity
        val addressesByBpnL = importedPartner.legalEntity.bpnl
            .let { bpnL -> requestAddressesOfLegalEntity(bpnL).content }
        // 1 legal address, 1 regular address
        assertThat(addressesByBpnL.size).isEqualTo(2)
        assertThat(addressesByBpnL.count { it.addressType == AddressType.LegalAddress || it.addressType == AddressType.LegalAndSiteMainAddress }).isEqualTo(1)

        // Same address if we use the address-by-BPNA method
        addressesByBpnL
            .forEach { address ->
                val addressByBpnA = requestAddress(address.bpna)
                assertThat(addressByBpnA.bpnLegalEntity).isEqualTo(importedPartner.legalEntity.bpnl)
                assertThat(addressByBpnA).isEqualTo(address)
            }
    }

    /**
     * Given partners in db
     * When requesting an address by non-existent bpn-a
     * Then a "not found" response is sent
     */
    @Test
    fun `get address by bpn-a, not found`() {
        testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate1,
                    addresses = listOf(BusinessPartnerNonVerboseValues.addressPartnerCreate1)
                )
            )
        )

        testHelpers.`get address by bpn-a, not found`("NONEXISTENT_BPN")

    }

    /**
     * Given multiple address partners
     * When searching addresses with BPNA
     * Then return those addresses
     */
    @Test
    fun `search addresses by BPNA`() {

        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate1,
                    addresses = listOf(BusinessPartnerNonVerboseValues.addressPartnerCreate1, BusinessPartnerNonVerboseValues.addressPartnerCreate2, BusinessPartnerNonVerboseValues.addressPartnerCreate3)
                )
            )
        )

        val bpnA1 = createdStructures[0].addresses[0].address.bpna
        val bpnA2 = createdStructures[0].addresses[1].address.bpna

        val searchRequest = AddressSearchRequest(addressBpns = listOf(bpnA1, bpnA2))
        val searchResult =
            poolClient.addresses.searchAddresses(searchRequest, PaginationRequest())

        val expected = listOf(
            BusinessPartnerVerboseValues.addressPartner1,
            BusinessPartnerVerboseValues.addressPartner2
        )

        assertAddressesAreEqual(searchResult.content, expected)
    }

    /**
     * Given multiple addresses of business partners
     * When searching addresses with BPNL
     * Then return addresses belonging to those legal entities (including legal addresses!)
     */
    @Test
    fun `search addresses by BPNL`() {
        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate1,
                    // no additional addresses
                ),
                LegalEntityStructureRequest(
                    legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate2,
                    addresses = listOf(BusinessPartnerNonVerboseValues.addressPartnerCreate3)
                )
            )
        )

        val bpnL2 = createdStructures[1].legalEntity.legalEntity.bpnl

        val searchRequest = AddressSearchRequest(legalEntityBpns = listOf(bpnL2))
        val searchResult = poolClient.addresses.searchAddresses(searchRequest, PaginationRequest())

        val expected = listOf(
            BusinessPartnerVerboseValues.addressPartner2.copy(addressType = AddressType.LegalAddress),
            BusinessPartnerVerboseValues.addressPartner3
        )

        assertAddressesAreEqual(searchResult.content, expected)
    }

    /**
     * Given multiple addresses of business partners
     * When searching addresses with BPNS
     * Then return addresses belonging to those sites (including main addresses!)
     */
    @Test
    fun `search addresses by BPNS`() {
        val createdStructures = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate1,
                    siteStructures = listOf(
                        SiteStructureRequest(
                            site = BusinessPartnerNonVerboseValues.siteCreate1,
                            addresses = listOf(BusinessPartnerNonVerboseValues.addressPartnerCreate1, BusinessPartnerNonVerboseValues.addressPartnerCreate2)
                        )
                    )
                ),
                LegalEntityStructureRequest(
                    legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate2,
                    siteStructures = listOf(
                        SiteStructureRequest(
                            site = BusinessPartnerNonVerboseValues.siteCreate2,
                            addresses = listOf(BusinessPartnerNonVerboseValues.addressPartnerCreate3)
                        )
                    )
                )
            )
        )

        val bpnS1 = createdStructures[0].siteStructures[0].site.site.bpns
        val bpnS2 = createdStructures[1].siteStructures[0].site.site.bpns

        // search for site1 -> main address and 2 regular addresses
        AddressSearchRequest(siteBpns = listOf(bpnS1))
            .let { poolClient.addresses.searchAddresses(it, PaginationRequest()) }
            .let {
                assertAddressesAreEqual(
                    it.content, listOf(
                        BusinessPartnerVerboseValues.addressPartner1.copy(addressType = AddressType.SiteMainAddress),
                        BusinessPartnerVerboseValues.addressPartner1,
                        BusinessPartnerVerboseValues.addressPartner2,
                    )
                )
            }

        // search for site2 -> main address and 1 regular address
        AddressSearchRequest(siteBpns = listOf(bpnS2))       // search for site2
            .let { poolClient.addresses.searchAddresses(it, PaginationRequest()) }
            .let {
                assertAddressesAreEqual(
                    it.content, listOf(
                        BusinessPartnerVerboseValues.addressPartner2.copy(addressType = AddressType.SiteMainAddress),
                        BusinessPartnerVerboseValues.addressPartner3,
                    )
                )
            }

        // search for site1 and site2 -> 2 main addresses and 3 regular addresses
        AddressSearchRequest(siteBpns = listOf(bpnS2, bpnS1))    // search for site1 and site2
            .let { poolClient.addresses.searchAddresses(it, PaginationRequest()) }
            .let {
                assertAddressesAreEqual(
                    it.content, listOf(
                        // site1
                        BusinessPartnerVerboseValues.addressPartner1.copy(addressType = AddressType.SiteMainAddress),
                        BusinessPartnerVerboseValues.addressPartner1,
                        BusinessPartnerVerboseValues.addressPartner2,
                        // site2
                        BusinessPartnerVerboseValues.addressPartner2.copy(addressType = AddressType.SiteMainAddress),
                        BusinessPartnerVerboseValues.addressPartner3,
                    )
                )
            }
    }

    /**
     * Given sites and legal entities
     * When creating addresses for sites and legal entities
     * Then new addresses created and returned
     */
    @Test
    fun `create new addresses`() {

        val givenStructure = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate1,
                    siteStructures = listOf(SiteStructureRequest(BusinessPartnerNonVerboseValues.siteCreate1))
                ),
            )
        )

        val bpnL = givenStructure[0].legalEntity.legalEntity.bpnl
        val bpnS = givenStructure[0].siteStructures[0].site.site.bpns

        val expected = listOf(
            BusinessPartnerVerboseValues.addressPartnerCreate1,
            BusinessPartnerVerboseValues.addressPartnerCreate2,
            BusinessPartnerVerboseValues.addressPartnerCreate3
        )

        val toCreate = listOf(
            BusinessPartnerNonVerboseValues.addressPartnerCreate1.copy(bpnParent = bpnL),
            BusinessPartnerNonVerboseValues.addressPartnerCreate2.copy(bpnParent = bpnL),
            BusinessPartnerNonVerboseValues.addressPartnerCreate3.copy(bpnParent = bpnS)
        )

        val response = poolClient.addresses.createAddresses(toCreate)

        assertCreatedAddressesAreEqual(response.entities, expected)
//        response.entities.forEach { assertThat(it.address.bpn).matches(testHelpers.bpnAPattern) }
//        assertHelpers.assertRecursively(response.entities)
//            .ignoringFields(LogisticAddressResponse::bpn.name)
//            .isEqualTo(expected)
        assertThat(response.errorCount).isEqualTo(0)
    }

    /**
     * Given no legal entities
     * When creating new legal entity with duplicate identifiers on legal entity and address
     * Then new legal entity is returned with error
     */
    @Test
    fun `create new addresses and get duplicate error`() {

        poolClient.metadata.createIdentifierType(
            IdentifierTypeDto(
                technicalKey = addressIdentifier.type,
                businessPartnerType = IdentifierBusinessPartnerType.ADDRESS, name = addressIdentifier.value
            )
        )

        val givenStructure = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate1,
                    siteStructures = listOf(SiteStructureRequest(BusinessPartnerNonVerboseValues.siteCreate1))
                ),
            )
        )

        val bpnL = givenStructure[0].legalEntity.legalEntity.bpnl


        val toCreate = BusinessPartnerNonVerboseValues.addressPartnerCreate5.copy(bpnParent = bpnL)
        val secondCreate = BusinessPartnerNonVerboseValues.addressPartnerCreate5.copy(bpnParent = bpnL, index = BusinessPartnerNonVerboseValues.addressPartnerCreate4.index)

        val response = poolClient.addresses.createAddresses(listOf(toCreate, secondCreate))


        assertThat(response.errorCount).isEqualTo(1)
        assertThat(response.entityCount).isEqualTo(0)
        val errors = response.errors.toList()
        testHelpers.assertErrorResponse(errors[0], AddressCreateError.AddressDuplicateIdentifier, toCreate.index!!)

    }

    /**
     * Given no address entities
     * When creating some address entities in one request that have duplicate identifiers (regarding type and value)
     * Then for these address entities an error is returned
     */
    @Test
    fun `update address entities and get duplicate identifier error`() {

        poolClient.metadata.createIdentifierType(
            IdentifierTypeDto(
                technicalKey = addressIdentifier.type,
                businessPartnerType = IdentifierBusinessPartnerType.ADDRESS, name = addressIdentifier.value
            )
        )

        val givenStructure = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate1,
                    siteStructures = listOf(
                        SiteStructureRequest(
                            site = BusinessPartnerNonVerboseValues.siteCreate1,
                            addresses = listOf(BusinessPartnerNonVerboseValues.addressPartnerCreate1, BusinessPartnerNonVerboseValues.addressPartnerCreate2)
                        )
                    )
                ),
                LegalEntityStructureRequest(
                    legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate2,
                    addresses = listOf(BusinessPartnerNonVerboseValues.addressPartnerCreate3)
                )
            )
        )

        val bpnA1 = givenStructure[0].siteStructures[0].addresses[0].address.bpna
        val bpnA2 = givenStructure[0].siteStructures[0].addresses[1].address.bpna
        val bpnA3 = givenStructure[1].addresses[0].address.bpna

        val expected = listOf(
            BusinessPartnerVerboseValues.addressPartner1.copy(bpna = bpnA2),
            BusinessPartnerVerboseValues.addressPartner2.copy(bpna = bpnA3),
            BusinessPartnerVerboseValues.addressPartner3.copy(bpna = bpnA1)
        )

        val toUpdate = listOf(
            BusinessPartnerNonVerboseValues.addressPartnerUpdate1.copy(bpna = bpnA2, address = BusinessPartnerNonVerboseValues.logisticAddress5),
            BusinessPartnerNonVerboseValues.addressPartnerUpdate2.copy(bpna = bpnA3, address = BusinessPartnerNonVerboseValues.logisticAddress5),
            BusinessPartnerNonVerboseValues.addressPartnerUpdate3.copy(bpna = bpnA1, address = BusinessPartnerNonVerboseValues.logisticAddress5)
        )

        val response = poolClient.addresses.updateAddresses(toUpdate)

        assertThat(response.errorCount).isEqualTo(3)
        assertThat(response.entityCount).isEqualTo(0)
        val errors = response.errors.toList()
        testHelpers.assertErrorResponse(errors[0], AddressUpdateError.AddressDuplicateIdentifier, toUpdate[0].bpna)
        testHelpers.assertErrorResponse(errors[1], AddressUpdateError.AddressDuplicateIdentifier, toUpdate[1].bpna)
        testHelpers.assertErrorResponse(errors[2], AddressUpdateError.AddressDuplicateIdentifier, toUpdate[2].bpna)
    }

    /**
     * Given sites and legal entities
     * When creating addresses with some having non-existent parents
     * Then only addresses with existing parents created and returned
     */
    @Test
    fun `don't create addresses with non-existent parent`() {
        val bpnL = poolClient.legalEntities.createBusinessPartners(listOf(BusinessPartnerNonVerboseValues.legalEntityCreate1))
            .entities.single().legalEntity.bpnl

        val expected = listOf(
            BusinessPartnerVerboseValues.addressPartnerCreate1,
        )
        val expectedErrors = listOf(
            ErrorInfo(AddressCreateError.BpnNotValid, "message ignored", BusinessPartnerNonVerboseValues.addressPartnerCreate3.index),
            ErrorInfo(AddressCreateError.SiteNotFound, "message ignored", BusinessPartnerNonVerboseValues.addressPartnerCreate1.index),
            ErrorInfo(AddressCreateError.LegalEntityNotFound, "message ignored ", BusinessPartnerNonVerboseValues.addressPartnerCreate2.index)
        )

        val invalidSiteBpn = "BPNSXXXXXXXXXX"
        val invalidLegalEntityBpn = "BPNLXXXXXXXXXX"
        val completelyInvalidBpn = "XYZ"
        val toCreate = listOf(
            BusinessPartnerNonVerboseValues.addressPartnerCreate1.copy(bpnParent = bpnL),
            BusinessPartnerNonVerboseValues.addressPartnerCreate1.copy(bpnParent = invalidSiteBpn),
            BusinessPartnerNonVerboseValues.addressPartnerCreate2.copy(bpnParent = invalidLegalEntityBpn),
            BusinessPartnerNonVerboseValues.addressPartnerCreate3.copy(bpnParent = completelyInvalidBpn),
        )

        val response = poolClient.addresses.createAddresses(toCreate)
        assertCreatedAddressesAreEqual(response.entities, expected)
//        response.entities.forEach { assertThat(it.address.bpn).matches(testHelpers.bpnAPattern) }
//        assertHelpers.assertRecursively(response.entities).ignoringFields(LogisticAddressResponse::bpn.name).isEqualTo(expected)

        assertThat(response.errorCount).isEqualTo(3)
        assertHelpers.assertRecursively(response.errors)
            .ignoringFields(ErrorInfo<AddressCreateError>::message.name)
            .isEqualTo(expectedErrors)
    }

    /**
     * Given addresses
     * When updating addresses via BPNs
     * Then update and return those addresses
     */
    @Test
    fun `update addresses`() {
        val givenStructure = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate1,
                    siteStructures = listOf(
                        SiteStructureRequest(
                            site = BusinessPartnerNonVerboseValues.siteCreate1,
                            addresses = listOf(BusinessPartnerNonVerboseValues.addressPartnerCreate1, BusinessPartnerNonVerboseValues.addressPartnerCreate2)
                        )
                    )
                ),
                LegalEntityStructureRequest(
                    legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate2,
                    addresses = listOf(BusinessPartnerNonVerboseValues.addressPartnerCreate3)
                )
            )
        )

        val bpnA1 = givenStructure[0].siteStructures[0].addresses[0].address.bpna
        val bpnA2 = givenStructure[0].siteStructures[0].addresses[1].address.bpna
        val bpnA3 = givenStructure[1].addresses[0].address.bpna

        val expected = listOf(
            BusinessPartnerVerboseValues.addressPartner1.copy(bpna = bpnA2),
            BusinessPartnerVerboseValues.addressPartner2.copy(bpna = bpnA3),
            BusinessPartnerVerboseValues.addressPartner3.copy(bpna = bpnA1)
        )

        val toUpdate = listOf(
            BusinessPartnerNonVerboseValues.addressPartnerUpdate1.copy(bpna = bpnA2),
            BusinessPartnerNonVerboseValues.addressPartnerUpdate2.copy(bpna = bpnA3),
            BusinessPartnerNonVerboseValues.addressPartnerUpdate3.copy(bpna = bpnA1)
        )

        val response = poolClient.addresses.updateAddresses(toUpdate)

        assertAddressesAreEqual(response.entities, expected)
        assertThat(response.errorCount).isEqualTo(0)
    }

    /**
     * Given addresses
     * When updating addresses with some having non-existent BPNs
     * Then only update and return addresses with existent BPNs
     */
    @Test
    fun `updates addresses, ignore non-existent`() {
        val givenStructure = testHelpers.createBusinessPartnerStructure(
            listOf(
                LegalEntityStructureRequest(
                    legalEntity = BusinessPartnerNonVerboseValues.legalEntityCreate1,
                    siteStructures = listOf(
                        SiteStructureRequest(
                            site = BusinessPartnerNonVerboseValues.siteCreate1,
                            addresses = listOf(BusinessPartnerNonVerboseValues.addressPartnerCreate1)
                        )
                    )
                )
            )
        )

        val bpnA1 = givenStructure[0].siteStructures[0].addresses[0].address.bpna

        val expected = listOf(
            BusinessPartnerVerboseValues.addressPartner2.copy(bpna = bpnA1)
        )

        val firstInvalidBpn = "BPNLXXXXXXXX"
        val secondInvalidBpn = "BPNAXXXXXXXX"
        val toUpdate = listOf(
            BusinessPartnerNonVerboseValues.addressPartnerUpdate2.copy(bpna = bpnA1),
            BusinessPartnerNonVerboseValues.addressPartnerUpdate2.copy(bpna = firstInvalidBpn),
            BusinessPartnerNonVerboseValues.addressPartnerUpdate3.copy(bpna = secondInvalidBpn)
        )

        val response = poolClient.addresses.updateAddresses(toUpdate)

        assertAddressesAreEqual(response.entities, expected)

        assertThat(response.errorCount).isEqualTo(2)
        testHelpers.assertErrorResponse(response.errors.first(), AddressUpdateError.AddressNotFound, firstInvalidBpn)
        testHelpers.assertErrorResponse(response.errors.last(), AddressUpdateError.AddressNotFound, secondInvalidBpn)
    }

    private fun assertCreatedAddressesAreEqual(actuals: Collection<AddressPartnerCreateVerboseDto>, expected: Collection<AddressPartnerCreateVerboseDto>) {
        actuals.forEach { assertThat(it.address.bpna).matches(testHelpers.bpnAPattern) }

        assertHelpers.assertRecursively(actuals)
            .ignoringFields(
                AddressPartnerCreateVerboseDto::address.name + "." + LogisticAddressVerboseDto::bpna.name,
                AddressPartnerCreateVerboseDto::address.name + "." + LogisticAddressVerboseDto::bpnLegalEntity.name,
                AddressPartnerCreateVerboseDto::address.name + "." + LogisticAddressVerboseDto::bpnSite.name
            )
            .isEqualTo(expected)
    }

    private fun assertAddressesAreEqual(actuals: Collection<LogisticAddressVerboseDto>, expected: Collection<LogisticAddressVerboseDto>) {
        actuals.forEach { assertThat(it.bpna).matches(testHelpers.bpnAPattern) }

        assertHelpers.assertRecursively(actuals)
            .ignoringFields(
                LogisticAddressVerboseDto::bpna.name,
                LogisticAddressVerboseDto::bpnLegalEntity.name,
                LogisticAddressVerboseDto::bpnSite.name
            )
            .isEqualTo(expected)
    }

    private fun requestAddress(bpnAddress: String) = poolClient.addresses.getAddress(bpnAddress)

    private fun requestAddressesOfLegalEntity(bpn: String) =
        poolClient.legalEntities.getAddresses(bpn, PaginationRequest())

}