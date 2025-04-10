/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.api.client

import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.ApiCommons
import org.eclipse.tractusx.bpdm.gate.api.StatsApi
import org.eclipse.tractusx.bpdm.gate.api.model.response.StatsAddressTypesResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.StatsConfidenceCriteriaResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.StatsSharingStatesResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.StatsStagesResponse
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange
interface StatsApiClient: StatsApi {

    @GetExchange(value = "${ApiCommons.STATS_PATH_V7}/sharing-states")
    override fun countPartnersBySharingState(): StatsSharingStatesResponse

    @GetExchange(value = "${ApiCommons.STATS_PATH_V7}/stages")
    override fun countPartnersPerStage(): StatsStagesResponse

    @GetExchange(value = "${ApiCommons.STATS_PATH_V7}/{stage}/address-types")
    override fun countAddressTypes(@PathVariable("stage") stage: StageType): StatsAddressTypesResponse

    @GetExchange(value = "${ApiCommons.STATS_PATH_V7}/confidence-criteria")
    override fun getConfidenceCriteriaStats(): StatsConfidenceCriteriaResponse
}