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

package org.eclipse.tractusx.bpdm.pool.service.validation.validator.impl

import org.eclipse.tractusx.bpdm.pool.dto.input.PostalAddress
import org.eclipse.tractusx.bpdm.pool.dto.valid.PostalAddressValid
import org.eclipse.tractusx.bpdm.pool.dto.validation.Validated
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.PostalAddressError
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.PostalAddressError.*
import org.eclipse.tractusx.bpdm.pool.service.validation.validator.*
import org.springframework.stereotype.Service

@Service
class PostalAddressValidatorImpl(
    private val geoDataValidator: GeoDataValidator,
    private val adminAreaValidator: AdminAreaValidator,
    private val requiredStringValidator: RequiredStringValidator,
    private val optionalStringValidator: OptionalStringValidator,
    private val countryCodeValidator: CountryCodeValidator
): PostalAddressValidator {

    override fun validate(addresses: List<PostalAddress>): List<Validated<PostalAddressValid, PostalAddressError>> {
        val geoCoordinates = geoDataValidator.validate(addresses.map { it.geographicCoordinates })
        val countries = countryCodeValidator.validate(addresses.map { it.country })
        val adminAreas = adminAreaValidator.validate(addresses.map { it.administrativeAreaLevel1 })
        val postCodes = optionalStringValidator.validate(addresses.map { it.postCode })
        val cities = requiredStringValidator.validate(addresses.map { it.city })

        return addresses.mapIndexed { index, address ->
            val errors = listOf(
                geoCoordinates[index].errors.map(::GeoData),
                countries[index].errors.map(::Country),
                adminAreas[index].errors.map(::AdminArea),
                postCodes[index].errors.map(::PostCode),
                cities[index].errors.map(::City)
            ).flatten().toSet()

            Validated.onEmpty(errors){
                PostalAddressValid(
                    geoCoordinates[index].validValue,
                    countries[index].validValue,
                    adminAreas[index].validValue,
                    postCodes[index].validValue,
                    cities[index].validValue
                )
            }
        }
    }
}