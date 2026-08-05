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

package org.eclipse.tractusx.bpdm.pool.service

import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.model.error.ScriptVariantCoverageParseError
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.service.parser.AddressPartnerScriptCodeReader
import org.eclipse.tractusx.bpdm.pool.service.parser.ScriptVariantCoverageValidator
import org.eclipse.tractusx.orchestrator.api.model.BpnReference
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartner
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Decides the script variant coverage a single golden record task upsert cannot decide on its own: whether the task
 * leaves a business partner it does not write named in a script its address no longer covers.
 *
 * A task writes its legal entity, its site and their addresses in several operations, and one address can be written
 * twice when a site's main address is the legal address, so every upsert sees a half-written state. The upserts
 * therefore parse without the coverage check and coverage is judged here, once, for the task as a whole.
 */
@Service
class TaskScriptVariantCoverageValidator(
    private val logisticAddressRepository: LogisticAddressRepository,
    private val partnerReader: AddressPartnerScriptCodeReader,
    private val scriptVariantCoverageValidator: ScriptVariantCoverageValidator
) {

    /**
     * Reports every coverage [businessPartner] would take away from a business partner outside this task.
     */
    @Transactional(readOnly = true)
    fun validate(
        businessPartner: BusinessPartner,
        taskEntryBpnMapping: TaskEntryBpnMapping
    ): List<ScriptVariantCoverageParseError> {
        val rewrittenBpns = rewrittenPartnerBpns(businessPartner, taskEntryBpnMapping)

        return writtenAddresses(businessPartner, taskEntryBpnMapping).flatMap { (address, coveredScriptCodes) ->
            scriptVariantCoverageValidator.check(coveredScriptCodes, partnerReader.storedPartners(address, rewrittenBpns))
        }
    }

    /**
     * The already persisted addresses this task writes, each with the script codes it will cover afterwards. An address
     * the task does not write keeps the coverage it has, and a reference that resolves to no BPN yet becomes a new
     * address no partner can be named on — neither can strand anyone, so neither appears here.
     */
    private fun writtenAddresses(
        businessPartner: BusinessPartner,
        taskEntryBpnMapping: TaskEntryBpnMapping
    ): List<Pair<LogisticAddressDb, List<String>>> {
        val site = businessPartner.site?.takeIf { it.hasChanged != false }
        val legalEntityWritten = businessPartner.legalEntity.hasChanged != false
        val legalEntityScriptCodes = businessPartner.legalEntity.scriptVariants.map { it.scriptCode }
        val siteScriptCodes = site?.scriptVariants?.map { it.scriptCode }.orEmpty()

        // A site whose main address is the legal address is covered by that one address, so both partners' script codes
        // end up on it - the task writes their union there (see TaskStepBuildService.legalAddressCoverageNotStatedBy).
        val siteSharesLegalAddress = site != null && site.siteMainIsLegalAddress
        val legalAddress = businessPartner.legalEntity.legalAddress.bpnReference
            .takeIf { legalEntityWritten || siteSharesLegalAddress }
            ?.let { resolveAddress(it, taskEntryBpnMapping) }
        val legalAddressScriptCodes =
            if (siteSharesLegalAddress) legalEntityScriptCodes.plus(siteScriptCodes).distinct() else legalEntityScriptCodes

        val siteMainAddress = site?.siteMainAddress?.let { resolveAddress(it.bpnReference, taskEntryBpnMapping) }

        return listOfNotNull(
            legalAddress?.let { it to legalAddressScriptCodes },
            siteMainAddress?.let { it to siteScriptCodes }
        )
    }

    /**
     * The BPNs of the business partners this task writes itself; a partner reported as unchanged is not among them, so
     * the coverage it has today has to survive the task.
     */
    private fun rewrittenPartnerBpns(businessPartner: BusinessPartner, taskEntryBpnMapping: TaskEntryBpnMapping): Set<String> {
        val legalEntityBpn = businessPartner.legalEntity
            .takeIf { it.hasChanged != false }
            ?.let { taskEntryBpnMapping.getBpn(it.bpnReference) }
        val siteBpn = businessPartner.site
            ?.takeIf { it.hasChanged != false }
            ?.let { taskEntryBpnMapping.getBpn(it.bpnReference) }

        return setOfNotNull(legalEntityBpn, siteBpn)
    }

    private fun resolveAddress(bpnReference: BpnReference, taskEntryBpnMapping: TaskEntryBpnMapping): LogisticAddressDb? =
        taskEntryBpnMapping.getBpn(bpnReference)?.let { logisticAddressRepository.findByBpn(it) }
}
