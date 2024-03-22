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

package org.eclipse.tractusx.bpdm.gate.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.StatsApi.Companion.STATS_PATH
import org.eclipse.tractusx.bpdm.gate.api.model.response.StatsAddressTypesResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.StatsConfidenceCriteriaResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.StatsSharingStatesResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.StatsStagesResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping(STATS_PATH, produces = [MediaType.APPLICATION_JSON_VALUE])
interface StatsApi {

    companion object{
        const val STATS_PATH = "${ApiCommons.BASE_PATH}/stats"
    }

    @Operation
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200")
        ]
    )
    @GetMapping("/sharing-states")
    fun countPartnersBySharingState(): StatsSharingStatesResponse

    @Operation
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200")
        ]
    )
    @GetMapping("/stages")
    fun countPartnersPerStage(): StatsStagesResponse

    @Operation
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200")
        ]
    )
    @GetMapping("/{stage}/address-types")
    fun countAddressTypes(@PathVariable("stage") stage: StageType): StatsAddressTypesResponse

    @Operation
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200")
        ]
    )
    @GetMapping("/confidence-criteria")
    fun getConfidenceCriteriaStats(): StatsConfidenceCriteriaResponse


}