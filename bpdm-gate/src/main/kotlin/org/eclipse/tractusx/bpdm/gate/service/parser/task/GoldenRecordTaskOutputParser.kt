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
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerOutputContentParsed
import org.eclipse.tractusx.bpdm.gate.model.parsed.task.GoldenRecordTaskOutputError
import org.eclipse.tractusx.bpdm.gate.model.raw.task.GoldenRecordTaskOutputRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.AdditionalSiteRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.AlternativeAddressRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.BusinessPartnerValuesRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.ConfidenceCriteriaRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.IdentifierRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.PhysicalPostalAddressRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.StateRaw
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
        request: GoldenRecordTaskOutputRaw,
        roles: Collection<BusinessPartnerRole>,
        tenantBpnl: String?
    ): ParseResult<BusinessPartnerOutputContentParsed, GoldenRecordTaskOutputError> {
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
            BusinessPartnerOutputContentParsed(
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
                scriptVariants = request.scriptVariants,
                legalEntityGoldenRecordRelations = request.legalEntityGoldenRecordRelations,
                siteGoldenRecordRelations = request.siteGoldenRecordRelations,
                addressGoldenRecordRelations = request.addressGoldenRecordRelations,
                ownershipUltimate = request.ownershipUltimate,
                ultimateOwnerBpnl = request.ultimateOwnerBpnl,
                additionalSites = request.additionalSites.mapNotNull { site -> site.bpn?.let { AdditionalSiteRaw(siteBpn = it, name = site.name) } }
            )
        }
    }

    private fun parsePhysicalPostalAddress(
        address: BusinessPartnerValuesRaw.PhysicalPostalAddress?
    ): ParseResult<PhysicalPostalAddressRaw, GoldenRecordTaskOutputError> {
        if (address == null) return missing("physicalPostalAddress")

        return zipParseResults(
            required(address.country, "physicalPostalAddress.country"),
            required(address.city, "physicalPostalAddress.city")
        ) { country, city ->
            PhysicalPostalAddressRaw(
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
        address: BusinessPartnerValuesRaw.AlternativePostalAddress
    ): ParseResult<AlternativeAddressRaw, GoldenRecordTaskOutputError> =
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
            AlternativeAddressRaw(
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
        confidence: GoldenRecordTaskOutputRaw.Confidence?,
        property: String
    ): ParseResult<ConfidenceCriteriaRaw, GoldenRecordTaskOutputError> {
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
            ConfidenceCriteriaRaw(
                sharedByOwner = sharedByOwner,
                checkedByExternalDataSource = checkedByExternalDataSource,
                numberOfSharingMembers = numberOfSharingMembers,
                lastConfidenceCheckAt = lastConfidenceCheckAt,
                nextConfidenceCheckAt = nextConfidenceCheckAt,
                confidenceLevel = confidenceLevel
            )
        }
    }

    private fun toIdentifier(identifier: GoldenRecordTaskOutputRaw.Identifier): IdentifierRaw? {
        val type = identifier.type ?: return null
        val value = identifier.value ?: return null
        return IdentifierRaw(type = type, value = value, issuingBody = identifier.issuingBody, businessPartnerType = identifier.businessPartnerType)
    }

    private fun toState(state: GoldenRecordTaskOutputRaw.State): StateRaw? =
        state.type?.let { StateRaw(validFrom = state.validFrom, validTo = state.validTo, type = it, businessPartnerType = state.businessPartnerType) }

    private fun <T : Any> required(value: T?, property: String): ParseResult<T, GoldenRecordTaskOutputError> =
        value?.let { ParseResult.Success(it) } ?: missing(property)

    private fun missing(property: String): ParseResult<Nothing, GoldenRecordTaskOutputError> =
        ParseResult.ofSingleFailure(GoldenRecordTaskOutputError.ValueMissing(property))
}
