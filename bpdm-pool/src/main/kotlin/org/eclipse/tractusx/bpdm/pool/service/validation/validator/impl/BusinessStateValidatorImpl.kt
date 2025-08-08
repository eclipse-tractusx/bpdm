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

import org.eclipse.tractusx.bpdm.pool.dto.input.BusinessState
import org.eclipse.tractusx.bpdm.pool.dto.valid.BusinessStateValid
import org.eclipse.tractusx.bpdm.pool.dto.validation.Validated
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.BusinessStateError
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.IsMissingError
import org.eclipse.tractusx.bpdm.pool.service.validation.validator.BusinessStateValidator
import org.springframework.stereotype.Service

@Service
class BusinessStateValidatorImpl: BusinessStateValidator {

    override fun validate(businessStates: List<BusinessState>): List<Validated<BusinessStateValid, BusinessStateError>> {
        return businessStates.mapIndexed { index, state ->
            val error = if(state.type == null) BusinessStateError.Type(IsMissingError(), index) else null
            val errors = setOfNotNull(error)

            Validated.onEmpty(errors){
                BusinessStateValid(state.validFrom, state.validTo, state.type!!)
            }
        }
    }
}