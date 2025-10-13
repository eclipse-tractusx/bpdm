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

package org.eclipse.tractusx.bpdm.gate.api.v6.model.response

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerSharingError
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import java.time.LocalDateTime

data class SharingStateDto(
    @get:Schema(description = "The external identifier of the business partner for which the sharing state entry was created.")
    val externalId: String,

    @get:Schema(description = "One of the sharing state types of the current sharing state.")
    val sharingStateType: SharingStateType = SharingStateType.Initial,

    @get:Schema(description = "One of the sharing error codes in case the current sharing state type is \"error\". \n" +
            "* `NaturalPersonError`: The provided record contains natural person information.\n" +
            "* `BpnErrorNotFound`: The provided record can not be matched to a legal entity or an address.\n" +
            "* `BpnErrorTooManyOptions`: The provided record can not link to a clear legal entity.\n" +
            "* `MandatoryFieldValidationFailed`: The provided record does not fulfill mandatory validation rules.\n" +
            "* `BlacklistCountryPresent`: The provided record is part of a country that is not allowed to be processed by the GR process (example: Brazil).\n" +
            "* `UnknownSpecialCharacters`: The provided record contains unallowed special characters.")
    val sharingErrorCode: BusinessPartnerSharingError? = null,

    @get:Schema(description = "The error message in case the current sharing state type is \"error\".")
    val sharingErrorMessage: String? = null,

    @get:Schema(description = "The date and time when the sharing process was started.")
    val sharingProcessStarted: LocalDateTime? = null,

    @get:Schema(description = "The orchestrator task identifier that was created")
    val taskId: String? = null
)
