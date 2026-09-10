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

    /**
     * Describes an error the task decides for itself, before any of its operations runs.
     */
    fun toTaskDescription(error: GoldenRecordTaskParseError): String =
        when (error) {
            SiteMainAddressMissing -> GoldenRecordTaskErrorMessage.MAINE_ADDRESS_IS_NULL.message
            is SiteNotInTaskLegalEntity -> GoldenRecordTaskErrorMessage.SITE_WRONG_LEGAL_ENTITY_REFERENCE.message
            is AdditionalAddressNotInTaskLegalEntity -> GoldenRecordTaskErrorMessage.ADDITIONAL_ADDRESS_WRONG_LEGAL_ENTITY_REFERENCE.message
        }

    /**
     * Describes an error from creating the task's additional address.
     */
    fun toAddressCreateDescription(error: AddressCreateParseError): String =
        when (error) {
            is UnresolvableLegalEntity -> "Legal entity ${error.bpn} not found"
            is UnresolvableSite -> "Site ${error.bpn} not found"
            is SiteNotInAddressLegalEntity -> "Site ${error.siteBpn} does not belong to legal entity ${error.legalEntityBpn}"
            // Unreachable on the task path: parents arrive already typed, so the untyped-stage InvalidParentBpn never occurs here.
            is InvalidParentBpn -> "Parent ${error.bpn} is not a valid BPNL/BPNS"
            is AddressContentParseError -> toAddressContentDescription(error)
        }

    /**
     * Describes an error from updating the task's additional address.
     */
    fun toAddressUpdateDescription(error: AddressUpdateParseError): String =
        when (error) {
            is ScriptVariantCoverageParseError -> toScriptVariantCoverageDescription(error)
            is UnresolvableAddress -> "Address ${error.bpn} not found"
            is SiteMainAddressOmitted -> "Site ${error.siteBpn} has this address as its main address and must be stated"
            is AddressContentParseError -> toAddressContentDescription(error)
            is UnresolvableSite -> "Site parent ${error.bpn} not found"
            is SiteNotInAddressLegalEntity -> "Site ${error.siteBpn} does not belong to legal entity ${error.legalEntityBpn}"
        }

    /**
     * Describes an error from stating which sites the task's record address belongs to.
     */
    fun toAddressSiteMembershipDescription(error: AddressSiteMembershipParseError): String =
        when (error) {
            is UnresolvableAddress -> "Address ${error.bpn} not found"
            is UnresolvableSite -> "Site ${error.bpn} not found"
            is SiteNotInAddressLegalEntity -> "Site ${error.siteBpn} does not belong to legal entity ${error.legalEntityBpn}"
            is SiteMainAddressOmitted -> "Site ${error.siteBpn} has this address as its main address and must be stated"
        }

    /**
     * Describes an error from creating the task's legal entity, naming the legal address where coverage is at stake.
     */
    fun toLegalEntityCreateDescription(error: LegalEntityCreateParseError): String =
        when (error) {
            is ScriptVariantCoverageParseError -> toLegalAddressCoverageDescription(error)
            is LegalEntityContentParseError -> toLegalEntityContentDescription(error)
            is AddressContentParseError -> toAddressContentDescription(error)
        }

    /**
     * Describes an error from updating the task's legal entity, naming the legal address where coverage is at stake.
     */
    fun toLegalEntityUpdateDescription(error: LegalEntityUpdateParseError): String =
        when (error) {
            is UnresolvableLegalEntity -> "Legal entity ${error.bpn} not found"
            is MultipleUltimateOwnersInHierarchy ->
                "An ownership hierarchy can have at most one ultimate owner, but these legal entities are also flagged " +
                        "as ultimate owner: ${error.conflictingBpnls.joinToString(", ")}"
            is AlternativeHeadquarterCannotOwnUltimately ->
                "Legal entity ${error.bpnl} cannot carry the ultimate-owner flag because it is an alternative headquarter"
            is ScriptVariantCoverageParseError -> toLegalAddressCoverageDescription(error)
            is LegalEntityContentParseError -> toLegalEntityContentDescription(error)
            is AddressContentParseError -> toAddressContentDescription(error)
        }

    /**
     * Describes an error from creating one of the task's sites, naming the site main address where coverage is at stake.
     */
    fun toSiteCreateDescription(error: SiteCreateParseError): String =
        when (error) {
            is UnresolvableLegalEntity -> "Legal entity ${error.bpn} not found"
            is UnresolvableAddress -> "Address ${error.bpn} not found"
            is LegalAddressAlreadyMainAddress -> "Legal address already is the main address of site ${error.bpnSite}"
            is ScriptVariantCoverageParseError -> toMainAddressCoverageDescription(error)
            is SiteContentParseError -> toSiteContentDescription(error)
            is AddressContentParseError -> toAddressContentDescription(error)
        }

    /**
     * Describes an error from updating the task's site, naming the site main address where coverage is at stake.
     */
    fun toSiteUpdateDescription(error: SiteUpdateParseError): String =
        when (error) {
            is UnresolvableSite -> "Site ${error.bpn} not found"
            is ScriptVariantCoverageParseError -> toMainAddressCoverageDescription(error)
            is SiteContentParseError -> toSiteContentDescription(error)
            is AddressContentParseError -> toAddressContentDescription(error)
        }

    /**
     * Describes a coverage error the task decides for the record as a whole, without naming one of its addresses.
     */
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

    private fun toLegalEntityContentDescription(error: LegalEntityContentParseError): String =
        when (error) {
            LegalEntityContentParseError.NameMissing -> GoldenRecordTaskErrorMessage.LEGAL_NAME_IS_NULL.message
            LegalEntityContentParseError.ConfidenceCriteriaMissing -> GoldenRecordTaskErrorMessage.LEGAL_ENTITY_CONFIDENCE_CRITERIA_MISSING.message
            is LegalEntityContentParseError.LegalFormNotFound -> "Legal form '${error.legalForm}' is not known"
            is LegalEntityContentParseError.IdentifierValueMissing -> "Identifier value is null"
            is LegalEntityContentParseError.IdentifierTypeMissing -> "Identifier type is null"
            is LegalEntityContentParseError.IdentifierTypeNotFound -> "Legal entity identifier type '${error.type}' is not known"
            is LegalEntityContentParseError.IdentifiersTooMany -> "Too many identifiers: ${error.count} exceeds the allowed limit"
            is LegalEntityContentParseError.DuplicateIdentifier -> "Duplicate identifier of type '${error.type}' with value '${error.value}'"
            is LegalEntityContentParseError.ScriptCodeNotFound -> "Script code '${error.scriptCode}' is not known"
            is LegalEntityContentParseError.ScriptVariantLegalNameMissing -> "Script variant ${error.index} has no legal name"
            is LegalEntityContentParseError.ScriptVariantDuplicateScriptCode ->
                "Duplicate legal entity script variant for script code '${error.scriptCode}'"
        }

    private fun toSiteContentDescription(error: SiteContentParseError): String =
        when (error) {
            SiteContentParseError.NameMissing -> GoldenRecordTaskErrorMessage.SITE_NAME_MISSING.message
            SiteContentParseError.ConfidenceCriteriaMissing -> GoldenRecordTaskErrorMessage.SITE_CONFIDENCE_CRITERIA_MISSING.message
            is SiteContentParseError.ScriptCodeNotFound -> "Script code '${error.scriptCode}' is not known"
            is SiteContentParseError.ScriptVariantNameMissing -> "Script variant ${error.index} has no site name"
            is SiteContentParseError.ScriptVariantDuplicateScriptCode -> "Duplicate site script variant for script code '${error.scriptCode}'"
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
