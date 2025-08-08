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

import org.eclipse.tractusx.bpdm.pool.dto.input.PhysicalAddress
import org.eclipse.tractusx.bpdm.pool.dto.valid.PhysicalAddressValid
import org.eclipse.tractusx.bpdm.pool.dto.validation.Validated
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.PhysicalAddressError
import org.eclipse.tractusx.bpdm.pool.service.validation.validator.OptionalStringValidator
import org.eclipse.tractusx.bpdm.pool.service.validation.validator.PhysicalAddressValidator
import org.eclipse.tractusx.bpdm.pool.service.validation.validator.PostalAddressValidator
import org.eclipse.tractusx.bpdm.pool.service.validation.validator.StreetValidator
import org.springframework.stereotype.Service

@Service
class PhysicalAddressValidatorImpl(
    private val postalAddressValidator: PostalAddressValidator,
    private val optionalStringValidator: OptionalStringValidator,
    private val streetValidator: StreetValidator
): PhysicalAddressValidator {

    override fun validate(physicalAddresses: List<PhysicalAddress>): List<Validated<PhysicalAddressValid, PhysicalAddressError>> {

        val baseAddresses = postalAddressValidator.validate(physicalAddresses)
        val adminAreasLevel2 = optionalStringValidator.validate(physicalAddresses.map { it.administrativeAreaLevel2 })
        val adminAreasLevel3 = optionalStringValidator.validate(physicalAddresses.map { it.administrativeAreaLevel3 })
        val districts = optionalStringValidator.validate(physicalAddresses.map { it.district })
        val streets = streetValidator.validate(physicalAddresses.map { it.street })
        val companyPostCodes = optionalStringValidator.validate(physicalAddresses.map { it.companyPostCode })
        val industrialZones = optionalStringValidator.validate(physicalAddresses.map { it.industrialZone })
        val buildings = optionalStringValidator.validate(physicalAddresses.map { it.building })
        val floors = optionalStringValidator.validate(physicalAddresses.map { it.floor })
        val doors = optionalStringValidator.validate(physicalAddresses.map { it.door })
        val taxJurisdictionCodes = optionalStringValidator.validate(physicalAddresses.map { it.taxJurisdictionCode })

        return physicalAddresses.mapIndexed { index, physicalAddress ->
            val errors = listOf(
                baseAddresses[index].errors.map(PhysicalAddressError::PostalAddress),
                adminAreasLevel2[index].errors.map(PhysicalAddressError::AdminAreaLevel2),
                adminAreasLevel3[index].errors.map(PhysicalAddressError::AdminAreaLevel3),
                districts[index].errors.map(PhysicalAddressError::District),
                streets[index].errors.map(PhysicalAddressError::Street),
                companyPostCodes[index].errors.map(PhysicalAddressError::CompanyPostCode),
                industrialZones[index].errors.map(PhysicalAddressError::IndustrialZone),
                buildings[index].errors.map(PhysicalAddressError::Building),
                floors[index].errors.map(PhysicalAddressError::Floor),
                doors[index].errors.map(PhysicalAddressError::Door),
                taxJurisdictionCodes[index].errors.map(PhysicalAddressError::TaxJurisdiction)
            ).flatten()

            Validated.onEmpty(errors){
                PhysicalAddressValid(
                    baseAddresses[index].validValue,
                    adminAreasLevel2[index].validValue,
                    adminAreasLevel3[index].validValue,
                    districts[index].validValue,
                    streets[index].validValue,
                    companyPostCodes[index].validValue,
                    industrialZones[index].validValue,
                    buildings[index].validValue,
                    floors[index].validValue,
                    doors[index].validValue,
                    taxJurisdictionCodes[index].validValue
                )
            }
            }
    }
}
