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

package org.eclipse.tractusx.bpdm.gate.api.v6.client

import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.ApiCommons
import org.eclipse.tractusx.bpdm.gate.api.v6.StatsV6Api
import org.eclipse.tractusx.bpdm.gate.api.v6.model.stats.StatsAddressTypesV6Response
import org.eclipse.tractusx.bpdm.gate.api.v6.model.stats.StatsConfidenceCriteriaV6Response
import org.eclipse.tractusx.bpdm.gate.api.v6.model.stats.StatsSharingStatesV6Response
import org.eclipse.tractusx.bpdm.gate.api.v6.model.stats.StatsStagesV6Response
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.service.annotation.GetExchange

interface StatsV6ApiClient: StatsV6Api {
    @GetExchange(value = "${ApiCommons.STATS_PATH_V6}/sharing-states")
    override fun countPartnersBySharingState(): StatsSharingStatesV6Response

    @GetExchange(value = "${ApiCommons.STATS_PATH_V6}/stages")
    override fun countPartnersPerStage(): StatsStagesV6Response

    @GetExchange(value = "${ApiCommons.STATS_PATH_V6}/{stage}/address-types")
    override fun countAddressTypes(@PathVariable("stage") stage: StageType): StatsAddressTypesV6Response

    @GetExchange(value = "${ApiCommons.STATS_PATH_V6}/confidence-criteria")
    override fun getConfidenceCriteriaStats(): StatsConfidenceCriteriaV6Response
}