/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.common.dto.cdq

data class ValidationRequestCdq(
    val businessPartner: BusinessPartnerValidationCdq,
)

data class BusinessPartnerValidationCdq(
    val addresses: Collection<AddressValidationCdq>,
    val identifiers: Collection<IdentifierValidationCdq>,
    val legalForm: NameValidationCdq?,
    val names: Collection<ValueValidationCdq>
)

data class AddressValidationCdq(
    val administrativeAreas: Collection<ValueValidationCdq>,
    val country: CountryValidationCdq?,
    val localities: Collection<ValueValidationCdq>,
    val postalDeliveryPoints: Collection<ValueValidationCdq>,
    val postCodes: Collection<ValueValidationCdq>,
    val premises: Collection<ValueValidationCdq>,
    val thoroughfares: Collection<ValueValidationCdq>
)

data class IdentifierValidationCdq(
    val type: TechnicalKeyValidationCdq,
    val value: String
)

data class ValueValidationCdq(
    val value: String
)

data class TechnicalKeyValidationCdq(
    val technicalKey: String
)

data class NameValidationCdq(
    val name: String
)

data class CountryValidationCdq(
    val shortName: String
)

