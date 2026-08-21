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

package org.eclipse.tractusx.bpdm.test.system.utils

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.ConfidenceCriteriaDto

enum class OutputComponent { LEGAL_ENTITY, LEGAL_ADDRESS, ADDITIONAL_ADDRESS, SITE }

enum class ConfidenceLevel(val sharedByOwner: Boolean, val checkedByExternalDataSource: Boolean) {
    NO_CONFIDENCE(sharedByOwner = false, checkedByExternalDataSource = false),
    OWNER_SHARED(sharedByOwner = true,  checkedByExternalDataSource = false),
    VERIFIED(sharedByOwner = false, checkedByExternalDataSource = true),
    VERIFIED_OWNER_SHARED(sharedByOwner = true,  checkedByExternalDataSource = true)
}

class ConfidenceAssertHelper(
    private val gateClient: GateClient,
    private val apiCallEvidence: ApiCallEvidence
) {

    private val context: ScenarioContext get() = ScenarioContext.current()!!

    fun assertConfidence(recordId: String, component: OutputComponent, level: ConfidenceLevel) {
        val runId = context.runId(recordId)
        val outputPage = gateClient.businessParters.getBusinessPartnersOutput(listOf(runId))
        apiCallEvidence.attach("POST", "/v7/output/business-partners/search", listOf(runId), outputPage)
        val output = outputPage.content.single()

        val actual: ConfidenceCriteriaDto = when (component) {
            OutputComponent.LEGAL_ENTITY      -> output.legalEntity.confidenceCriteria
            OutputComponent.LEGAL_ADDRESS     -> output.address.confidenceCriteria
            OutputComponent.ADDITIONAL_ADDRESS -> output.address.confidenceCriteria
            OutputComponent.SITE              -> output.site!!.confidenceCriteria
        }

        assertThat(actual.sharedByOwner)
            .describedAs("$component of '$recordId': sharedByOwner")
            .isEqualTo(level.sharedByOwner)
        assertThat(actual.checkedByExternalDataSource)
            .describedAs("$component of '$recordId': checkedByExternalDataSource")
            .isEqualTo(level.checkedByExternalDataSource)
    }
}
