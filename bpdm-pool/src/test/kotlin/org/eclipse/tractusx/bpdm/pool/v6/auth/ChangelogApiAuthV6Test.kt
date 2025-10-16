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

package org.eclipse.tractusx.bpdm.pool.v6.auth

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.PoolChangelogApi
import org.eclipse.tractusx.bpdm.pool.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.pool.api.model.response.ChangelogEntryVerboseDto
import org.eclipse.tractusx.bpdm.pool.v6.IsPoolV6Test
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType
import org.junit.jupiter.api.Test

interface ChangelogApiAuthV6Test: PoolChangelogApi, IsPoolV6Test {

    val expectationGetChangelogEntries: AuthExpectationType

    @Test
    fun getChangelogEntries(){
        authAssertionHelper.assert(expectationGetChangelogEntries){ getChangelogEntries(ChangelogSearchRequest(), PaginationRequest()) }
    }

    override fun getChangelogEntries(changelogSearchRequest: ChangelogSearchRequest, paginationRequest: PaginationRequest): PageDto<ChangelogEntryVerboseDto> {
        return poolClient.changelogs.getChangelogEntries(changelogSearchRequest, paginationRequest)
    }
}