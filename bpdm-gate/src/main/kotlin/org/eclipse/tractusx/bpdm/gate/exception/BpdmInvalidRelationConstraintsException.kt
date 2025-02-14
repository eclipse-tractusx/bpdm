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

package org.eclipse.tractusx.bpdm.gate.exception

import org.eclipse.tractusx.bpdm.gate.service.IRelationService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BpdmInvalidRelationConstraintsException(
    val errors: List<String>
) : RuntimeException("The following errors have been discovered when validating business partner relationships: ${errors.joinToString(System.lineSeparator())}"){

    companion object{
        fun fromConstraintErrors(errors: List<IRelationService.ConstraintError>) =
            BpdmInvalidRelationConstraintsException(errors.map { "Constraint for relation '${it.externalId}' is violated by value '${it.erroneousValue}': ${it.message}"})
    }
}