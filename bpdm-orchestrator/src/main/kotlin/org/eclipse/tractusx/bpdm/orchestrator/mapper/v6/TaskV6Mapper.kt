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

package org.eclipse.tractusx.bpdm.orchestrator.mapper.v6

import org.eclipse.tractusx.orchestrator.api.model.TaskErrorType
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskErrorTypeV6
import org.eclipse.tractusx.orchestrator.api.v6.model.TaskStepV6

object TaskV6Mapper {

    fun TaskStepV6.toTaskStep(): TaskStep =
        when (this) {
            TaskStepV6.CleanAndSync -> TaskStep.CleanAndSync
            TaskStepV6.PoolSync -> TaskStep.PoolSync
            TaskStepV6.Clean -> TaskStep.Clean
        }

    fun TaskStep.toV6(): TaskStepV6 =
        when (this) {
            TaskStep.CleanAndSync -> TaskStepV6.CleanAndSync
            TaskStep.PoolSync -> TaskStepV6.PoolSync
            TaskStep.Clean -> TaskStepV6.Clean
        }

    fun TaskErrorTypeV6.toTaskErrorType(): TaskErrorType =
        when (this) {
            TaskErrorTypeV6.Timeout -> TaskErrorType.Timeout
            TaskErrorTypeV6.Unspecified -> TaskErrorType.Unspecified
            TaskErrorTypeV6.NaturalPersonError -> TaskErrorType.NaturalPersonError
            TaskErrorTypeV6.BpnErrorNotFound -> TaskErrorType.BpnErrorNotFound
            TaskErrorTypeV6.BpnErrorTooManyOptions -> TaskErrorType.BpnErrorTooManyOptions
            TaskErrorTypeV6.MandatoryFieldValidationFailed -> TaskErrorType.MandatoryFieldValidationFailed
            TaskErrorTypeV6.BlacklistCountryPresent -> TaskErrorType.BlacklistCountryPresent
            TaskErrorTypeV6.UnknownSpecialCharacters -> TaskErrorType.UnknownSpecialCharacters
        }

    fun TaskErrorType.toV6(): TaskErrorTypeV6 =
        when (this) {
            TaskErrorType.Timeout -> TaskErrorTypeV6.Timeout
            TaskErrorType.Unspecified -> TaskErrorTypeV6.Unspecified
            TaskErrorType.NaturalPersonError -> TaskErrorTypeV6.NaturalPersonError
            TaskErrorType.BpnErrorNotFound -> TaskErrorTypeV6.BpnErrorNotFound
            TaskErrorType.BpnErrorTooManyOptions -> TaskErrorTypeV6.BpnErrorTooManyOptions
            TaskErrorType.MandatoryFieldValidationFailed -> TaskErrorTypeV6.MandatoryFieldValidationFailed
            TaskErrorType.BlacklistCountryPresent -> TaskErrorTypeV6.BlacklistCountryPresent
            TaskErrorType.UnknownSpecialCharacters -> TaskErrorTypeV6.UnknownSpecialCharacters
        }
}
