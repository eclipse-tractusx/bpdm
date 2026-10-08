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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.pool.service.parser.address

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.entity.CountryDb
import org.eclipse.tractusx.bpdm.pool.entity.RegionDb
import org.eclipse.tractusx.bpdm.pool.model.error.AddressContentParseError
import org.eclipse.tractusx.bpdm.pool.model.error.AddressMetadataParseError
import org.eclipse.tractusx.bpdm.pool.model.request.AlternativePostalAddressRequest
import org.eclipse.tractusx.bpdm.pool.model.request.ConfidenceCriteriaRequest
import org.eclipse.tractusx.bpdm.pool.model.request.LogisticAddressRequest
import org.eclipse.tractusx.bpdm.pool.model.request.PhysicalPostalAddressRequest
import org.eclipse.tractusx.bpdm.pool.repository.CountryRepository
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierTypeRepository
import org.eclipse.tractusx.bpdm.pool.repository.RegionRepository
import org.eclipse.tractusx.bpdm.pool.repository.ScriptCodeRepository
import org.junit.jupiter.api.Test
import java.time.Instant

class AddressRequestParserRegionCountryTest {

    private val identifierTypeRepository = mockk<IdentifierTypeRepository>()
    private val countryRepository = mockk<CountryRepository>()
    private val regionRepository = mockk<RegionRepository>()
    private val scriptCodeRepository = mockk<ScriptCodeRepository>()
    private val parser = AddressRequestParser(
        identifierTypeRepository,
        countryRepository,
        regionRepository,
        scriptCodeRepository
    )

    @Test
    fun `reject physical region whose country is not maintained`() {
        val regionCode = "TEST-PHYSICAL-REGION"
        prepareMetadata(regionCode)

        val result = parser.parse(listOf(addressRequest(regionCode, null))).single()

        assertThat(result).isInstanceOf(ParseResult.Failure::class.java)
        val failure = result as ParseResult.Failure<AddressContentParseError>
        assertThat(failure.errors).contains(AddressMetadataParseError.PhysicalCountryNotFound("ZZ"))
    }

    @Test
    fun `reject alternative region whose country is not maintained`() {
        val regionCode = "TEST-ALTERNATIVE-REGION"
        prepareMetadata(regionCode)

        val result = parser.parse(listOf(addressRequest(null, regionCode))).single()

        assertThat(result).isInstanceOf(ParseResult.Failure::class.java)
        val failure = result as ParseResult.Failure<AddressContentParseError>
        assertThat(failure.errors).contains(AddressMetadataParseError.AlternativeCountryNotFound("ZZ"))
    }

    @Test
    fun `region becomes usable immediately after its country is maintained`() {
        val regionCode = "TEST-NEW-COUNTRY-REGION"
        prepareMetadata(regionCode)
        val request = addressRequest(regionCode, null)

        assertThat(parser.parse(listOf(request)).single()).isInstanceOf(ParseResult.Failure::class.java)

        every { countryRepository.findByCountryCodeIn(any()) } returns listOf(
            CountryDb("DE", "Germany", null),
            CountryDb("ZZ", "Maintained test country", null)
        )

        assertThat(parser.parse(listOf(request)).single()).isInstanceOf(ParseResult.Success::class.java)
    }

    private fun prepareMetadata(regionCode: String) {
        every {
            identifierTypeRepository.findByBusinessPartnerTypeAndTechnicalKeyIn(
                IdentifierBusinessPartnerType.ADDRESS,
                any()
            )
        } returns emptySet()
        every { countryRepository.findByCountryCodeIn(any()) } answers {
            firstArg<Set<String>>()
                .filter { it == "DE" }
                .map { CountryDb(it, "Germany", null) }
        }
        every { regionRepository.findByRegionCodeIn(any()) } returns
            setOf(RegionDb("ZZ", regionCode, "Test region"))
        every { scriptCodeRepository.findByTechnicalKeyIn(any()) } returns emptySet()
    }

    private fun addressRequest(
        physicalRegionCode: String?,
        alternativeRegionCode: String?
    ) = LogisticAddressRequest(
        name = null,
        states = emptyList(),
        identifiers = emptyList(),
        physicalPostalAddress = PhysicalPostalAddressRequest(
            geographicCoordinates = null,
            country = "DE",
            administrativeAreaLevel1 = physicalRegionCode,
            administrativeAreaLevel2 = null,
            administrativeAreaLevel3 = null,
            postalCode = null,
            city = "Test city",
            district = null,
            street = null,
            companyPostalCode = null,
            industrialZone = null,
            building = null,
            floor = null,
            door = null,
            taxJurisdictionCode = null
        ),
        alternativePostalAddress = alternativeRegionCode?.let {
            AlternativePostalAddressRequest(
                geographicCoordinates = null,
                country = "DE",
                administrativeAreaLevel1 = it,
                postalCode = null,
                city = "Test city",
                deliveryServiceType = DeliveryServiceType.PO_BOX,
                deliveryServiceQualifier = null,
                deliveryServiceNumber = "123"
            )
        },
        confidenceCriteria = ConfidenceCriteriaRequest(
            sharedByOwner = false,
            checkedByExternalDataSource = false,
            lastConfidenceCheckAt = Instant.EPOCH,
            nextConfidenceCheckAt = Instant.EPOCH
        ),
        scriptVariants = emptyList()
    )
}
