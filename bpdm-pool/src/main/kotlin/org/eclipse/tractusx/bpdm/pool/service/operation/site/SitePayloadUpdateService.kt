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

package org.eclipse.tractusx.bpdm.pool.service.operation.site

import org.eclipse.tractusx.bpdm.pool.mapper.ParsedToDbMapper
import org.eclipse.tractusx.bpdm.pool.model.parsed.businesspartner.UpsertResultParsed
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.raw.businesspartner.FieldUpdateRaw
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.SiteChangeParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.site.SiteUpdateParsed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Applies a full parsed site-update payload — header content plus main address — to an already-resolved site, replacing
 * every field the payload covers.
 */
@Service
class SitePayloadUpdateService(
    private val siteUpdateService: SiteUpdateService,
    private val parsedToDbMapper: ParsedToDbMapper
) {

    /**
     * Applies the given payloads in full and reports for each site whether it actually changed.
     */
    @Transactional
    fun update(parsed: List<SiteUpdateParsed>): List<UpsertResultParsed<SiteDb>> {
        val updateRequests = parsed.map {
            SiteChangeParsed(
                it.target,
                parsedToDbMapper.toSiteHeaderFullChange(it.content.header),
                parsedToDbMapper.toAddressFullChange(it.content.mainAddress, FieldUpdateRaw.NoOp)
            )
        }

        return siteUpdateService.update(updateRequests)
    }
}
