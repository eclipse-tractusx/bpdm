/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.common.model.ClassificationType
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.PostalAddressRepository
import org.eclipse.tractusx.bpdm.test.containers.PostgreSQLContextInitializer
import org.eclipse.tractusx.bpdm.test.util.AssertHelpers
import org.eclipse.tractusx.bpdm.test.util.DbTestHelpers
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
@ActiveProfiles("test-no-auth")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
internal class BusinessPartnerIT @Autowired constructor(
    val testHelpers: DbTestHelpers,
    val assertHelpers: AssertHelpers,
) {

    @Autowired
    lateinit var sharingStateRepository: SharingStateRepository

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
        val sharingState = createSharingState()
        sharingStateRepository.save(sharingState)

        val businessPartner = createBusinessPartner(sharingState)

        val savedEntity = businessPartnerRepository.save(businessPartner)
        val foundEntity = businessPartnerRepository.findById(savedEntity.id).get()

        assertNotNull(savedEntity)
        assertEquals(savedEntity.id, foundEntity.id)
        assertEquals(savedEntity.roles.toList(), foundEntity.roles.toList())
        assertEquals(savedEntity.nameParts.toList(), foundEntity.nameParts.toList())
        assertEquals(savedEntity.identifiers.toList(), foundEntity.identifiers.toList())
        assertEquals(savedEntity.classifications.toList(), foundEntity.classifications.toList())
        assertHelpers.assertRecursively(foundEntity.states.toList()).isEqualTo(savedEntity.states.toList())
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

        assertEquals(10.0, foundPhysicalPostalAddress?.geographicCoordinates?.altitude)
        assertEquals(52.0, foundPhysicalPostalAddress?.geographicCoordinates?.latitude)
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

        assertEquals(15.0, foundAlternativePostalAddress?.geographicCoordinates?.altitude)
        assertEquals(52.5, foundAlternativePostalAddress?.geographicCoordinates?.latitude)
        assertEquals("Berlin", foundAlternativePostalAddress?.city)
    }

    private fun createSharingState(): SharingStateDb {
        return SharingStateDb(
            externalId = "testExternalId",
            sharingErrorCode = null,
            orchestratorRecordId = null,
            isGoldenRecordCounted = null,
            syncedIsGoldenRecordCounted = null,
            sharingStateType = SharingStateType.Initial
        )
    }


    private fun createBusinessPartner(sharingState: SharingStateDb): BusinessPartnerDb {
        val postalAddress = createPostalAddress()

        return BusinessPartnerDb(
            sharingState = sharingState,
            nameParts = mutableListOf("testNameParts", "testNameParts2", "testNameParts3", "testNameParts4", "testNameParts5"),
            shortName = "testShortName",
            legalName = "testLegalName",
            siteName = "testSiteName",
            addressName = "testAddressName",
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
            legalEntityConfidence = null,
            addressConfidence = null,
            siteConfidence = null
        )
    }

    private fun createPostalAddress(): PostalAddressDb {
        return PostalAddressDb(
            addressType = AddressType.LegalAddress,
            physicalPostalAddress = createPhysicalPostalAddress(),
            alternativePostalAddress = null
        )
    }

    private fun createPhysicalPostalAddress() =
        PhysicalPostalAddressDb(
            geographicCoordinates = GeographicCoordinateDb(
                altitude = 10.0,
                latitude = 52.0,
                longitude = 13.0
            ),
            country = CountryCode.DE,
            administrativeAreaLevel1 = "adminlevel1",
            administrativeAreaLevel2 = "adminlevel2",
            administrativeAreaLevel3 = "adminlevel3",
            postalCode = "10115",
            city = "Berlin",
            district = "district9",
            street = StreetDb(
                name = "unknown street",
                houseNumberSupplement = "house-number-supplement",
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
        AlternativePostalAddressDb(
            geographicCoordinates = GeographicCoordinateDb(
                altitude = 15.0,
                latitude = 52.5,
                longitude = 13.5
            ),
            country = CountryCode.DE,
            administrativeAreaLevel1 = "level1",
            postalCode = "10117",
            city = "Berlin",
            deliveryServiceType = DeliveryServiceType.PO_BOX,
            deliveryServiceQualifier = "DHL Express",
            deliveryServiceNumber = "12345"
        )

    private fun createIdentifier(): IdentifierDb {

        return IdentifierDb(
            value = "1234567890",
            type = "Passport",
            issuingBody = "Government of XYZ",
            businessPartnerType = BusinessPartnerType.GENERIC
        )
    }

    private fun createState(): StateDb {
        return StateDb(
            type = BusinessStateType.ACTIVE,
            validFrom = LocalDateTime.now(),
            validTo = LocalDateTime.now().plusDays(365),
            businessPartnerTyp = BusinessPartnerType.GENERIC
        )
    }

    private fun createClassification(): ClassificationDb {

        return ClassificationDb(
            value = "A1",
            code = "OP123",
            type = ClassificationType.NACE
        )
    }

}