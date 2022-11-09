/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.common.dto.AddressDto
import org.eclipse.tractusx.bpdm.common.dto.IdentifierDto
import org.eclipse.tractusx.bpdm.common.dto.cdq.*
import org.eclipse.tractusx.bpdm.gate.dto.BusinessPartnerCandidateDto
import org.springframework.stereotype.Service

@Service
class CdqLookupMappingService {

    fun toCdq(candidate: BusinessPartnerCandidateDto): BusinessPartnerLookupCdq {
        return with(candidate) {
            BusinessPartnerLookupCdq(
                names = names.map { ValueLookupCdq(it.value) },
                identifiers = identifiers.map { toCdq(it) },
                legalForm = null, //legalForm parameter seems to have no influence on result currently
                address = listOf(toCdq(address))
            )
        }
    }

    private fun toCdq(identifier: IdentifierDto): IdentifierLookupCdq =
        IdentifierLookupCdq(identifier.value, TechnicalKeyLookupCdq(identifier.type))

    private fun toCdq(address: AddressDto): AddressLookupCdq =
        with(address) {
            AddressLookupCdq(
                administrativeAreas = administrativeAreas.map { ValueLookupCdq(it.value) },
                country = NameLookupCdq(country.alpha2),
                localities = localities.map { ValueLookupCdq(it.value) },
                postCodes = postCodes.map { ValueLookupCdq(it.value) },
                thoroughfares = thoroughfares.map { ThoroughfareLookupCdq(it.number, it.value) }
            )
        }

}