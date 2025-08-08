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

sealed class StreetError: ValidationError {
    data class Name(val name: ExceedLengthError): StreetError()
    data class HouseNumber(val houseNumber: ExceedLengthError): StreetError()
    data class HouseNumberSupplement(val houseNumberSupplement: ExceedLengthError): StreetError()
    data class Milestone(val milestone: ExceedLengthError): StreetError()
    data class Direction(val direction: ExceedLengthError): StreetError()
    data class NamePrefix(val namePrefix: ExceedLengthError): StreetError()
    data class AdditionalNamePrefix(val additionalNamePrefix: ExceedLengthError): StreetError()
    data class NameSuffix(val nameSuffix: ExceedLengthError): StreetError()
    data class AdditionalNameSuffix(val additionalNameSuffix: ExceedLengthError): StreetError()
}