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

package org.eclipse.tractusx.bpdm.pool.mapper.apiv7

import org.eclipse.tractusx.bpdm.pool.api.v7.model.changelog.ChangelogEntryVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.exception.BpdmRequestSizeException
import org.eclipse.tractusx.bpdm.pool.model.output.changelog.ChangelogEntryRecord
import org.eclipse.tractusx.bpdm.pool.model.parsed.changelog.ChangelogSearchError
import org.springframework.stereotype.Component

/**
 * Maps changelog records to their API v7 form.
 */
@Component
class ApiV7ChangelogMapper {

    /**
     * Returns the API DTO for [record].
     */
    fun toDto(record: ChangelogEntryRecord): ChangelogEntryVerboseV7Dto =
        ChangelogEntryVerboseV7Dto(
            bpn = record.bpn,
            businessPartnerType = record.businessPartnerType,
            timestamp = record.timestamp,
            changelogType = record.changelogType
        )

    /**
     * Returns the exception the API reports [errors] with, surfacing only the first because the search fails as a
     * whole.
     */
    fun toException(errors: List<ChangelogSearchError>): RuntimeException =
        when (val error = errors.first()) {
            is ChangelogSearchError.BpnsTooMany -> BpdmRequestSizeException(error.count, error.maxCount)
        }
}
