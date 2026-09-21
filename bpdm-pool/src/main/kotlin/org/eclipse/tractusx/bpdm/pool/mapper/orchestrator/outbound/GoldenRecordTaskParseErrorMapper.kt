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

package org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.outbound

import org.eclipse.tractusx.bpdm.pool.model.error.*
import org.springframework.stereotype.Component

/**
 * Maps the sealed parse errors of the operations a golden record task drives to the error descriptions it reports back.
 *
 * The `when`s are exhaustive so a new error won't compile until it gets a description.
 */
@Component
class GoldenRecordTaskParseErrorMapper {

    fun toUpsertDescription(error: GoldenRecordUpsertParseError): String =
        when (error) {
            is LegalEntityContentInvalid -> toLegalEntityContentDescription(error.error)
            is LegalAddressContentInvalid -> toAddressContentDescription(error.error)
            is SiteContentInvalid -> toSiteContentDescription(error.error)
            is SiteMainAddressContentInvalid -> toAddressContentDescription(error.error)
            is AdditionalAddressContentInvalid -> toAddressContentDescription(error.error)
            is AdditionalSiteContentInvalid -> toSiteContentDescription(error.error)
            is LegalAddressCoverageLost -> toLegalAddressCoverageDescription(error.error)
            is SiteMainAddressCoverageLost -> toMainAddressCoverageDescription(error.error)
            is LegalEntityNotFound -> "Legal entity ${error.bpn} not found"
            is SiteNotFound -> "Site ${error.bpn} not found"
            is SiteMainAddressNotFound -> "Address ${error.bpn} not found"
            is AdditionalAddressNotFound -> "Address ${error.bpn} not found"
            is AdditionalSiteNotFound -> "Site ${error.bpn} not found"
            is SiteNotInRequestLegalEntity -> GoldenRecordTaskErrorMessage.SITE_WRONG_LEGAL_ENTITY_REFERENCE.message
            is AdditionalAddressNotInRequestLegalEntity ->
                GoldenRecordTaskErrorMessage.ADDITIONAL_ADDRESS_WRONG_LEGAL_ENTITY_REFERENCE.message
            is MultipleUltimateOwners ->
                "An ownership hierarchy can have at most one ultimate owner, but these legal entities are also flagged " +
                        "as ultimate owner: ${error.conflictingBpnls.joinToString(", ")}"
            is AlternativeHeadquarterCannotOwn ->
                "Legal entity ${error.bpnl} cannot carry the ultimate-owner flag because it is an alternative headquarter"
            is ScriptVariantCoverageLost -> toScriptVariantCoverageDescription(error.error)
            SiteMainAddressRestatesLegalAddress ->
                "A site whose main address is the legal address must state no main address of its own"
            is SiteDoesNotSitOnLegalAddress ->
                "Site ${error.siteBpn} has its own main address ${error.mainAddressBpn}, so it cannot be updated without one: " +
                        "state that main address, or address the site that sits on the legal address"
            AdditionalAddressRestatesLegalAddress ->
                "An additional address must be a different address than the legal address"
            AdditionalAddressRestatesSiteMainAddress ->
                "An additional address must be a different address than the site main address"
            is SiteScriptCodeNotStatedByLegalEntity ->
                "A site whose main address is the legal address can only be named in scripts the legal entity is named in: " +
                        "state script code '${error.scriptCode}' on the legal entity as well, or drop it from the site"
            is AdditionalSiteOmitted ->
                "Site ${error.siteBpn} has the record's address as its main address, so it must be stated among the " +
                        "additional sites of this record"
            AdditionalSitesWithoutSite ->
                "Additional sites can only be stated for a business partner that states a site of its own"
            is AdditionalSiteNotInLegalEntity ->
                "Site ${error.siteBpn} does not belong to legal entity ${error.legalEntityBpn ?: "of this record"}"
        }

    fun toScriptVariantCoverageDescription(error: ScriptVariantCoverageParseError): String =
        when (error) {
            is ScriptVariantNotCoveredByAddress -> "Script code '${error.scriptCode}' is not covered by the address"
            is ScriptVariantCoverageStillNeeded ->
                "Script code '${error.scriptCode}' must stay covered: business partner ${error.requiredByBpn} is named in that script"
        }

    private fun toAddressContentDescription(error: AddressContentParseError): String =
        when (error) {
            is AddressFieldParseError -> toAddressFieldDescription(error)
            is AddressMetadataParseError -> toAddressMetadataDescription(error)
            is AddressConstraintParseError -> toAddressConstraintDescription(error)
            is AddressScriptVariantParseError -> toAddressScriptVariantDescription(error)
        }

