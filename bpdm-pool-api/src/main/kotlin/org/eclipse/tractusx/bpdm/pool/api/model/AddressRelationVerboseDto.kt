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

package org.eclipse.tractusx.bpdm.pool.api.model

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.RelationDescription

@Schema(
    name = "AddressRelationVerboseDto",
    description = "Directed relation between two business partners of type address"
)
data class AddressRelationVerboseDto(

    @get:Schema(description = "The type of relation between the logistic addresses")
    val type: AddressRelationType,

    @get:Schema(description = "BPNA of the address from which the relation starts (source)")
    val businessPartnerSourceBpna: String,

    @get:Schema(description = "BPNA of the address to which this relation points (target)")
    val businessPartnerTargetBpna: String,

    @get:Schema(description = "Validity periods of this address relation")
    val validityPeriods: Collection<RelationValidityPeriod>,

    @get:Schema(description = RelationDescription.reasonCode)
    val reasonCode: String
)