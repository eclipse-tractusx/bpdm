/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.bpdm.gate.service.parser.task

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.zipParseResults
import org.eclipse.tractusx.bpdm.gate.api.model.AddressScriptVariantDto
import org.eclipse.tractusx.bpdm.gate.api.model.AlternativeAddressScriptVariantDto
import org.eclipse.tractusx.bpdm.gate.api.model.BusinessPartnerScriptVariantDto
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityScriptVariantDto
import org.eclipse.tractusx.bpdm.gate.api.model.PhysicalAddressScriptVariantDto
import org.eclipse.tractusx.bpdm.gate.api.model.SiteScriptVariantDto
import org.eclipse.tractusx.bpdm.gate.api.model.StreetScriptVariantDto
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskOutputError
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskOutputRequest
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.AdditionalSite
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.AlternativeAddress
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.ConfidenceCriteria
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.Identifier
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.OutputUpsertData
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.PhysicalPostalAddress
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.State
import org.eclipse.tractusx.bpdm.gate.model.value.BusinessPartnerValues
import org.springframework.stereotype.Service

/**
 * Validates the business partner output a golden record task produced.
 */
@Service
class GoldenRecordTaskOutputParser {

    /**
     * Turns [request] into the output of a business partner with [roles] shared by the tenant [tenantBpnl], rejecting
     * it with every mandatory value it lacks and dropping identifiers, states and additional sites without their key.
     */
    fun parse(
        request: GoldenRecordTaskOutputRequest,
        roles: Collection<BusinessPartnerRole>,
        tenantBpnl: String?
    ): ParseResult<OutputUpsertData, GoldenRecordTaskOutputError> {
        val address = zipParseResults(
            required(request.addressType, "addressType"),
            required(request.addressBpn, "addressBpn"),
            parsePhysicalPostalAddress(request.physicalPostalAddress)
        ) { addressType, addressBpn, physicalPostalAddress -> Triple(addressType, addressBpn, physicalPostalAddress) }
        val confidences = zipParseResults(
            parseConfidence(request.legalEntityConfidence, "legalEntityConfidence"),
            request.siteConfidence?.let { parseConfidence(it, "siteConfidence") } ?: ParseResult.Success(null),
            parseConfidence(request.addressConfidence, "addressConfidence")
        ) { legalEntity, site, address -> Triple(legalEntity, site, address) }
        val legalEntityAndAlternative = zipParseResults(
            required(request.legalEntityBpn, "legalEntityBpn"),
            request.alternativePostalAddress?.let(::parseAlternativePostalAddress) ?: ParseResult.Success(null)
        ) { legalEntityBpn, alternativePostalAddress -> legalEntityBpn to alternativePostalAddress }

        return zipParseResults(address, confidences, legalEntityAndAlternative) { (addressType, addressBpn, physicalPostalAddress),
                                                                                (legalEntityConfidence, siteConfidence, addressConfidence),
                                                                                (legalEntityBpn, alternativePostalAddress) ->
            OutputUpsertData(
                nameParts = request.nameParts,
                identifiers = request.identifiers.mapNotNull(::toIdentifier),
                states = request.states.mapNotNull(::toState),
                roles = roles.toSortedSet(),
                isOwnCompanyData = tenantBpnl != null && tenantBpnl == request.owningCompanyBpnl,
                legalEntityBpn = legalEntityBpn,
                legalName = request.legalName,
                shortName = request.shortName,
                legalForm = request.legalForm,
                siteBpn = request.siteBpn,
                siteName = request.siteName,
                addressBpn = addressBpn,
                addressName = request.addressName,
                addressType = addressType,
                physicalPostalAddress = physicalPostalAddress,
                alternativePostalAddress = alternativePostalAddress,
                legalEntityConfidence = legalEntityConfidence,
                siteConfidence = siteConfidence,
                addressConfidence = addressConfidence,
                legalEntityUpdatedAt = request.legalEntityUpdatedAt,
                siteUpdatedAt = request.siteUpdatedAt,
                addressUpdatedAt = request.addressUpdatedAt,
                scriptVariants = request.scriptVariants.map(::toScriptVariantDto),
                legalEntityGoldenRecordRelations = request.legalEntityGoldenRecordRelations,
                siteGoldenRecordRelations = request.siteGoldenRecordRelations,
                addressGoldenRecordRelations = request.addressGoldenRecordRelations,
                ownershipUltimate = request.ownershipUltimate,
                ultimateOwnerBpnl = request.ultimateOwnerBpnl,
                additionalSites = request.additionalSites.mapNotNull { site -> site.bpn?.let { AdditionalSite(siteBpn = it, name = site.name) } }
            )
        }
    }

    private fun parsePhysicalPostalAddress(
        address: BusinessPartnerValues.PhysicalPostalAddress?
    ): ParseResult<PhysicalPostalAddress, GoldenRecordTaskOutputError> {
        if (address == null) return missing("physicalPostalAddress")

        return zipParseResults(
            required(address.country, "physicalPostalAddress.country"),
            required(address.city, "physicalPostalAddress.city")
        ) { country, city ->
            PhysicalPostalAddress(
                geographicCoordinates = address.geographicCoordinates,
                country = country,
                administrativeAreaLevel1 = address.administrativeAreaLevel1,
                administrativeAreaLevel2 = address.administrativeAreaLevel2,
                administrativeAreaLevel3 = address.administrativeAreaLevel3,
                postalCode = address.postalCode,
                city = city,
                district = address.district,
                street = address.street,
                companyPostalCode = address.companyPostalCode,
                industrialZone = address.industrialZone,
                building = address.building,
                floor = address.floor,
                door = address.door,
                taxJurisdictionCode = address.taxJurisdictionCode
            )
        }
    }

