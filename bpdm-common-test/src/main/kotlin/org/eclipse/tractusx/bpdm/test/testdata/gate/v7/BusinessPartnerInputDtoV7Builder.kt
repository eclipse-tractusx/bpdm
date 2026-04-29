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

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.gate.api.model.BusinessPartnerIdentifierDto
import org.eclipse.tractusx.bpdm.gate.api.model.BusinessPartnerScriptVariantDto
import org.eclipse.tractusx.bpdm.gate.api.model.BusinessPartnerStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressRepresentationInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityRepresentationInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteRepresentationInputDto
import java.time.Instant
import java.time.LocalDateTime

class BusinessPartnerInputDtoV7Builder(seed: String) {

    private var externalId: String = seed
    private var nameParts: List<String> = listOf("Name Part 1 $seed", "Name Part 2 $seed")
    private var identifiers: Collection<BusinessPartnerIdentifierDto> = listOf(
        BusinessPartnerIdentifierDto(
            type = "Identifier Type $seed",
            value = "Identifier Value $seed",
            issuingBody = "Issuing Body $seed"
        )
    )
    private var states: Collection<BusinessPartnerStateDto> = listOf(
        BusinessPartnerStateDto(
            validFrom = LocalDateTime.of(2020, 1, 1, 0, 0),
            validTo = LocalDateTime.of(2025, 1, 1, 0, 0),
            type = BusinessStateType.ACTIVE
        )
    )
    private var roles: Collection<BusinessPartnerRole> = BusinessPartnerRole.entries
    private var isOwnCompanyData: Boolean = true
    private var legalEntity: LegalEntityRepresentationInputDto = LegalEntityRepresentationInputV7Builder(seed).build()
    private var site: SiteRepresentationInputDto = SiteRepresentationInputV7Builder(seed).build()
    private var address: AddressRepresentationInputDto = AddressRepresentationInputV7Builder(seed).build()
    private var externalSequenceTimestamp: Instant? = Instant.parse("2024-01-01T00:00:00Z")
    private var scriptVariants: List<BusinessPartnerScriptVariantDto> = listOf(BusinessPartnerScriptVariantV7Builder(seed).build())
    private var createdAt: Instant = Instant.parse("2024-01-01T00:00:00Z")
    private var updatedAt: Instant = Instant.parse("2024-01-01T00:00:00Z")

    fun withExternalId(externalId: String) = apply { this.externalId = externalId }
    fun withNameParts(nameParts: List<String>) = apply { this.nameParts = nameParts }
    fun withIdentifiers(identifiers: Collection<BusinessPartnerIdentifierDto>) = apply { this.identifiers = identifiers }
    fun withStates(states: Collection<BusinessPartnerStateDto>) = apply { this.states = states }
    fun withRoles(roles: Collection<BusinessPartnerRole>) = apply { this.roles = roles }
    fun withIsOwnCompanyData(isOwnCompanyData: Boolean) = apply { this.isOwnCompanyData = isOwnCompanyData }
    fun withLegalEntity(legalEntity: LegalEntityRepresentationInputDto) = apply { this.legalEntity = legalEntity }
    fun withSite(site: SiteRepresentationInputDto) = apply { this.site = site }
    fun withAddress(address: AddressRepresentationInputDto) = apply { this.address = address }
    fun withExternalSequenceTimestamp(externalSequenceTimestamp: Instant?) = apply { this.externalSequenceTimestamp = externalSequenceTimestamp }
    fun withScriptVariants(scriptVariants: List<BusinessPartnerScriptVariantDto>) = apply { this.scriptVariants = scriptVariants }
    fun withCreatedAt(createdAt: Instant) = apply { this.createdAt = createdAt }
    fun withUpdatedAt(updatedAt: Instant) = apply { this.updatedAt = updatedAt }

    fun fromRequest(request: BusinessPartnerInputRequest) = apply {
        externalId = request.externalId
        nameParts = request.nameParts
        identifiers = request.identifiers
        states = request.states
        roles = request.roles
        isOwnCompanyData = request.isOwnCompanyData
        legalEntity = request.legalEntity
        site = request.site
        address = request.address
        externalSequenceTimestamp = request.externalSequenceTimestamp
        scriptVariants = request.scriptVariants
    }

    fun build(): BusinessPartnerInputDto = BusinessPartnerInputDto(
        externalId = externalId,
        nameParts = nameParts,
        identifiers = identifiers,
        states = states,
        roles = roles,
        isOwnCompanyData = isOwnCompanyData,
        legalEntity = legalEntity,
        site = site,
        address = address,
        externalSequenceTimestamp = externalSequenceTimestamp,
        scriptVariants = scriptVariants,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
