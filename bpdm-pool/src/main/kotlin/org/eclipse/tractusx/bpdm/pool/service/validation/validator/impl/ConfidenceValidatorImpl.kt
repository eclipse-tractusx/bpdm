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

package org.eclipse.tractusx.bpdm.pool.service.validation.validator.impl

import org.eclipse.tractusx.bpdm.pool.dto.input.Confidence
import org.eclipse.tractusx.bpdm.pool.dto.valid.ConfidenceValid
import org.eclipse.tractusx.bpdm.pool.dto.validation.Validated
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.ConfidenceError
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.IsMissingError
import org.eclipse.tractusx.bpdm.pool.service.validation.validator.ConfidenceValidator
import org.springframework.stereotype.Service

@Service
class ConfidenceValidatorImpl: ConfidenceValidator {

    private val sharedByOwnerMissing = ConfidenceError.SharedByOwner(IsMissingError())
    private val checkedByExternalDataSourceMissing = ConfidenceError.CheckedExternally(IsMissingError())
    private val numberSharingMembersMissing = ConfidenceError.SharingMemberAmount(IsMissingError())
    private val lastCheckMissing = ConfidenceError.LastCheck(IsMissingError())
    private val nextCheckMissing = ConfidenceError.NextCheck(IsMissingError())
    private val confidenceLevelMissing = ConfidenceError.Level(IsMissingError())

    override fun validate(confidences: List<Confidence>): List<Validated<ConfidenceValid, ConfidenceError>> {
        return confidences.mapIndexed { index, confidence ->
            val errors = listOfNotNull(
                sharedByOwnerMissing.takeIf { confidence.sharedByOwner == null },
                checkedByExternalDataSourceMissing.takeIf { confidence.checkedByExternalDataSource == null },
                numberSharingMembersMissing.takeIf { confidence.numberOfSharingMembers == null },
                lastCheckMissing.takeIf { confidence.lastConfidenceCheckAt == null },
                nextCheckMissing.takeIf { confidence.nextConfidenceCheckAt == null },
                confidenceLevelMissing.takeIf { confidence.confidenceLevel == null },
            )

            Validated.onEmpty(errors){
                ConfidenceValid(
                    confidence.sharedByOwner!!,
                    confidence.checkedByExternalDataSource!!,
                    confidence.numberOfSharingMembers!!,
                    confidence.lastConfidenceCheckAt!!,
                    confidence.nextConfidenceCheckAt!!,
                    confidence.confidenceLevel!!
                )
            }
        }
    }
}