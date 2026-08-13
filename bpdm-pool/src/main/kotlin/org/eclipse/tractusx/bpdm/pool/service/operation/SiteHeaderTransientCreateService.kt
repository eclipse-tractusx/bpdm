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

package org.eclipse.tractusx.bpdm.pool.service.operation

import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.mapper.entity.SiteEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.parsed.SiteHeaderCreateParsed
import org.springframework.stereotype.Service

/**
 * Issues site BPNs and builds the unsaved site header entities for site creators that must wire the main address before
 * persisting.
 */
@Service
class SiteHeaderTransientCreateService(
    private val bpnIssueService: BpnIssueService,
    private val siteEntityMapper: SiteEntityMapper
) {

    /**
     * Builds the site headers with their BPNs issued, unsaved and unlogged, leaving persistence and changelog to the
     * caller.
     */
    fun createTransiently(request: List<SiteHeaderCreateParsed>): List<SiteDb>{
        val bpns = bpnIssueService.issueSiteBpns(request.size)
        return request.zip(bpns) { entry, bpn ->
            siteEntityMapper.toEntity(bpn, entry.legalEntity, entry.header, numberOfSharingMembers = 1)
        }
    }
}