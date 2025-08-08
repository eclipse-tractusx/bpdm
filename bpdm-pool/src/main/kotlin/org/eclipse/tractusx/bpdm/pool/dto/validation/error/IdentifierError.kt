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

package org.eclipse.tractusx.bpdm.pool.dto.validation.error

sealed class IdentifierError(open val index: Int): ValidationError {
    data class Value(val value: RequiredStringError, override val index: Int): IdentifierError(index)
    data class Type(val type: RequiredMatchError, override val index: Int): IdentifierError(index)
    data class IssuingService(val issuingService: ExceedLengthError, override val index: Int): IdentifierError(index)
    data class Duplicate(override val index: Int): IdentifierError(index)


    fun withNewIndex(newIndex: Int):  IdentifierError{
        return when(this){
            is Duplicate -> copy(index = newIndex)
            is IssuingService -> copy(index = newIndex)
            is Type -> copy(index = newIndex)
            is Value -> copy(index = newIndex)
        }
    }
}