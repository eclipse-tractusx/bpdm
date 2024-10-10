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

package org.eclipse.tractusx.bpdm.common.mapping.types

import org.eclipse.tractusx.bpdm.common.mapping.BpdmExceedLengthValidation
import org.eclipse.tractusx.bpdm.common.mapping.BpdmFilledLimitedStringMapper
import org.eclipse.tractusx.bpdm.common.mapping.CommonValidationErrorCodes

const val TINY_STRING_LENGTH = 10

/**
 * A non-empty String limited to the length of [TINY_STRING_LENGTH]
 */
@JvmInline
value class FilledTinyString(val value: String) {
    init { assert(value) }

    companion object: BpdmFilledLimitedStringMapper<FilledTinyString>(
        BpdmExceedLengthValidation(TINY_STRING_LENGTH, CommonValidationErrorCodes.ExceedsTinyLength)
    ){
        override fun transform(value: String) = FilledTinyString(value)
    }
}