/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.common.dto.saas

data class ValidationRequestSaas(
    val businessPartner: BusinessPartnerValidationSaas,
)

data class BusinessPartnerValidationSaas(
    val addresses: Collection<AddressValidationSaas>,
    val identifiers: Collection<IdentifierValidationSaas>,
    val legalForm: NameValidationSaas?,
    val names: Collection<ValueValidationSaas>
)

data class AddressValidationSaas(
    val administrativeAreas: Collection<ValueValidationSaas>,
    val country: CountryValidationSaas?,
    val localities: Collection<ValueValidationSaas>,
    val postalDeliveryPoints: Collection<ValueValidationSaas>,
    val postCodes: Collection<ValueValidationSaas>,
    val premises: Collection<ValueValidationSaas>,
    val thoroughfares: Collection<NameValidationSaas>
)

data class IdentifierValidationSaas(
    val type: TechnicalKeyValidationSaas,
    val value: String
)

data class ValueValidationSaas(
    val value: String
)

data class TechnicalKeyValidationSaas(
    val technicalKey: String
)

data class NameValidationSaas(
    val name: String
)

data class CountryValidationSaas(
    val shortName: String
)
