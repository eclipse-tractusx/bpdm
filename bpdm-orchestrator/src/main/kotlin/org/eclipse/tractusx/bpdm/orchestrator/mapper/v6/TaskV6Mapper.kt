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

import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskErrorTypeV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.task.TaskStepV7
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskErrorTypeV6
import org.eclipse.tractusx.bpdm.orchestrator.api.v6.model.task.TaskStepV6

object TaskV6Mapper {

    fun TaskStepV6.toTaskStep(): TaskStepV7 =
        when (this) {
            TaskStepV6.CleanAndSync -> TaskStepV7.CleanAndSync
            TaskStepV6.PoolSync -> TaskStepV7.PoolSync
            TaskStepV6.Clean -> TaskStepV7.Clean
        }

    fun TaskStepV7.toV6(): TaskStepV6 =
        when (this) {
            TaskStepV7.CleanAndSync -> TaskStepV6.CleanAndSync
            TaskStepV7.PoolSync -> TaskStepV6.PoolSync
            TaskStepV7.Clean -> TaskStepV6.Clean
        }

    fun TaskErrorTypeV6.toTaskErrorType(): TaskErrorTypeV7 =
        when (this) {
            TaskErrorTypeV6.Timeout -> TaskErrorTypeV7.Timeout
            TaskErrorTypeV6.Unspecified -> TaskErrorTypeV7.Unspecified
            TaskErrorTypeV6.NaturalPersonError -> TaskErrorTypeV7.NaturalPersonError
            TaskErrorTypeV6.BpnErrorNotFound -> TaskErrorTypeV7.BpnErrorNotFound
            TaskErrorTypeV6.BpnErrorTooManyOptions -> TaskErrorTypeV7.BpnErrorTooManyOptions
            TaskErrorTypeV6.MandatoryFieldValidationFailed -> TaskErrorTypeV7.MandatoryFieldValidationFailed
            TaskErrorTypeV6.BlacklistCountryPresent -> TaskErrorTypeV7.BlacklistCountryPresent
            TaskErrorTypeV6.UnknownSpecialCharacters -> TaskErrorTypeV7.UnknownSpecialCharacters
        }

    fun TaskErrorTypeV7.toV6(): TaskErrorTypeV6 =
        when (this) {
            TaskErrorTypeV7.Timeout -> TaskErrorTypeV6.Timeout
            TaskErrorTypeV7.Unspecified -> TaskErrorTypeV6.Unspecified
            TaskErrorTypeV7.NaturalPersonError -> TaskErrorTypeV6.NaturalPersonError
            TaskErrorTypeV7.BpnErrorNotFound -> TaskErrorTypeV6.BpnErrorNotFound
            TaskErrorTypeV7.BpnErrorTooManyOptions -> TaskErrorTypeV6.BpnErrorTooManyOptions
            TaskErrorTypeV7.MandatoryFieldValidationFailed -> TaskErrorTypeV6.MandatoryFieldValidationFailed
            TaskErrorTypeV7.BlacklistCountryPresent -> TaskErrorTypeV6.BlacklistCountryPresent
            TaskErrorTypeV7.UnknownSpecialCharacters -> TaskErrorTypeV6.UnknownSpecialCharacters
        }
}
