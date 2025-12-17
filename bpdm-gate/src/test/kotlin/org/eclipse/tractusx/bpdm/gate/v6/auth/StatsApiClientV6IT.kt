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

package org.eclipse.tractusx.bpdm.gate.v6.auth

import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.api.model.response.StatsAddressTypesResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.StatsConfidenceCriteriaResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.StatsSharingStatesResponse
import org.eclipse.tractusx.bpdm.gate.api.model.response.StatsStagesResponse
import org.eclipse.tractusx.bpdm.gate.api.v6.client.StatsApiClientV6
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType
import org.junit.jupiter.api.Test

class StatsApiClientV6IT: GateAuthV6Test(), StatsApiClientV6 {

    @Test
    fun testCountPartnersBySharingState(){
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Authorized,
            outputConsumer = AuthExpectationType.Authorized
        ){
            countPartnersBySharingState()
        }
    }

    @Test
    fun testCountPartnersPerStage(){
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Authorized,
            outputConsumer = AuthExpectationType.Authorized
        ){
            countPartnersPerStage()
        }
    }

    @Test
    fun testCountAddressTypes(){
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Authorized,
            outputConsumer = AuthExpectationType.Authorized
        ){
            countAddressTypes(StageType.Input)
        }
    }

    @Test
    fun testGetConfidenceCriteriaStats(){
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Authorized,
            outputConsumer = AuthExpectationType.Authorized
        ){
            getConfidenceCriteriaStats()
        }
    }

    override fun countPartnersBySharingState(): StatsSharingStatesResponse {
        return gateClient.stats.countPartnersBySharingState()
    }

    override fun countPartnersPerStage(): StatsStagesResponse {
        return gateClient.stats.countPartnersPerStage()
    }

    override fun countAddressTypes(stage: StageType): StatsAddressTypesResponse {
       return gateClient.stats.countAddressTypes(stage)
    }

    override fun getConfidenceCriteriaStats(): StatsConfidenceCriteriaResponse {
        return gateClient.stats.getConfidenceCriteriaStats()
    }
}