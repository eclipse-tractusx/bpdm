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

import org.eclipse.tractusx.bpdm.pool.exception.BpdmRequestSizeException
import org.eclipse.tractusx.bpdm.pool.model.error.ChangelogSearchParseError
import org.eclipse.tractusx.bpdm.pool.model.error.SearchValuesTooMany
import org.springframework.stereotype.Component

/**
 * Maps the changelog search parser's sealed parse errors to the errors the changelog endpoints report them with.
 */
@Component
class ChangelogParseErrorMapper {

    /**
     * Returns the exception reporting a failed changelog search parse, surfacing the first error because the search
     * fails as a whole rather than per entry.
     */
    fun toSearchException(errors: List<ChangelogSearchParseError>): RuntimeException =
        when (val error = errors.first()) {
            is SearchValuesTooMany -> BpdmRequestSizeException(error.count, error.maxCount)
        }
}
