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

import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.pool.api.v6.model.bpn.BpnIdentifierMappingV6Dto
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierTypeDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmRequestSizeException
import org.eclipse.tractusx.bpdm.pool.model.output.bpn.BpnIdentifierMatchRecord
import org.eclipse.tractusx.bpdm.pool.model.parsed.bpn.BpnIdentifierSearchError
import org.springframework.stereotype.Component

/**
 * Maps BPN lookup records to their API v6 form.
 */
@Component
class ApiV6BpnMapper {

    /**
     * Returns the API DTO for [record].
     */
    fun toDto(record: BpnIdentifierMatchRecord): BpnIdentifierMappingV6Dto =
        BpnIdentifierMappingV6Dto(idValue = record.identifierValue, bpn = record.bpn)

    /**
     * Returns the exception the API reports [errors] with, surfacing only the first because the search fails as a
     * whole rather than per identifier.
     */
    fun toException(errors: List<BpnIdentifierSearchError>): RuntimeException =
        when (val error = errors.first()) {
            is BpnIdentifierSearchError.IdentifierValuesTooMany -> BpdmRequestSizeException(error.count, error.maxCount)
            is BpnIdentifierSearchError.IdentifierTypeNotFound ->
                BpdmNotFoundException(IdentifierTypeDb::class, "${error.typeKey}/${error.businessPartnerType}")
        }
}
