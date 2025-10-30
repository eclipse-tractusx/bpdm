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

package org.eclipse.tractusx.bpdm.pool.api.model.request

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema


@Schema(description = "Contains keywords used for searching in legal entity properties")
data class LegalEntityPropertiesSearchRequest constructor(
    @field:Parameter(description = "Filter legal entities by name")
    val legalName: String?,

    @field:Parameter(description = "Filter business partners by CX-BPN.")
    val id: String?,

    @field:Parameter(description = "Filter business partners by street name.")
    val street: String?,

    @field:Parameter(description = "Filter business partners by zip code.")
    val postcode: String?,

    @field:Parameter(description = "Filter business partners by city.")
    val city: String?,

    @field:Parameter(description = "Filter business partners by country code ISO 3166-1.")
    val country: String?,

) {
    companion object {
        val EmptySearchRequest = LegalEntityPropertiesSearchRequest(null, null, null, null, null, null)
    }
}
