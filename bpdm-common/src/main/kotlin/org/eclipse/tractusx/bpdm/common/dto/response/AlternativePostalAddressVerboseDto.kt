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

package org.eclipse.tractusx.bpdm.common.dto.response

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.model.DeliveryServiceType
import org.eclipse.tractusx.bpdm.common.service.DataClassUnwrappedJsonDeserializer

@JsonDeserialize(using = DataClassUnwrappedJsonDeserializer::class)
@Schema(name = "AlternativePostalAddressVerboseDto", description = "Alternative Postal Address Part")
data class AlternativePostalAddressVerboseDto(

    @field:JsonUnwrapped
    val baseAddress: BasePostalAddressVerboseDto,

    @field:JsonUnwrapped
    val areaPart: AreaDistrictAlternativVerboseDto,

    @get:Schema(description = "Describes the PO Box or private Bag number the delivery should be placed at.")
    val deliveryServiceNumber: String = "",

    @get:Schema(description = "The type of this specified delivery")
    val type: DeliveryServiceType = DeliveryServiceType.PO_BOX,

    @get:Schema(description = "Delivery Service Qualifier")
    val deliveryServiceQualifier: String = "",
)
