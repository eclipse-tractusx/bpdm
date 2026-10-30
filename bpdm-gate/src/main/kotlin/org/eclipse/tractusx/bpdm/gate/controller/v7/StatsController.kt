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

package org.eclipse.tractusx.bpdm.gate.controller.v7

import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.v7.StatsApi
import org.eclipse.tractusx.bpdm.gate.api.v7.model.response.StatsAddressTypesResponse
import org.eclipse.tractusx.bpdm.gate.api.v7.model.response.StatsConfidenceCriteriaResponse
import org.eclipse.tractusx.bpdm.gate.api.v7.model.response.StatsSharingStatesResponse
import org.eclipse.tractusx.bpdm.gate.api.v7.model.response.StatsStagesResponse
import org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.gate.mapper.apiv7.ApiV7StatsMapper
import org.eclipse.tractusx.bpdm.gate.service.application.StatsReadApplicationService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class StatsController(
    private val statsReadApplicationService: StatsReadApplicationService,
    private val apiV7StatsMapper: ApiV7StatsMapper
) : StatsApi {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_STATS})")
    override fun countPartnersBySharingState(): StatsSharingStatesResponse {
        return apiV7StatsMapper.toDto(statsReadApplicationService.countSharingStates())
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_STATS})")
    override fun countPartnersPerStage(): StatsStagesResponse {
        return apiV7StatsMapper.toDto(statsReadApplicationService.countStages())
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_STATS})")
    override fun countAddressTypes(stage: StageType): StatsAddressTypesResponse {
        return apiV7StatsMapper.toDto(statsReadApplicationService.countAddressTypes(stage))
    }

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_STATS})")
    override fun getConfidenceCriteriaStats(): StatsConfidenceCriteriaResponse {
        return apiV7StatsMapper.toDto(statsReadApplicationService.summarizeConfidenceCriteria())
    }
}