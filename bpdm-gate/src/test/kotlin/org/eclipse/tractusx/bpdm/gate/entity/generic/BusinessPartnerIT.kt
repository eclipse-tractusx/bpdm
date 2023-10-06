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

package org.eclipse.tractusx.bpdm.gate.entity.generic

import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.ClassificationType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.entity.AlternativePostalAddress
import org.eclipse.tractusx.bpdm.gate.entity.GeographicCoordinate
import org.eclipse.tractusx.bpdm.gate.entity.PhysicalPostalAddress
import org.eclipse.tractusx.bpdm.gate.entity.Street
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.PostalAddressRepository
import org.eclipse.tractusx.bpdm.gate.util.DbTestHelpers
import org.eclipse.tractusx.bpdm.gate.util.PostgreSQLContextInitializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
internal class BusinessPartnerIT @Autowired constructor(
    val testHelpers: DbTestHelpers,
) {

    @Autowired
    lateinit var businessPartnerRepository: BusinessPartnerRepository

    @Autowired
    lateinit var postalAddressRepository: PostalAddressRepository


    companion object {
        @RegisterExtension
        private val wireMockServer: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()
    }

    @BeforeEach
    fun beforeEach() {
        wireMockServer.resetAll()
        testHelpers.truncateDbTables()
    }

    @Test
    fun `test save BusinessPartner`() {
        val businessPartner = createBusinessPartner()

        val savedEntity = businessPartnerRepository.save(businessPartner)
        val foundEntity = businessPartnerRepository.findById(savedEntity.id).get()

        assertNotNull(savedEntity)
        assertEquals(savedEntity.id, foundEntity.id)
        assertEquals(savedEntity.roles.toList(), foundEntity.roles.toList())
        assertEquals(savedEntity.nameParts.toList(), foundEntity.nameParts.toList())
        assertEquals(savedEntity.identifiers.toList(), foundEntity.identifiers.toList())
        assertEquals(savedEntity.classifications.toList(), foundEntity.classifications.toList())
        testHelpers.assertRecursively(foundEntity.states.toList()).isEqualTo(savedEntity.states.toList())
    }


    @Test
    fun `test save PostalAddress`() {

        val address = createPostalAddress()

        val savedAddress = postalAddressRepository.save(address)

        val foundAddress = postalAddressRepository.findById(savedAddress.id).get()
        val foundPhysicalPostalAddress = foundAddress.physicalPostalAddress

        assertNotNull(foundAddress)
        assertEquals(savedAddress.id, foundAddress.id)
        assertEquals(AddressType.LegalAddress, foundAddress.addressType)

        assertEquals(10.0f, foundPhysicalPostalAddress?.geographicCoordinates?.altitude)
        assertEquals(52.0f, foundPhysicalPostalAddress?.geographicCoordinates?.latitude)
        assertEquals("Berlin", foundPhysicalPostalAddress?.city)
    }

    @Test
    fun `test save PostalAddress with alternative address`() {

        val address = createPostalAddress()
        address.alternativePostalAddress = createAlternativePostalAddress()

        val savedAddress = postalAddressRepository.save(address)

        val foundAddress = postalAddressRepository.findById(savedAddress.id).get()
        val foundAlternativePostalAddress = foundAddress.alternativePostalAddress

        assertNotNull(foundAddress)
        assertEquals(savedAddress.id, foundAddress.id)
        assertEquals(AddressType.LegalAddress, foundAddress.addressType)

        assertEquals(15.0f, foundAlternativePostalAddress?.geographicCoordinates?.altitude)
        assertEquals(52.5f, foundAlternativePostalAddress?.geographicCoordinates?.latitude)
        assertEquals("Berlin", foundAlternativePostalAddress?.city)
    }


    private fun createBusinessPartner(): BusinessPartner {
        val postalAddress = createPostalAddress()

        return BusinessPartner(
            externalId = "testExternalId",
            nameParts = mutableListOf("testNameParts", "testNameParts2", "testNameParts3", "testNameParts4", "testNameParts5"),
            shortName = "testShortName",
            legalForm = "testLegalForm",
            isOwnCompanyData = true,
            bpnA = "testAddressBpn",
            bpnL = "testLegalEntityBpn",
            bpnS = "testSiteBpn",
            postalAddress = postalAddress,
            roles = sortedSetOf(BusinessPartnerRole.CUSTOMER, BusinessPartnerRole.SUPPLIER),
            identifiers = sortedSetOf(createIdentifier()),
            states = sortedSetOf(createState()),
            classifications = sortedSetOf(createClassification()),
            stage = StageType.Input,
            parentId = null,
            parentType = null
        )
    }

    private fun createPostalAddress(): PostalAddress {
        return PostalAddress(
            addressType = AddressType.LegalAddress,
            physicalPostalAddress = createPhysicalPostalAddress(),
            alternativePostalAddress = null
        )
    }

    private fun createPhysicalPostalAddress() =
        PhysicalPostalAddress(
            geographicCoordinates = GeographicCoordinate(
                altitude = 10.0f,
                latitude = 52.0f,
                longitude = 13.0f
            ),
            country = CountryCode.DE,
            administrativeAreaLevel1 = "adminlevel1",
            administrativeAreaLevel2 = "adminlevel2",
            administrativeAreaLevel3 = "adminlevel3",
            postalCode = "10115",
            city = "Berlin",
            district = "district9",
            street = Street(
                name = "unknown street",
                namePrefix = "Un",
                nameSuffix = "know",
                additionalNamePrefix = "empty"
            ),
            companyPostalCode = "newCode",
            industrialZone = "oldCode",
            building = "3",
            floor = "6",
            door = "42"
        )

    private fun createAlternativePostalAddress() =
        AlternativePostalAddress(
            geographicCoordinates = GeographicCoordinate(
                altitude = 15.0f,
                latitude = 52.5f,
                longitude = 13.5f
            ),
            country = CountryCode.DE,
            administrativeAreaLevel1 = "level1",
            postalCode = "10117",
            city = "Berlin",
            deliveryServiceType = DeliveryServiceType.PO_BOX,
            deliveryServiceQualifier = "DHL Express",
            deliveryServiceNumber = "12345"
        )

    private fun createIdentifier(): Identifier {

        return Identifier(
            value = "1234567890",
            type = "Passport",
            issuingBody = "Government of XYZ"
        )
    }

    private fun createState(): State {
        return State(
            description = "Active",
            type = BusinessStateType.ACTIVE,
            validFrom = LocalDateTime.now(),
            validTo = LocalDateTime.now().plusDays(365)
        )
    }
    private fun createClassification(): Classification {

        return Classification(
            value = "A1",
            code = "OP123",
            type = ClassificationType.NACE
        )
    }

}