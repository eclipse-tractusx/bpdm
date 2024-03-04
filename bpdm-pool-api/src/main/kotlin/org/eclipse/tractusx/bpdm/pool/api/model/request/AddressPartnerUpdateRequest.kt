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

package org.eclipse.tractusx.bpdm.pool.api.model.request

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.RequestWithKey
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.LogisticAddressDescription
import org.eclipse.tractusx.bpdm.common.service.DataClassUnwrappedJsonDeserializer
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddress

@JsonDeserialize(using = DataClassUnwrappedJsonDeserializer::class)
@Schema(description = LogisticAddressDescription.headerUpdateRequest)
data class AddressPartnerUpdateRequest(

    @Schema(description = LogisticAddressDescription.bpna)
    val bpna: String,

    @field:JsonUnwrapped
    val address: LogisticAddress

): RequestWithKey {
    override fun getRequestKey(): String {
        return bpna
    }
}
