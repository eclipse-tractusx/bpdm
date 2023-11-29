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

package org.eclipse.tractusx.bpdm.pool.api.model.verbose

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.LegalEntityDescription
import org.eclipse.tractusx.bpdm.common.dto.response.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.common.service.DataClassUnwrappedJsonDeserializer

@JsonDeserialize(using = DataClassUnwrappedJsonDeserializer::class)
@Schema(description = LegalEntityDescription.header)
data class PoolLegalEntityVerboseDto(

    @get:Schema(description = LegalEntityDescription.legalName)
    val legalName: String,

    @field:JsonUnwrapped
    val legalEntity: LegalEntityVerboseDto,

    // TODO OpenAPI description for complex field does not work!!
    @get:Schema(description = LegalEntityDescription.legalAddress)
    val legalAddress: LogisticAddressVerboseDto,
)
