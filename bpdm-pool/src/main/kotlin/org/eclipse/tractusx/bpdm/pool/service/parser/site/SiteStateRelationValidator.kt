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
package org.eclipse.tractusx.bpdm.pool.service.parser.site

import org.eclipse.tractusx.bpdm.pool.entity.SiteRelationDb
import org.eclipse.tractusx.bpdm.pool.model.PartnerActivityTimeline
import org.eclipse.tractusx.bpdm.pool.model.error.SiteStateRelationParseError
import org.eclipse.tractusx.bpdm.pool.model.request.SiteHeaderRequest
import org.eclipse.tractusx.bpdm.pool.model.toActivityTimeline
import org.eclipse.tractusx.bpdm.pool.repository.SiteRelationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Checks the states a site write states against the successions the site already takes part in.
 */
@Service
class SiteStateRelationValidator(
    private val siteRelationRepository: SiteRelationRepository
) {

    /**
     * Reports, per header, each succession the states it states would contradict. [siteBpns] is positional with
     * [headers]: null for a site being created, which takes part in no succession yet.
     */
    @Transactional(readOnly = true)
    fun validate(headers: List<SiteHeaderRequest>, siteBpns: List<String?>): List<List<SiteStateRelationParseError>> {
        val statedSiteBpns = siteBpns.filterNotNull()
        val relations =
            if (statedSiteBpns.isEmpty()) emptySet()
            else siteRelationRepository.findByStartSiteBpnInOrEndSiteBpnIn(statedSiteBpns)

        return headers.zip(siteBpns) { header, bpn ->
            if (bpn == null) return@zip emptyList()
            val activity = header.states.toActivityTimeline()
            relations
                .filter { it.startSite.bpn == bpn || it.endSite.bpn == bpn }
                .flatMap { validate(bpn, activity, it) }
        }
    }

    private fun validate(bpn: String, activity: PartnerActivityTimeline, relation: SiteRelationDb): List<SiteStateRelationParseError> =
        relation.validityPeriods.sortedBy { it.validFrom }.flatMap { validityPeriod ->
            val validFrom = validityPeriod.validFrom
            listOfNotNull(
                if (relation.startSite.bpn == bpn && activity.isRecordedActiveOnceReplacedFrom(validFrom))
                    SiteStateRelationParseError.ReplacedSiteRecordedActive(bpn, relation.endSite.bpn, validFrom) else null,
                if (relation.endSite.bpn == bpn && activity.isRecordedInactiveWhenReplacingFrom(validFrom))
                    SiteStateRelationParseError.ReplacingSiteRecordedInactive(bpn, relation.startSite.bpn, validFrom) else null
            )
        }
}
