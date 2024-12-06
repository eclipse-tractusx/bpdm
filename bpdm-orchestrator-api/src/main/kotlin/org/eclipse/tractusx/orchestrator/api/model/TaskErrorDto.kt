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

package org.eclipse.tractusx.orchestrator.api.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes an error that happened during processing of a task \n")
data class TaskErrorDto(

    @get:Schema(description = "The type of error that occurred. \n" +
            "* `NaturalPersonError`: The provided record contains natural person information.\n" +
            "* `BpnErrorNotFound`: The provided record can not be matched to a legal entity or an address.\n" +
            "* `BpnErrorTooManyOptions`: The provided record can not link to a clear legal entity.\n" +
            "* `MandatoryFieldValidationFailed`: The provided record does not fulfill mandatory validation rules.\n" +
            "* `BlacklistCountryPresent`: The provided record is part of a country that is not allowed to be processed by the GR process (example: Brazil).\n" +
            "* `UnknownSpecialCharacters`: The provided record contains unallowed special characters.\n" , required = true)
    val type: TaskErrorType,

    @get:Schema(description = "The free text, detailed description of the error", required = true)
    val description: String
)
