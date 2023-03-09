/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.service

import org.eclipse.tractusx.bpdm.common.dto.GenericIdentifierDto
import org.eclipse.tractusx.bpdm.common.dto.LegalEntityIdentifierDto
import org.eclipse.tractusx.bpdm.common.dto.LogisticAddressDto
import org.eclipse.tractusx.bpdm.common.dto.saas.*
import org.eclipse.tractusx.bpdm.gate.api.model.BusinessPartnerCandidateDto
import org.springframework.stereotype.Service

@Service
class SaasLookupMappingService {

    fun toSaas(candidate: BusinessPartnerCandidateDto): BusinessPartnerLookupSaas {
        return with(candidate) {
            BusinessPartnerLookupSaas(
                names = names.map { ValueLookupSaas(it.value) },
                identifiers = identifiers.map { toSaas(it) },
                legalForm = null, //legalForm parameter seems to have no influence on result currently
                address = listOf(toSaas(address))
            )
        }
    }

    private fun toSaas(identifier: LegalEntityIdentifierDto): IdentifierLookupSaas =
        IdentifierLookupSaas(identifier.value, TechnicalKeyLookupSaas(identifier.type))

    private fun toSaas(identifier: GenericIdentifierDto): IdentifierLookupSaas =
        IdentifierLookupSaas(identifier.value, TechnicalKeyLookupSaas(identifier.type))

    private fun toSaas(address: LogisticAddressDto): AddressLookupSaas =
        with(address) {
            AddressLookupSaas(
                // TODO Mapping
                administrativeAreas = listOf(),
                country = null,
                localities = listOf(),
                postCodes = listOf(),
                thoroughfares = listOf()
            )
        }

}