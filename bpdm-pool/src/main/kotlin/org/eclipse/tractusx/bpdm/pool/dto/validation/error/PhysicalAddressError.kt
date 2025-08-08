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

sealed class PhysicalAddressError: ValidationError {
    data class PostalAddress(val postalAddress: PostalAddressError): PhysicalAddressError()
    data class AdminAreaLevel2(val adminAreaLevel2: ExceedLengthError): PhysicalAddressError()
    data class AdminAreaLevel3(val adminAreaLevel3: ExceedLengthError): PhysicalAddressError()
    data class District(val district: ExceedLengthError): PhysicalAddressError()
    data class Street(val street: StreetError): PhysicalAddressError()
    data class CompanyPostCode(val companyPostCode: ExceedLengthError): PhysicalAddressError()
    data class IndustrialZone(val industrialZone: ExceedLengthError): PhysicalAddressError()
    data class Building(val building: ExceedLengthError): PhysicalAddressError()
    data class Floor(val floor: ExceedLengthError): PhysicalAddressError()
    data class Door(val door: ExceedLengthError): PhysicalAddressError()
    data class TaxJurisdiction(val taxJurisdiction: ExceedLengthError): PhysicalAddressError()
}