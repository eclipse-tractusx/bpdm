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

import org.eclipse.tractusx.bpdm.common.dto.saas.*

/**
 * This class provides functions for mapping the regular SaaS Business Partner model to its corresponding validation model
 **/
class ValidationMapper {

    fun toValidation(partner: BusinessPartnerSaas) =
        with(partner) {
            BusinessPartnerValidationSaas(
                addresses = addresses.map { toValidation(it) },
                identifiers = identifiers.map { toValidationSaas(it) },
                legalForm = legalForm?.name?.let { NameValidationSaas(it) },
                names = names.map { ValueValidationSaas(it.value) }
            )
        }


    fun toValidation(address: AddressSaas) =
        with(address) {
            AddressValidationSaas(
                administrativeAreas = administrativeAreas.map { ValueValidationSaas(it.value!!) },
                country = CountryValidationSaas(country?.shortName?.alpha2!!),
                localities = localities.map { ValueValidationSaas(it.value!!) },
                postalDeliveryPoints = postalDeliveryPoints.map { ValueValidationSaas(it.value!!) },
                postCodes = postCodes.map { ValueValidationSaas(it.value!!) },
                premises = premises.map { ValueValidationSaas(it.value!!) },
                thoroughfares = thoroughfares.map { ValueValidationSaas(it.value!!) }
            )
        }

    private fun toValidationSaas(identifier: IdentifierSaas) =
        IdentifierValidationSaas(TechnicalKeyValidationSaas(identifier.type?.technicalKey!!), identifier.value!!)

}