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


package org.eclipse.tractusx.bpdm.pool.service.parser.task

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.BpnReferenceAllocation
import org.eclipse.tractusx.bpdm.pool.model.error.GoldenRecordUpsertParseError
import org.eclipse.tractusx.bpdm.pool.model.error.SiteNotFound
import org.eclipse.tractusx.bpdm.pool.model.parsed.ResolvedReference
import org.eclipse.tractusx.bpdm.pool.model.request.BpnReferenceRequest
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteBpnParser
import org.springframework.stereotype.Service

/**
 * Turns a site's stated BPN reference into the site it names.
 */
@Service
class SiteReferenceParser(
    private val referenceResolutionParser: BpnReferenceResolutionParser,
    private val siteBpnParser: SiteBpnParser
) {

    /**
     * Reports the reference together with the site it names, or a rejection where it names none that exists.
     */
    fun parse(
        reference: BpnReferenceRequest,
        bpnReferences: BpnReferenceAllocation
    ): ParseResult<ResolvedReference<SiteDb>, GoldenRecordUpsertParseError> =
        referenceResolutionParser.parse(
            reference,
            bpnReferences,
            { bpn -> (siteBpnParser.parse(listOf(bpn)).single() as? ParseResult.Success)?.parsed },
            ::SiteNotFound
        )
}
