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

package org.eclipse.tractusx.bpdm.common.dto.openapidescription

object IdentifierTypeDescription {
    const val header = "An identifier type defines the name or category of an identifier, such as the German " +
            "Handelsregisternummer, VAT number, Global Location Number (GLN), etc. The identifier type " +
            "is valid for a business partner type."

    const val technicalKey = "The technical identifier (unique in combination with businessPartnerType)."
    const val businessPartnerType = "One of the types of business partners for which the identifier type is valid."
    const val name = "The name of the identifier type."
    const val abbreviation = "The local short form of the identifier type used in particular country."
    const val transliteratedName = "The transliterated (Latinized) form of the name."
    const val transliteratedAbbreviation = "The transliterated (Latinized) form of the abbreviation."
    const val format = "The expected format of the identifier values for this type, given as a regular expression"
    const val categories = "The broader categories to which the identifier type belongs to:\n" +
            "* `VAT` - value-added tax registration (so-called value-added tax identification numbers (VAT IDs or VATINs), e.g. EU VAT ID, UID MWST/TVA/IPA)\n" +
            "* `TIN` - taxpayer identification (so-called taxpayer identification numbers (TINs), e.g. SIREN, NIF)\n" +
            "* `NBR` - national business registration (e.g. HRB-Nummer, Firmenbuchnummer) for different purposes (e.g. commercial register, trade register), which are not related to tax\n" +
            "* `IBR` - international business registration (e.g. LEI, EORI) for different purposes (e.g. regulatory reporting, risk management at financial regulatory bodies)\n" +
            "* `OTH` - other identifier types (e.g. D&B D-U-N-S, GS1 GLN), which are not legally secure"

    const val details = "Validity details."

    const val headerDetail = "Information for which countries an identifier type is valid and mandatory."
    const val detailCountry = "2-digit country code for which this identifier is valid; null for universal identifiers."
    const val detailMandatory = "True if identifier is mandatory in this country."
}
