/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv7.outbound

import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierTypeDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmRequestSizeException
import org.eclipse.tractusx.bpdm.pool.model.error.BpnIdentifierSearchParseError
import org.eclipse.tractusx.bpdm.pool.model.error.BpnRequestIdentifierSearchParseError
import org.eclipse.tractusx.bpdm.pool.model.error.SearchValuesTooMany
import org.springframework.stereotype.Component

/**
 * Maps the BPN search parsers' sealed parse errors to the errors the BPN search endpoints report them with.
 */
@Component
class BpnSearchParseErrorMapper {

    /**
     * Returns the exception reporting a failed BPN-by-identifier search parse, surfacing the first error because the
     * search fails as a whole rather than per identifier.
     */
    fun toIdentifierSearchException(errors: List<BpnIdentifierSearchParseError>): RuntimeException =
        when (val error = errors.first()) {
            is SearchValuesTooMany -> BpdmRequestSizeException(error.count, error.maxCount)
            is BpnIdentifierSearchParseError.IdentifierTypeNotFound ->
                BpdmNotFoundException(IdentifierTypeDb::class, "${error.typeKey}/${error.businessPartnerType}")
        }

    /**
     * Returns the exception reporting a failed BPN-by-request-identifier search parse, surfacing the first error because
     * the search fails as a whole rather than per request identifier.
     */
    fun toRequestIdentifierSearchException(errors: List<BpnRequestIdentifierSearchParseError>): RuntimeException =
        when (val error = errors.first()) {
            is SearchValuesTooMany -> BpdmRequestSizeException(error.count, error.maxCount)
        }
}
