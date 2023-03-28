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

package org.eclipse.tractusx.bpdm.gate.api.model.response

import io.swagger.v3.oas.annotations.media.Schema

data class GateLogisticAddressExtension(
    //TODO use enum for this
    @Schema(description = "IsCustomer: true, IsSupplier: false")
    val role: Boolean?,

    @Schema(description = "This is the internalID of the sharing member in their own ERP systems. This is necessary for the correct mapping on the way back to the sharing member.")
    val sharingMemberExternalID: String,

    // TODO BPNL ?
    @Schema(description = "This is the sharing member who has uploaded the data set. e.g. Schaeffler uploads a BMW-data set which leads Schaeffler as the UploadingEntity but not as the owner.")
    val uploadingEntity: String,

    // TODO BPNL ?
    @Schema(description = "This is the owner of the described legal entity in the data set.")
    val legalEntityOwner: String,
)
