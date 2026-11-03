/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.mapper.apiv6

import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.CountrySubdivisionV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.CountryV6Dto
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.AdministrativeAreaRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.CountryRecord
import org.springframework.stereotype.Component

/**
 * Maps metadata records to their API v6 form.
 */
@Component
class ApiV6MetadataMapper {

    /**
     * Returns the API DTO for [record].
     */
    fun toDto(record: CountryRecord): CountryV6Dto =
        CountryV6Dto(
            countryCode = record.countryCode,
            name = record.name,
            description = record.description
        )

    /**
     * Returns the API DTO for [record].
     */
    fun toDto(record: AdministrativeAreaRecord): CountrySubdivisionV6Dto =
        CountrySubdivisionV6Dto(
            countryCode = record.countryCode,
            code = record.code,
            name = record.name
        )
}
