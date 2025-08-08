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

import org.eclipse.tractusx.bpdm.pool.dto.validation.ValidatedOptional
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.NoMatchError
import org.eclipse.tractusx.bpdm.pool.entity.RegionDb
import org.eclipse.tractusx.bpdm.pool.repository.RegionRepository
import org.eclipse.tractusx.bpdm.pool.service.validation.validator.AdminAreaValidator
import org.springframework.stereotype.Service

@Service
class AdminAreaValidatorImpl(
    private val regionRepository: RegionRepository,
    private val matchValidator: MatchValidator
): AdminAreaValidator {

    override fun validate(contents: List<String?>): List<ValidatedOptional<RegionDb, NoMatchError>> {
        val foundRegions = regionRepository.findByRegionCodeIn(contents.filterNotNull().toSet())
        val regionsByCode = foundRegions.associateBy { it.regionCode }
        return matchValidator.matchNullable(contents, regionsByCode)
    }
}