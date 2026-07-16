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

package org.eclipse.tractusx.bpdm.pool.service.parser

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.entity.RegionDb
import org.eclipse.tractusx.bpdm.pool.model.AddressMetadata
import org.eclipse.tractusx.bpdm.pool.model.AddressState
import org.eclipse.tractusx.bpdm.pool.model.GeoCoordinate
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.error.AddressConstraintParseError
import org.eclipse.tractusx.bpdm.pool.model.error.AddressContentParseError
import org.eclipse.tractusx.bpdm.pool.model.error.AddressFieldParseError
import org.eclipse.tractusx.bpdm.pool.model.error.AddressMetadataParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressIdentifierParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressScriptVariantParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.AlternativePostalAddressParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.ConfidenceCriteriaParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.LogisticAddressParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.PhysicalPostalAddressParsed
import org.eclipse.tractusx.bpdm.pool.model.request.AddressIdentifierRequest
import org.eclipse.tractusx.bpdm.pool.model.request.AddressScriptVariant
import org.eclipse.tractusx.bpdm.pool.model.request.AddressStateRequest
import org.eclipse.tractusx.bpdm.pool.model.request.AlternativePostalAddressRequest
import org.eclipse.tractusx.bpdm.pool.model.request.ConfidenceCriteriaRequest
import org.eclipse.tractusx.bpdm.pool.model.request.GeoCoordinateRequest
import org.eclipse.tractusx.bpdm.pool.model.request.LogisticAddressRequest
import org.eclipse.tractusx.bpdm.pool.model.request.PhysicalPostalAddressRequest
import org.eclipse.tractusx.bpdm.pool.repository.IdentifierTypeRepository
import org.eclipse.tractusx.bpdm.pool.repository.RegionRepository
import org.eclipse.tractusx.bpdm.pool.repository.ScriptCodeRepository
import org.eclipse.tractusx.bpdm.pool.util.ValidationLimits
import org.springframework.stereotype.Service

/**
 * Shared by both address create and update parsing — hence its [AddressContentParseError]s subtype both operation error
 * types. Errors are accumulated (not fail-fast) so one entry's report is complete.
 */
@Service
class AddressRequestParser(
    private val identifierTypeRepository: IdentifierTypeRepository,
    private val regionRepository: RegionRepository,
    private val scriptCodeRepository: ScriptCodeRepository
) {

    fun parse(contents: List<LogisticAddressRequest>): List<ParseResult<LogisticAddressParsed, AddressContentParseError>> {
        val metadata = fetchMetadata(contents)
        return contents.map { parseEntry(it, metadata) }
    }

    private fun fetchMetadata(contents: List<LogisticAddressRequest>): AddressMetadata {
        val scriptVariants = contents.flatMap { it.scriptVariants }

        val idTypeKeys = contents.flatMap { it.identifiers }.mapNotNull { it.type }.toSet()
        val regionKeys = contents.flatMap {
            listOfNotNull(it.physicalPostalAddress.administrativeAreaLevel1, it.alternativePostalAddress?.administrativeAreaLevel1)
        }.toSet()
        val scriptCodeKeys = scriptVariants.map { it.scriptCode }.toSet()

        val idTypes = identifierTypeRepository.findByBusinessPartnerTypeAndTechnicalKeyIn(IdentifierBusinessPartnerType.ADDRESS, idTypeKeys)
        val regions = regionRepository.findByRegionCodeIn(regionKeys)
        val scriptCodes = scriptCodeRepository.findByTechnicalKeyIn(scriptCodeKeys)

        return AddressMetadata(
            idTypes = idTypes.associateBy { it.technicalKey },
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
        val parsedScriptVariants = request.scriptVariants.mapIndexedNotNull { index, variant -> parseScriptVariant(index, variant, metadata, errors) }

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
        val country = parseCountry(request.country, errors, AddressFieldParseError.PhysicalCountryMissing)
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
        val country = parseCountry(request.country, errors, AddressFieldParseError.AlternativeCountryMissing)
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

    private fun parseScriptVariant(
        index: Int,
        variant: AddressScriptVariant,
        metadata: AddressMetadata,
        errors: MutableList<AddressContentParseError>
    ): AddressScriptVariantParsed? {
        val scriptCode = metadata.scriptCodes[variant.scriptCode]
            ?: run { errors.add(AddressMetadataParseError.ScriptCodeNotFound(index, variant.scriptCode)); return null }
        return AddressScriptVariantParsed(scriptCode, variant.address)
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
        errors: MutableList<AddressContentParseError>,
        missingError: AddressFieldParseError
    ): CountryCode? {
        if (value == null) {
            errors.add(missingError)
            return null
        }
        val code = try {
            CountryCode.getByAlpha2Code(value)
        } catch (e: IllegalArgumentException) {
            null
        }
        if (code == null) errors.add(AddressFieldParseError.CountryCodeNotRecognized(value))
        return code
    }

    private fun parseGeoCoordinate(request: GeoCoordinateRequest): GeoCoordinate? {
        val longitude = request.longitude
        val latitude = request.latitude
        // A coordinate without both longitude and latitude is treated as absent (matches existing behavior).
        return if (longitude != null && latitude != null) GeoCoordinate(longitude, latitude, request.altitude) else null
    }
}