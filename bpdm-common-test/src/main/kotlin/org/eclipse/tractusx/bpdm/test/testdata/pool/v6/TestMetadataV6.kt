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

package org.eclipse.tractusx.bpdm.test.testdata.pool.v6

import org.eclipse.tractusx.bpdm.common.dto.TypeKeyNameVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.CountrySubdivisionV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.CountryV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.IdentifierTypeV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.LegalFormV6Dto


data class TestMetadataV6(
    val legalForms: List<LegalFormV6Dto>,
    val legalEntityIdentifierTypes: List<IdentifierTypeV6Dto>,
    val addressIdentifierTypes: List<IdentifierTypeV6Dto>,
    val adminAreas: List<CountrySubdivisionV6Dto>,
    val countries: List<CountryV6Dto> = emptyList()
)

fun List<CountryV6Dto>.toCountryVerbose(countryCode: String): TypeKeyNameVerboseDto<String> =
    TypeKeyNameVerboseDto(countryCode, find { it.countryCode == countryCode }?.name ?: countryCode)
