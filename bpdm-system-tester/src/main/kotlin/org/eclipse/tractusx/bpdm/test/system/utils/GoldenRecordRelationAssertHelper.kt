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
import org.eclipse.tractusx.bpdm.gate.api.model.AddressGoldenRecordRelationTypeDto
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGoldenRecordRelationTypeDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import tools.jackson.databind.json.JsonMapper

/**
 * Asserts that a record's Gate output reflects an established golden record relation.
 *
 * A relation is written into the Pool by the relation golden record process and then propagates into the
 * (already shared) business partner outputs through the Gate's "update output on golden record change" batch.
 * The establish step waits for the relation's completed sharing state, and the [SharingStateWatcher] only
 * reports completion after a short sync delay, which is enough for that propagation to land - so the output
 * is read once here rather than polled.
 *
 * The relation is matched by its type and the unordered pair of source/target BPNs taken from the resolved
 * relation. Comparing the BPN pair as a set keeps the assertion independent of the relation's direction,
 * which also makes it robust against the address-type swap an IsReplacedBy relation triggers.
 */
class GoldenRecordRelationAssertHelper(
    private val gateClient: GateClient,
    private val jsonMapper: JsonMapper
) {

    private val context: ScenarioContext get() = ScenarioContext.current()!!

    /**
     * Asserts the [recordId] output's legal entity reflects [relation] in its golden record relations. When
     * [expectedParentBpnl] is given, also asserts the output's legal entity BPN is that parent - proving an
     * additional address record surfaces the relation through its parent legal entity.
     */
    fun assertLegalEntityRelationReflected(recordId: String, relation: RelationState, expectedParentBpnl: String? = null) {
        val expectedType = LegalEntityGoldenRecordRelationTypeDto.valueOf(toGateType(relation).name)
        val expectedBpns = resolvedBpnPair(relation)

        val output = fetchOutput(recordId)

        assertThat(output.legalEntity.goldenRecordRelations.map { setOf(it.sourceBpn, it.targetBpn) to it.relationType })
            .describedAs("legal entity output of '%s' must reflect a %s relation between BPNs %s", recordId, expectedType, expectedBpns)
            .contains(expectedBpns to expectedType)

        if (expectedParentBpnl != null) {
            assertThat(output.legalEntity.legalEntityBpn)
                .describedAs("output legal entity BPN of '%s' must be its parent legal entity", recordId)
                .isEqualTo(expectedParentBpnl)
        }
    }

    /**
     * Asserts the [recordId] output's address reflects [relation] in its golden record relations.
     */
    fun assertAddressRelationReflected(recordId: String, relation: RelationState) {
        val expectedType = AddressGoldenRecordRelationTypeDto.valueOf(toGateType(relation).name)
        val expectedBpns = resolvedBpnPair(relation)

        val output = fetchOutput(recordId)

        assertThat(output.address.goldenRecordRelations.map { setOf(it.sourceBpn, it.targetBpn) to it.relationType })
            .describedAs("address output of '%s' must reflect a %s relation between BPNs %s", recordId, expectedType, expectedBpns)
            .contains(expectedBpns to expectedType)
    }

    private fun toGateType(relation: RelationState): RelationType = relation.submittedEntry.relationType

    private fun resolvedBpnPair(relation: RelationState): Set<String> = setOf(
        relation.resolvedSourceBpn ?: error("relation '${relation.submittedEntry.externalId}' has not been established yet"),
        relation.resolvedTargetBpn ?: error("relation '${relation.submittedEntry.externalId}' has not been established yet")
    )

    private fun fetchOutput(recordId: String): BusinessPartnerOutputDto {
        val runId = context.runId(recordId)
        val outputPage = gateClient.businessParters.getBusinessPartnersOutput(listOf(runId))
        attachApiCall("POST", "/v7/output/business-partners/search", listOf(runId), outputPage)
        return outputPage.content.single()
    }

    private fun attachApiCall(method: String, path: String, request: Any? = null, response: Any? = null) {
        val content = buildMap {
            put("uri", "$method $path")
            if (request != null) put("request", request)
            if (response != null) put("response", response)
        }
        context.scenario.attach(
            jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(content),
            "application/json",
            "$method $path"
        )
    }
}
