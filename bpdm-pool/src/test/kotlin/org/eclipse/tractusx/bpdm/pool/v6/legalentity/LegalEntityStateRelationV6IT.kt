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

package org.eclipse.tractusx.bpdm.pool.v6.legalentity

import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalEntityStateDtoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.ErrorInfoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityPartnerUpdateResponseWrapperV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.response.LegalEntityUpdateErrorV6
import org.eclipse.tractusx.bpdm.pool.v6.UnscheduledPoolTestBaseV6AndV7
import org.eclipse.tractusx.bpdm.test.testdata.pool.v7.TestDataV7
import org.junit.jupiter.api.Test

/**
 * A state change over the v6 API is judged against the partner's relations exactly as over v7.
 */
class LegalEntityStateRelationV6IT : UnscheduledPoolTestBaseV6AndV7() {

    /**
     * GIVEN a legal entity owning another
     * WHEN operator records the owner as inactive over the v6 API while the ownership holds
     * THEN the update is rejected with the ownership error code
     */
    @Test
    fun `try update owner to inactive while its ownership holds`() {
        //GIVEN
        val ownedBpn = testDataClientV7.createLegalEntity("$testName owned").header.bpnl
        val ownerBpn = testDataClientV7.createLegalEntity("$testName owner").header.bpnl
        testDataClientV7.createIsOwnedByRelation(ownedBpn, ownerBpn)

        //WHEN
        val request = testDataFactory.request.createLegalEntityUpdateRequest("Updated $testName", ownerBpn)
        val inactiveRequest = request.copy(
            legalEntity = request.legalEntity.copy(
                states = listOf(LegalEntityStateDtoV6(TestDataV7.currentStateValidFrom, null, BusinessStateType.INACTIVE))
            )
        )
        val response = poolClient.legalEntities.updateBusinessPartners(listOf(inactiveRequest))

        //THEN
        val expectedError = ErrorInfoV6(LegalEntityUpdateErrorV6.StatesContradictOwnership, "IGNORED", ownerBpn)
        val expectedResponse = LegalEntityPartnerUpdateResponseWrapperV6(emptyList(), listOf(expectedError))
        assertRepository.assertLegalEntityUpdate(response, expectedResponse)
    }
}
