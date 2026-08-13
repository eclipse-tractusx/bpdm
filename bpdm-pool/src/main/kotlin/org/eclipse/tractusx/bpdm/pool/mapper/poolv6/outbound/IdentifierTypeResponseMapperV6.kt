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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound

import org.eclipse.tractusx.bpdm.pool.api.v6.model.IdentifierBusinessPartnerTypeV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.IdentifierTypeDetailDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.IdentifierTypeDtoV6
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierTypeDb
import org.springframework.stereotype.Component

/**
 * Maps stored identifier types to the v6 API identifier type DTOs.
 */
@Component
class IdentifierTypeResponseMapperV6 {

    /**
     * Returns the given identifier type as the v6 API reports it.
     */
    fun toIdentifierType(identifierType: IdentifierTypeDb): IdentifierTypeDtoV6 =
        with(identifierType) {
            IdentifierTypeDtoV6(
                technicalKey = technicalKey,
                businessPartnerType = IdentifierBusinessPartnerTypeV6.valueOf(businessPartnerType.name),
                name = name,
                abbreviation = abbreviation,
                transliteratedName = transliteratedName,
                transliteratedAbbreviation = transliteratedAbbreviation,
                details = details.map { IdentifierTypeDetailDtoV6(country = it.countryCode, mandatory = it.mandatory) }
            )
        }
}
