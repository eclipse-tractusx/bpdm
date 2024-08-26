/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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
    const val details = "Validity details."

    const val headerDetail = "Information for which countries an identifier type is valid and mandatory."
    const val detailCountry = "2-digit country code for which this identifier is valid; null for universal identifiers."
    const val detailMandatory = "True if identifier is mandatory in this country."
}
