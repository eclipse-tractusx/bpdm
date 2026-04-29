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

package org.eclipse.tractusx.bpdm.test.testdata.gate.v7

import org.eclipse.tractusx.bpdm.gate.api.model.*

class BusinessPartnerScriptVariantV7Builder(seed: String) {

    private var scriptCode: String = "Script Code $seed"
    private var nameParts: List<String> = listOf("Name Part Variant 1 $seed", "Name Part Variant 2 $seed")
    private var legalEntity: LegalEntityScriptVariantDto = LegalEntityScriptVariantDto(
        legalName = "Legal Name Variant $seed",
        shortName = "Short Name Variant $seed"
    )
    private var site: SiteScriptVariantDto = SiteScriptVariantDto(
        name = "Site Name Variant $seed"
    )
    private var address: AddressScriptVariantDto = AddressScriptVariantDto(
        name = "Address Name Variant $seed",
        physicalAddress = PhysicalAddressScriptVariantDto(
            postalCode = "Postal Code Variant $seed",
            city = "City Variant $seed",
            district = "District Variant $seed",
            street = StreetDto(
                name = "Street Name Variant $seed",
                houseNumber = "House Number Variant $seed"
            ),
            companyPostalCode = "Company Postal Code Variant $seed",
            industrialZone = "Industrial Zone Variant $seed",
            building = "Building Variant $seed",
            floor = "Floor Variant $seed",
            door = "Door Variant $seed"
        ),
        alternativeAddress = AlternativeAddressScriptVariantDto(
            postalCode = "Alt Postal Code Variant $seed",
            city = "Alt City Variant $seed",
            deliveryServiceQualifier = "Delivery Service Qualifier Variant $seed",
            deliveryServiceNumber = "Delivery Service Number Variant $seed"
        )
    )

    fun withScriptCode(scriptCode: String) = apply { this.scriptCode = scriptCode }
    fun withNameParts(nameParts: List<String>) = apply { this.nameParts = nameParts }
    fun withLegalEntity(legalEntity: LegalEntityScriptVariantDto) = apply { this.legalEntity = legalEntity }
    fun withSite(site: SiteScriptVariantDto) = apply { this.site = site }
    fun withAddress(address: AddressScriptVariantDto) = apply { this.address = address }

    fun build(): BusinessPartnerScriptVariantDto = BusinessPartnerScriptVariantDto(
        scriptCode = scriptCode,
        nameParts = nameParts,
        legalEntity = legalEntity,
        site = site,
        address = address
    )
}
