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

import io.cucumber.java.Scenario
import org.eclipse.tractusx.bpdm.gate.api.model.RelationOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutEntry
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.SiteVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartner
import java.time.Instant
import java.time.temporal.ChronoUnit

class ScenarioContext(val scenarioName: String, val scenarioSuffix: String, timeSuffix: Instant, val scenario: Scenario) {

    companion object {
        private val threadLocal = ThreadLocal<ScenarioContext?>()

        fun current(): ScenarioContext? = threadLocal.get()
        fun set(context: ScenarioContext) = threadLocal.set(context)
        fun clear() = threadLocal.remove()
    }

    private val timeSuffix = timeSuffix.truncatedTo(ChronoUnit.SECONDS)

    val siteLegalEntities: MutableMap<String, SiteBasedLegalEntity> = mutableMapOf()
    val legalEntities: MutableMap<String, LegalEntityWithLegalAddressVerboseDto> = mutableMapOf()
    val sites: MutableMap<String, SiteWithParent> = mutableMapOf()
    val additionalSiteAddresses: MutableMap<String, AdditionalSiteAddressWithParent> = mutableMapOf()
    val additionalLegalEntityAddresses: MutableMap<String, AdditionalLegalEntityAddressWithParent> = mutableMapOf()
    val taskData: MutableMap<String, BusinessPartner> = mutableMapOf()
    val inputData: MutableMap<String, BusinessPartnerInputRequest> = mutableMapOf()
    val outputData: MutableMap<String, BusinessPartnerOutputDto> = mutableMapOf()
    val relationInputData: MutableMap<String, RelationPutEntry> = mutableMapOf()
    val relationOutputData: MutableMap<String, RelationOutputContext> = mutableMapOf()
    val records: MutableMap<String, RecordState> = mutableMapOf()
    val relations: MutableMap<String, RelationState> = mutableMapOf()

    fun scenarioId() = "$scenarioSuffix-$timeSuffix"
    fun runId(id: String) = "$id-${scenarioId()}"
}

data class SiteBasedLegalEntity(
    val legalEntity: LegalEntityWithLegalAddressVerboseDto,
    val site: SiteVerboseDto
)

data class SiteWithParent(
    val legalEntity: LegalEntityWithLegalAddressVerboseDto,
    val site: SiteWithMainAddressVerboseDto
)

data class AdditionalSiteAddressWithParent(
    val siteWithParent: SiteWithParent,
    val address: LogisticAddressVerboseDto
)

data class AdditionalLegalEntityAddressWithParent(
    val legalEntity: LegalEntityWithLegalAddressVerboseDto,
    val address: LogisticAddressVerboseDto
)

data class RelationOutputContext(
    val outputDto: RelationOutputDto,
    val sourceExternalId: String,
    val targetExternalId: String
)

data class RecordState(
    val contentSeed: String? = null,
    val currentInput: BusinessPartnerInputRequest? = null,
    val currentTaskData: BusinessPartner? = null,
    val currentExpectedOutput: BusinessPartnerOutputDto? = null,
    val legalEntity: LegalEntityWithLegalAddressVerboseDto? = null
)

data class RelationState(
    val submittedEntry: RelationPutEntry,
    val sourceRecordId: String,
    val targetRecordId: String,
    val resolvedSourceBpn: String? = null,
    val resolvedTargetBpn: String? = null
)
