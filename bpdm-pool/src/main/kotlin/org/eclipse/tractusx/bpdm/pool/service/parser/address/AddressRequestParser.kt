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

package org.eclipse.tractusx.bpdm.pool.service.parser.address

import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.entity.RegionDb
import org.eclipse.tractusx.bpdm.pool.model.AddressMetadata
import org.eclipse.tractusx.bpdm.pool.model.AddressState
import org.eclipse.tractusx.bpdm.pool.model.GeoCoordinate
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.error.AddressConstraintParseError
import org.eclipse.tractusx.bpdm.pool.model.error.AddressContentParseError
import org.eclipse.tractusx.bpdm.pool.model.error.AddressFieldParseError
import org.eclipse.tractusx.bpdm.pool.model.error.AddressMetadataParseError
import org.eclipse.tractusx.bpdm.pool.model.error.AddressScriptVariantParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.*
import org.eclipse.tractusx.bpdm.pool.model.request.*
import org.eclipse.tractusx.bpdm.pool.repository.CountryRepository
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierTypeRepository
import org.eclipse.tractusx.bpdm.pool.repository.RegionRepository
import org.eclipse.tractusx.bpdm.pool.repository.ScriptCodeRepository
import org.eclipse.tractusx.bpdm.pool.util.ValidationLimits
import org.springframework.stereotype.Service

/**
 * Validates the fields of an address against the metadata they reference — countries, regions, identifier types and
 * script codes. Its errors subtype both the address-create and the address-update error type, because create and update
 * share this parser.
 */
