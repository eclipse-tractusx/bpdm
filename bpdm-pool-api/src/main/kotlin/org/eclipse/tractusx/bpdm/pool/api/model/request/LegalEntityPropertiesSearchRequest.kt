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

package org.eclipse.tractusx.bpdm.pool.api.model.request

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema


@Schema(name = "LegalEntityPropertiesSearchRequest", description = "Contains keywords used for searching in legal entity properties")
data class LegalEntityPropertiesSearchRequest constructor(
    @field:Parameter(description = "Filter legal entities by name")
    val name: String?,
    @field:Parameter(description = "Filter legal entities by legal form name")
    val legalForm: String?,
    @field:Parameter(description = "Filter legal entities by status official denotation")
    val status: String?,
    @field:Parameter(description = "Filter legal entities by classification denotation")
    val classification: String?,
) {
    companion object {
        val EmptySearchRequest = LegalEntityPropertiesSearchRequest(null, null, null, null)
    }
}

