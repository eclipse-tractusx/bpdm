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

package org.eclipse.tractusx.bpdm.common.mapping

import org.eclipse.tractusx.bpdm.common.mapping.types.*

enum class CommonValidationErrorCodes(val description: String) {
    IsNull("Is Null"),
    IsBlank("Is blank"),
    ExceedsTinyLength("Exceeds maximum length of $TINY_STRING_LENGTH"),
    ExceedsShortLength("Exceeds maximum length of $SHORT_STRING_LENGTH"),
    ExceedsMediumLength("Exceeds maximum length of $MEDIUM_STRING_LENGTH"),
    ExceedsLongLength("Exceeds maximum length of $LONG_STRING_LENGTH"),
    ExceedsHugeLength("Exceeds maximum length of $HUGE_STRING_LENGTH"),
    ISO31661("Not ISO 3166-1 conform"),
    ISO31662("Not ISO 3166-2 conform"),
    ISO6391("Not ISO 639-1 conform"),
    BpnL("Is not a BPNL");

    fun toValidationError(value: String?, context: ValidationContext) =
        ValidationError(name, description, value, context)
}