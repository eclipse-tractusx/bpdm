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

package org.eclipse.tractusx.bpdm.gate.controller.v6

import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.v6.StatsV6Api
import org.eclipse.tractusx.bpdm.gate.api.v6.model.stats.StatsAddressTypesV6Response
import org.eclipse.tractusx.bpdm.gate.api.v6.model.stats.StatsConfidenceCriteriaV6Response
import org.eclipse.tractusx.bpdm.gate.api.v6.model.stats.StatsSharingStatesV6Response
import org.eclipse.tractusx.bpdm.gate.api.v6.model.stats.StatsStagesV6Response
import org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.gate.mapper.apiv6.ApiV6StatsMapper
import org.eclipse.tractusx.bpdm.gate.service.application.StatsReadApplicationService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController("StatsControllerLegacy")
class StatsController(
    private val statsReadApplicationService: StatsReadApplicationService,
    private val apiV6StatsMapper: ApiV6StatsMapper
) : StatsV6Api {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_STATS})")
    override fun countPartnersBySharingState(): StatsSharingStatesV6Response {
        return apiV6StatsMapper.toDto(statsReadApplicationService.countSharingStates())
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_STATS})")
    override fun countPartnersPerStage(): StatsStagesV6Response {
        return apiV6StatsMapper.toDto(statsReadApplicationService.countStages())
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_STATS})")
    override fun countAddressTypes(stage: StageType): StatsAddressTypesV6Response {
        return apiV6StatsMapper.toDto(statsReadApplicationService.countAddressTypes(stage))
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_STATS})")
    override fun getConfidenceCriteriaStats(): StatsConfidenceCriteriaV6Response {
        return apiV6StatsMapper.toDto(statsReadApplicationService.summarizeConfidenceCriteria())
    }
}