@Service
class AddressRequestParser(
    private val identifierTypeRepository: IdentifierTypeRepository,
    private val countryRepository: CountryRepository,
    private val regionRepository: RegionRepository,
    private val scriptCodeRepository: ScriptCodeRepository
) {

    /**
     * Validates each address content and reports either the validated address or every problem found in that entry.
     */
    fun parse(contents: List<LogisticAddressRequest>): List<ParseResult<LogisticAddressParsed, AddressContentParseError>> {
        val metadata = fetchMetadata(contents)
        return contents.map { parseEntry(it, metadata) }
    }

    private fun fetchMetadata(contents: List<LogisticAddressRequest>): AddressMetadata {
        val scriptVariants = contents.flatMap { it.scriptVariants }

        val idTypeKeys = contents.flatMap { it.identifiers }.mapNotNull { it.type }.toSet()
        // The maintained country list is read per request, so a country added to it is usable without a restart.
        val countryKeys = contents.flatMap {
            listOfNotNull(it.physicalPostalAddress.country, it.alternativePostalAddress?.country)
        }.toSet()
        val regionKeys = contents.flatMap {
            listOfNotNull(it.physicalPostalAddress.administrativeAreaLevel1, it.alternativePostalAddress?.administrativeAreaLevel1)
        }.toSet()
        val scriptCodeKeys = scriptVariants.map { it.scriptCode }.toSet()

        val idTypes = identifierTypeRepository.findByBusinessPartnerTypeAndTechnicalKeyIn(IdentifierBusinessPartnerType.ADDRESS, idTypeKeys)
        val countries = countryRepository.findByCountryCodeIn(countryKeys)
        val regions = regionRepository.findByRegionCodeIn(regionKeys)
        val scriptCodes = scriptCodeRepository.findByTechnicalKeyIn(scriptCodeKeys)

        return AddressMetadata(
            idTypes = idTypes.associateBy { it.technicalKey },
            countries = countries.associateBy { it.countryCode },
            regions = regions.associateBy { it.regionCode },
            scriptCodes = scriptCodes.associateBy { it.technicalKey }
        )
    }

    private fun parseEntry(
        request: LogisticAddressRequest,
        metadata: AddressMetadata
    ): ParseResult<LogisticAddressParsed, AddressContentParseError> {
        val errors = mutableListOf<AddressContentParseError>()

        val physical = parsePhysical(request.physicalPostalAddress, metadata, errors)
        val alternative = request.alternativePostalAddress?.let { parseAlternative(it, metadata, errors) }
        val confidence = parseConfidence(request.confidenceCriteria, errors)
        val identifiers = parseIdentifiers(request.identifiers, metadata, errors)
        val states = parseStates(request.states, errors)
        val parsedScriptVariants = parseScriptVariants(request.scriptVariants, metadata, errors)

        if (errors.isNotEmpty()) return ParseResult.Failure(errors)

        // No errors guarantees the nullable sub-results above are present.
        return ParseResult.Success(
            LogisticAddressParsed(
                name = request.name,
                states = states,
                identifiers = identifiers,
                physicalPostalAddress = physical!!,
                alternativePostalAddress = alternative,
                confidenceCriteria = confidence!!,
                scriptVariants = parsedScriptVariants
            )
        )
    }

    private fun parsePhysical(
        request: PhysicalPostalAddressRequest,
        metadata: AddressMetadata,
        errors: MutableList<AddressContentParseError>
    ): PhysicalPostalAddressParsed? {
        val country = parseCountry(request.country, metadata, errors, AddressFieldParseError.PhysicalCountryMissing) { AddressMetadataParseError.PhysicalCountryNotFound(it) }
        val city = request.city ?: run { errors.add(AddressFieldParseError.PhysicalCityMissing); null }
        val region = parseRegion(request.administrativeAreaLevel1, metadata, errors) { AddressMetadataParseError.PhysicalRegionNotFound(it) }

        if (country == null || city == null) return null

        return PhysicalPostalAddressParsed(
            geographicCoordinates = request.geographicCoordinates?.let { parseGeoCoordinate(it) },
            country = country,
            administrativeAreaLevel1 = region,
            administrativeAreaLevel2 = request.administrativeAreaLevel2,
            administrativeAreaLevel3 = request.administrativeAreaLevel3,
            postalCode = request.postalCode,
            city = city,
            district = request.district,
            street = request.street,
            companyPostalCode = request.companyPostalCode,
            industrialZone = request.industrialZone,
            building = request.building,
            floor = request.floor,
            door = request.door,
            taxJurisdictionCode = request.taxJurisdictionCode
        )
    }

    private fun parseAlternative(
        request: AlternativePostalAddressRequest,
        metadata: AddressMetadata,
        errors: MutableList<AddressContentParseError>
    ): AlternativePostalAddressParsed? {
        val country = parseCountry(request.country, metadata, errors, AddressFieldParseError.AlternativeCountryMissing) { AddressMetadataParseError.AlternativeCountryNotFound(it) }
        val city = request.city ?: run { errors.add(AddressFieldParseError.AlternativeCityMissing); null }
        val deliveryServiceType = request.deliveryServiceType
            ?: run { errors.add(AddressFieldParseError.AlternativeDeliveryServiceTypeMissing); null }
        val deliveryServiceNumber = request.deliveryServiceNumber
            ?: run { errors.add(AddressFieldParseError.AlternativeDeliveryServiceNumberMissing); null }
        val region = parseRegion(request.administrativeAreaLevel1, metadata, errors) { AddressMetadataParseError.AlternativeRegionNotFound(it) }

        if (country == null || city == null || deliveryServiceType == null || deliveryServiceNumber == null) return null

        return AlternativePostalAddressParsed(
            geographicCoordinates = request.geographicCoordinates?.let { parseGeoCoordinate(it) },
            country = country,
            administrativeAreaLevel1 = region,
            postalCode = request.postalCode,
            city = city,
            deliveryServiceType = deliveryServiceType,
            deliveryServiceQualifier = request.deliveryServiceQualifier,
            deliveryServiceNumber = deliveryServiceNumber
        )
    }

    private fun parseConfidence(
        request: ConfidenceCriteriaRequest,
        errors: MutableList<AddressContentParseError>
    ): ConfidenceCriteriaParsed? {
        val sharedByOwner = request.sharedByOwner
        val checkedByExternalDataSource = request.checkedByExternalDataSource
        val lastConfidenceCheckAt = request.lastConfidenceCheckAt
        val nextConfidenceCheckAt = request.nextConfidenceCheckAt

        if (sharedByOwner == null || checkedByExternalDataSource == null ||
            lastConfidenceCheckAt == null || nextConfidenceCheckAt == null
        ) {
            errors.add(AddressFieldParseError.ConfidenceCriteriaMissing)
            return null
        }

        return ConfidenceCriteriaParsed(
            sharedByOwner = sharedByOwner,
            checkedByExternalDataSource = checkedByExternalDataSource,
            lastConfidenceCheckAt = lastConfidenceCheckAt,
            nextConfidenceCheckAt = nextConfidenceCheckAt
        )
    }

    private fun parseIdentifiers(
        requests: List<AddressIdentifierRequest>,
        metadata: AddressMetadata,
        errors: MutableList<AddressContentParseError>
    ): List<AddressIdentifierParsed> {
        if (requests.size > ValidationLimits.IDENTIFIER_AMOUNT_LIMIT) {
            errors.add(AddressConstraintParseError.IdentifiersTooMany(requests.size))
        }
        return requests.mapIndexedNotNull { index, request ->
            val value = request.value ?: run { errors.add(AddressFieldParseError.IdentifierValueMissing(index)); null }
            val type = request.type
            val typeEntity = when {
                type == null -> { errors.add(AddressFieldParseError.IdentifierTypeMissing(index)); null }
                else -> metadata.idTypes[type] ?: run { errors.add(AddressMetadataParseError.IdentifierTypeNotFound(index, type)); null }
            }
            if (value == null || typeEntity == null) null else AddressIdentifierParsed(value, typeEntity)
        }
    }

    private fun parseStates(
        requests: List<AddressStateRequest>,
        errors: MutableList<AddressContentParseError>
    ): List<AddressState> =
        requests.mapIndexedNotNull { index, request ->
            val type = request.type ?: run { errors.add(AddressFieldParseError.StateTypeMissing(index)); return@mapIndexedNotNull null }
            AddressState(request.validFrom, request.validTo, type)
        }

    private fun parseScriptVariants(
        requests: List<AddressScriptVariant>,
        metadata: AddressMetadata,
        errors: MutableList<AddressContentParseError>
    ): List<AddressScriptVariantParsed> {
        val claimedScriptCodes = mutableSetOf<String>()
        return requests.mapIndexedNotNull { index, variant ->
            if (!claimedScriptCodes.add(variant.scriptCode)) {
                errors.add(AddressScriptVariantParseError.DuplicateScriptCode(index, variant.scriptCode))
                null
            } else {
                parseScriptVariant(index, variant, metadata, errors)
            }
        }
    }

    private fun parseScriptVariant(
        index: Int,
        variant: AddressScriptVariant,
        metadata: AddressMetadata,
        errors: MutableList<AddressContentParseError>
    ): AddressScriptVariantParsed? {
        val scriptCode = metadata.scriptCodes[variant.scriptCode]
            ?: run { errors.add(AddressMetadataParseError.ScriptCodeNotFound(index, variant.scriptCode)); null }
        val physical = parsePhysicalScriptVariant(index, variant.address.physicalAddress, errors)
        val alternativeRequest = variant.address.alternativeAddress
        val alternative = alternativeRequest?.let { parseAlternativeScriptVariant(index, it, errors) }

        if (scriptCode == null || physical == null || (alternativeRequest != null && alternative == null)) return null

        return AddressScriptVariantParsed(
            scriptCode = scriptCode,
            address = PostalAddressScriptVariantParsed(
                addressName = variant.address.addressName,
                physicalAddress = physical,
                alternativeAddress = alternative
            )
        )
    }

    private fun parsePhysicalScriptVariant(
        index: Int,
        variant: PhysicalAddressScriptVariant,
        errors: MutableList<AddressContentParseError>
    ): PhysicalAddressScriptVariantParsed? {
        val city = variant.city?.takeIf { it.isNotBlank() }
            ?: run { errors.add(AddressScriptVariantParseError.PhysicalCityMissing(index)); return null }

        return PhysicalAddressScriptVariantParsed(
            city = city,
            district = variant.district,
            street = variant.street,
            industrialZone = variant.industrialZone,
            building = variant.building,
            floor = variant.floor,
            door = variant.door
        )
    }

    private fun parseAlternativeScriptVariant(
        index: Int,
        variant: AlternativeAddressScriptVariant,
        errors: MutableList<AddressContentParseError>
    ): AlternativeAddressScriptVariantParsed? {
        val city = variant.city?.takeIf { it.isNotBlank() }
            ?: run { errors.add(AddressScriptVariantParseError.AlternativeCityMissing(index)); return null }

        return AlternativeAddressScriptVariantParsed(city)
    }

    private fun parseRegion(
        regionCode: String?,
        metadata: AddressMetadata,
        errors: MutableList<AddressContentParseError>,
        notFound: (String) -> AddressMetadataParseError
    ): RegionDb? {
        if (regionCode == null) return null
        return metadata.regions[regionCode] ?: run { errors.add(notFound(regionCode)); null }
    }

    private fun parseCountry(
        value: String?,
        metadata: AddressMetadata,
        errors: MutableList<AddressContentParseError>,
        missingError: AddressFieldParseError,
        notFound: (String) -> AddressMetadataParseError
    ): String? {
        if (value == null) {
            errors.add(missingError)
            return null
        }
        // The country is valid exactly when it is in the maintained list, like any other metadata reference.
        if (!metadata.countries.containsKey(value)) {
            errors.add(notFound(value))
            return null
        }
        return value
    }

    private fun parseGeoCoordinate(request: GeoCoordinateRequest): GeoCoordinate? {
        val longitude = request.longitude
        val latitude = request.latitude
        // A coordinate says nothing without both longitude and latitude, so a half-given one counts as absent.
        return if (longitude != null && latitude != null) GeoCoordinate(longitude, latitude, request.altitude) else null
    }
}