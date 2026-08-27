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
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutEntry
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.SiteVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
import org.eclipse.tractusx.orchestrator.api.model.AdditionalSite
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

    val legalEntities: MutableMap<String, LegalEntityWithLegalAddressVerboseDto> = mutableMapOf()
    val sites: MutableMap<String, SiteWithParent> = mutableMapOf()
    val additionalSiteAddresses: MutableMap<String, AdditionalSiteAddressWithParent> = mutableMapOf()
    val additionalLegalEntityAddresses: MutableMap<String, AdditionalLegalEntityAddressWithParent> = mutableMapOf()
    // BPNA of an address remembered under the label a scenario refers to it by, so an assertion can name a
    // specific address (e.g. the relocation source/target) independent of how the record was refined.
    val addressBpnByLabel: MutableMap<String, String> = mutableMapOf()
    // The site each record puts on the address it is refined to, keyed by that address's BPN request identifier and
    // then by record. This is the ledger of the shared stream a refinement service has to keep to hand the Pool the
    // complete set of sites of an address; the tester stands in for that service.
    val sitesByAddressReference: MutableMap<String, MutableMap<String, AdditionalSite>> = mutableMapOf()
    val records: MutableMap<String, RecordState> = mutableMapOf()
    val relations: MutableMap<String, RelationState> = mutableMapOf()

    fun scenarioId() = "$scenarioSuffix-$timeSuffix"
    fun runId(id: String) = "$id-${scenarioId()}"

    /** Returns the sharing member that shared the record, whose Gate every later step on it acts through. */
    fun memberOf(recordId: String): SharingMember =
        records[recordId]?.member ?: error("record '$recordId' must be shared by an earlier step")
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

data class RecordState(
    val member: SharingMember,
    val contentSeed: String? = null,
    val currentInput: BusinessPartnerInputRequest? = null,
    val legalEntity: LegalEntityWithLegalAddressVerboseDto? = null,
    val poolSite: SiteWithMainAddressVerboseDto? = null,
    val poolAddress: LogisticAddressVerboseDto? = null,
)

data class RelationState(
    val submittedEntry: RelationPutEntry,
    val sourceRecordId: String,
    val targetRecordId: String,
    val resolvedSourceBpn: String? = null,
    val resolvedTargetBpn: String? = null
)
