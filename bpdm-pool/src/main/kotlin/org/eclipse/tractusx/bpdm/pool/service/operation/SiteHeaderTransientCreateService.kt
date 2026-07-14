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

import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.mapper.entity.SiteEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.PendingSiteWrite
import org.eclipse.tractusx.bpdm.pool.model.SiteHeaderCreateParsed
import org.eclipse.tractusx.bpdm.pool.repository.SiteRepository
import org.eclipse.tractusx.bpdm.pool.service.BpnIssuingService
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerEquivalenceMapper
import org.eclipse.tractusx.bpdm.pool.service.PartnerChangelogService

class SiteHeaderTransientCreateService(
    private val bpnIssuingService: BpnIssuingService,
    private val siteEntityMapper: SiteEntityMapper
) {

    fun createTransiently(request: List<SiteHeaderCreateParsed>): List<SiteDb>{
        val bpns = bpnIssuingService.issueSiteBpns(request.size)
        // A new site's confidence starts with one sharing member (preserves the previous create behavior).
        return request.zip(bpns) { entry, bpn ->
            siteEntityMapper.toEntity(bpn, entry.legalEntity, entry.header, numberOfSharingMembers = 1)
        }
    }
}