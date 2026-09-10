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

package org.eclipse.tractusx.bpdm.pool.service.application.v6

import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.SiteWithMainAddressVerboseDtoV6
import org.eclipse.tractusx.bpdm.pool.mapper.poolv6.outbound.SiteResponseMapperV6
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteBpnParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The REST-API boundary for the legacy v6 "get site" operation, using the v6 response shape.
 */
@Service
class SiteGetApplicationV6Service(
    private val siteBpnParser: SiteBpnParser,
    private val siteResponseMapperV6: SiteResponseMapperV6
) {

    /**
     * Returns the site with the given BPN and fails with a not-found error when no site carries it.
     */
    @Transactional(readOnly = true)
    fun getSite(bpns: String): SiteWithMainAddressVerboseDtoV6 =
        when (val result = siteBpnParser.parse(listOf(bpns)).single()) {
            is ParseResult.Success -> siteResponseMapperV6.toSiteWithMainAddress(result.parsed)
            is ParseResult.Failure -> throw BpdmNotFoundException("Site", bpns)
        }
}
