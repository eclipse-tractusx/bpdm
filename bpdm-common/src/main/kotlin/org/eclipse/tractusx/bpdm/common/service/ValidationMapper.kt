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

package org.eclipse.tractusx.bpdm.common.service

import org.eclipse.tractusx.bpdm.common.dto.cdq.*

/**
 * This class provides functions for mapping the regular Cdq Business Partner model to its corresponding validation model
 **/
class ValidationMapper {

    fun toValidation(partner: BusinessPartnerCdq) =
        with(partner) {
            BusinessPartnerValidationCdq(
                addresses = addresses.map { toValidation(it) },
                identifiers = identifiers.map { toValidationCdq(it) },
                legalForm = legalForm?.name?.let { NameValidationCdq(it) },
                names = names.map { ValueValidationCdq(it.value) }
            )
        }


    fun toValidation(address: AddressCdq) =
        with(address) {
            AddressValidationCdq(
                administrativeAreas = administrativeAreas.map { ValueValidationCdq(it.value!!) },
                country = CountryValidationCdq(country?.shortName?.alpha2!!),
                localities = localities.map { ValueValidationCdq(it.value!!) },
                postalDeliveryPoints = postalDeliveryPoints.map { ValueValidationCdq(it.value!!) },
                postCodes = postCodes.map { ValueValidationCdq(it.value!!) },
                premises = premises.map { ValueValidationCdq(it.value!!) },
                thoroughfares = thoroughfares.map { ValueValidationCdq(it.value!!) }
            )
        }

    private fun toValidationCdq(identifier: IdentifierCdq) =
        IdentifierValidationCdq(TechnicalKeyValidationCdq(identifier.type?.technicalKey!!), identifier.value!!)

}