    private fun toAddressFieldDescription(error: AddressFieldParseError): String =
        when (error) {
            AddressFieldParseError.PhysicalCountryMissing -> GoldenRecordTaskErrorMessage.PHYSICAL_ADDRESS_COUNTRY_MISSING.message
            AddressFieldParseError.PhysicalCityMissing -> GoldenRecordTaskErrorMessage.PHYSICAL_ADDRESS_CITY_MISSING.message
            AddressFieldParseError.AlternativeCountryMissing -> GoldenRecordTaskErrorMessage.ALTERNATIVE_ADDRESS_COUNTRY_MISSING.message
            AddressFieldParseError.AlternativeCityMissing -> GoldenRecordTaskErrorMessage.ALTERNATIVE_ADDRESS_CITY_MISSING.message
            AddressFieldParseError.AlternativeDeliveryServiceTypeMissing ->
                GoldenRecordTaskErrorMessage.ALTERNATIVE_ADDRESS_DELIVERY_SERVICE_TYPE_MISSING.message
            AddressFieldParseError.AlternativeDeliveryServiceNumberMissing ->
                GoldenRecordTaskErrorMessage.ALTERNATIVE_ADDRESS_DELIVERY_SERVICE_NUMBER_MISSING.message
            AddressFieldParseError.ConfidenceCriteriaMissing -> GoldenRecordTaskErrorMessage.ADDRESS_CONFIDENCE_CRITERIA_MISSING.message
            is AddressFieldParseError.CountryCodeNotRecognized -> "Country Code not recognized"
            is AddressFieldParseError.IdentifierValueMissing -> "Identifier value is null"
            is AddressFieldParseError.IdentifierTypeMissing -> "Identifier type is null"
            is AddressFieldParseError.StateTypeMissing -> "Business Partner state type is null"
        }

    private fun toAddressMetadataDescription(error: AddressMetadataParseError): String =
        when (error) {
            is AddressMetadataParseError.IdentifierTypeNotFound -> "Address identifier type '${error.type}' is not known"
            is AddressMetadataParseError.PhysicalRegionNotFound -> "Region '${error.regionCode}' in physical address is not known"
            is AddressMetadataParseError.AlternativeRegionNotFound -> "Region '${error.regionCode}' in alternative address is not known"
            is AddressMetadataParseError.ScriptCodeNotFound -> "Script code '${error.scriptCode}' is not known"
        }

    private fun toAddressConstraintDescription(error: AddressConstraintParseError): String =
        when (error) {
            is AddressConstraintParseError.IdentifiersTooMany -> "Too many identifiers: ${error.count} exceeds the allowed limit"
            is AddressConstraintParseError.DuplicateIdentifier -> "Duplicate identifier of type '${error.type}' with value '${error.value}'"
        }

    private fun toAddressScriptVariantDescription(error: AddressScriptVariantParseError): String =
        when (error) {
            is AddressScriptVariantParseError.PhysicalCityMissing -> "Script variant ${error.index} has no city in its physical address"
            is AddressScriptVariantParseError.AlternativeCityMissing -> "Script variant ${error.index} has no city in its alternative address"
            is AddressScriptVariantParseError.DuplicateScriptCode -> "Duplicate address script variant for script code '${error.scriptCode}'"
        }

    private fun toLegalEntityContentDescription(error: LegalEntityHeaderParseError): String =
        when (error) {
            LegalEntityHeaderParseError.NameMissing -> GoldenRecordTaskErrorMessage.LEGAL_NAME_IS_NULL.message
            LegalEntityHeaderParseError.ConfidenceCriteriaMissing -> GoldenRecordTaskErrorMessage.LEGAL_ENTITY_CONFIDENCE_CRITERIA_MISSING.message
            is LegalEntityHeaderParseError.LegalFormNotFound -> "Legal form '${error.legalForm}' is not known"
            is LegalEntityHeaderParseError.IdentifierValueMissing -> "Identifier value is null"
            is LegalEntityHeaderParseError.IdentifierTypeMissing -> "Identifier type is null"
            is LegalEntityHeaderParseError.IdentifierTypeNotFound -> "Legal entity identifier type '${error.type}' is not known"
            is LegalEntityHeaderParseError.IdentifiersTooMany -> "Too many identifiers: ${error.count} exceeds the allowed limit"
            is LegalEntityHeaderParseError.DuplicateIdentifier -> "Duplicate identifier of type '${error.type}' with value '${error.value}'"
            is LegalEntityHeaderParseError.ScriptCodeNotFound -> "Script code '${error.scriptCode}' is not known"
            is LegalEntityHeaderParseError.ScriptVariantLegalNameMissing -> "Script variant ${error.index} has no legal name"
            is LegalEntityHeaderParseError.ScriptVariantDuplicateScriptCode ->
                "Duplicate legal entity script variant for script code '${error.scriptCode}'"
        }

    private fun toSiteContentDescription(error: SiteHeaderParseError): String =
        when (error) {
            SiteHeaderParseError.NameMissing -> GoldenRecordTaskErrorMessage.SITE_NAME_MISSING.message
            SiteHeaderParseError.ConfidenceCriteriaMissing -> GoldenRecordTaskErrorMessage.SITE_CONFIDENCE_CRITERIA_MISSING.message
            is SiteHeaderParseError.ScriptCodeNotFound -> "Script code '${error.scriptCode}' is not known"
            is SiteHeaderParseError.ScriptVariantNameMissing -> "Script variant ${error.index} has no site name"
            is SiteHeaderParseError.ScriptVariantDuplicateScriptCode -> "Duplicate site script variant for script code '${error.scriptCode}'"
        }

    private fun toLegalAddressCoverageDescription(error: ScriptVariantCoverageParseError): String =
        when (error) {
            is ScriptVariantNotCoveredByAddress -> "Script code '${error.scriptCode}' is not covered by the legal address"
            is ScriptVariantCoverageStillNeeded -> toScriptVariantCoverageDescription(error)
        }

    private fun toMainAddressCoverageDescription(error: ScriptVariantCoverageParseError): String =
        when (error) {
            is ScriptVariantNotCoveredByAddress -> "Script code '${error.scriptCode}' is not covered by the site main address"
            is ScriptVariantCoverageStillNeeded -> toScriptVariantCoverageDescription(error)
        }
}
