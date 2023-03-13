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

package org.eclipse.tractusx.bpdm.pool.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.response.AddressResponse

@Schema(name = "SitePartnerCreateResponse", description = "Created business partner record of type site")
data class SitePartnerCreateResponse(
    @Schema(description = "Business Partner Number, main identifier value for sites")
    val bpn: String,
    @Schema(description = "Site name")
    val name: String,
    @Schema(description = "Main address of this site")
    val mainAddress: AddressResponse,
    @Schema(description = "User defined index to conveniently match this entry to the corresponding entry from the request")
    val index: String?
) {

}
