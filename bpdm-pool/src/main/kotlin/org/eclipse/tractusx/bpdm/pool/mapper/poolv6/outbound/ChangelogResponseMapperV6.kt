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

package org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound

import org.eclipse.tractusx.bpdm.pool.api.v6.model.ChangelogTypeV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.ChangelogEntryVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.entity.PartnerChangelogEntryDb
import org.springframework.stereotype.Component

/**
 * Maps stored changelog entries to the v6 API changelog DTOs.
 */
@Component
class ChangelogResponseMapperV6 {

    /**
     * Returns the given changelog entry as the v6 API reports it.
     */
    fun toChangelogEntry(changelogEntry: PartnerChangelogEntryDb): ChangelogEntryVerboseDtoV6 =
        with(changelogEntry) {
            ChangelogEntryVerboseDtoV6(bpn, businessPartnerType, updatedAt, ChangelogTypeV6.valueOf(changelogType.name))
        }
}