    private fun parseAlternativePostalAddress(
        address: BusinessPartnerValues.AlternativePostalAddress
    ): ParseResult<AlternativeAddress, GoldenRecordTaskOutputError> =
        zipParseResults(
            zipParseResults(
                required(address.country, "alternativePostalAddress.country"),
                required(address.city, "alternativePostalAddress.city")
            ) { country, city -> country to city },
            zipParseResults(
                required(address.deliveryServiceType, "alternativePostalAddress.deliveryServiceType"),
                required(address.deliveryServiceNumber, "alternativePostalAddress.deliveryServiceNumber")
            ) { type, number -> type to number }
        ) { (country, city), (deliveryServiceType, deliveryServiceNumber) ->
            AlternativeAddress(
                geographicCoordinates = address.geographicCoordinates,
                country = country,
                administrativeAreaLevel1 = address.administrativeAreaLevel1,
                postalCode = address.postalCode,
                city = city,
                deliveryServiceType = deliveryServiceType,
                deliveryServiceQualifier = address.deliveryServiceQualifier,
                deliveryServiceNumber = deliveryServiceNumber
            )
        }

    private fun parseConfidence(
        confidence: GoldenRecordTaskOutputRequest.Confidence?,
        property: String
    ): ParseResult<ConfidenceCriteria, GoldenRecordTaskOutputError> {
        if (confidence == null) return missing(property)

        return zipParseResults(
            zipParseResults(
                required(confidence.sharedByOwner, "$property.sharedByOwner"),
                required(confidence.checkedByExternalDataSource, "$property.checkedByExternalDataSource"),
                required(confidence.numberOfSharingMembers, "$property.numberOfSharingMembers")
            ) { sharedByOwner, checkedByExternalDataSource, numberOfSharingMembers ->
                Triple(sharedByOwner, checkedByExternalDataSource, numberOfSharingMembers)
            },
            zipParseResults(
                required(confidence.lastConfidenceCheckAt, "$property.lastConfidenceCheckAt"),
                required(confidence.nextConfidenceCheckAt, "$property.nextConfidenceCheckAt"),
                required(confidence.confidenceLevel, "$property.confidenceLevel")
            ) { lastConfidenceCheckAt, nextConfidenceCheckAt, confidenceLevel ->
                Triple(lastConfidenceCheckAt, nextConfidenceCheckAt, confidenceLevel)
            }
        ) { (sharedByOwner, checkedByExternalDataSource, numberOfSharingMembers), (lastConfidenceCheckAt, nextConfidenceCheckAt, confidenceLevel) ->
            ConfidenceCriteria(
                sharedByOwner = sharedByOwner,
                checkedByExternalDataSource = checkedByExternalDataSource,
                numberOfSharingMembers = numberOfSharingMembers,
                lastConfidenceCheckAt = lastConfidenceCheckAt,
                nextConfidenceCheckAt = nextConfidenceCheckAt,
                confidenceLevel = confidenceLevel
            )
        }
    }

    private fun toIdentifier(identifier: GoldenRecordTaskOutputRequest.Identifier): Identifier? {
        val type = identifier.type ?: return null
        val value = identifier.value ?: return null
        return Identifier(type = type, value = value, issuingBody = identifier.issuingBody, businessPartnerType = identifier.businessPartnerType)
    }

    private fun toState(state: GoldenRecordTaskOutputRequest.State): State? =
        state.type?.let { State(validFrom = state.validFrom, validTo = state.validTo, type = it, businessPartnerType = state.businessPartnerType) }

    private fun toScriptVariantDto(scriptVariant: BusinessPartnerValues.ScriptVariant): BusinessPartnerScriptVariantDto {
        val physicalAddress = scriptVariant.physicalAddress
        val street = physicalAddress.street
        return BusinessPartnerScriptVariantDto(
            scriptCode = scriptVariant.scriptCode,
            nameParts = scriptVariant.nameParts,
            legalEntity = LegalEntityScriptVariantDto(legalName = scriptVariant.legalName, shortName = scriptVariant.shortName),
            site = SiteScriptVariantDto(name = scriptVariant.siteName),
            address = AddressScriptVariantDto(
                name = scriptVariant.addressName,
                physicalAddress = PhysicalAddressScriptVariantDto(
                    city = physicalAddress.city,
                    district = physicalAddress.district,
                    street = StreetScriptVariantDto(
                        name = street.name,
                        direction = street.direction,
                        namePrefix = street.namePrefix,
                        additionalNamePrefix = street.additionalNamePrefix,
                        nameSuffix = street.nameSuffix,
                        additionalNameSuffix = street.additionalNameSuffix
                    ),
                    industrialZone = physicalAddress.industrialZone,
                    building = physicalAddress.building,
                    floor = physicalAddress.floor,
                    door = physicalAddress.door
                ),
                alternativeAddress = scriptVariant.alternativeAddressCity?.let { AlternativeAddressScriptVariantDto(city = it) }
            )
        )
    }

    private fun <T : Any> required(value: T?, property: String): ParseResult<T, GoldenRecordTaskOutputError> =
        value?.let { ParseResult.Success(it) } ?: missing(property)

    private fun missing(property: String): ParseResult<Nothing, GoldenRecordTaskOutputError> =
        ParseResult.ofSingleFailure(GoldenRecordTaskOutputError.ValueMissing(property))
}
