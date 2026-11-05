/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.service.parser.task

import org.eclipse.tractusx.bpdm.common.model.parsedOrNull
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.model.error.ScriptVariantCoverageParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.GoldenRecordTaskReferencesParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.task.BpnReferenceRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.GoldenRecordRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.SiteUpsertRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.UpsertIntent
import org.eclipse.tractusx.bpdm.pool.service.parser.ScriptVariantCoverageValidator
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressBpnParser
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressPartnerScriptCodeReader
import org.springframework.stereotype.Service

/**
 * Reports every script variant coverage a golden record task would take away from a business partner it does not write.
 *
 * A task writes its legal entity, its site and their addresses in several operations, and one address is written twice
 * when a site's main address is the legal address, so each node is parsed without the coverage check and coverage is
 * judged here, once, for the task as a whole.
 */
@Service
class GoldenRecordTaskCoverageValidator(
    private val addressBpnParser: AddressBpnParser,
    private val partnerReader: AddressPartnerScriptCodeReader,
    private val scriptVariantCoverageValidator: ScriptVariantCoverageValidator
) {

    /**
     * Reports every coverage [goldenRecord] would take away from a business partner outside the task.
     */
    fun validate(goldenRecord: GoldenRecordRaw, references: GoldenRecordTaskReferencesParsed): List<ScriptVariantCoverageParseError> {
        val rewrittenBpns = rewrittenPartnerBpns(goldenRecord, references)

        return writtenAddresses(goldenRecord, references).flatMap { (address, coveredScriptCodes) ->
            scriptVariantCoverageValidator.check(coveredScriptCodes, partnerReader.storedPartners(address, rewrittenBpns))
        }
    }

    // An address the task does not write keeps the coverage it has, and a reference naming no BPN yet becomes a new address
    // no partner can be named on, so neither can strand anyone.
    private fun writtenAddresses(
        goldenRecord: GoldenRecordRaw,
        references: GoldenRecordTaskReferencesParsed
    ): List<Pair<LogisticAddressDb, List<String>>> {
        val legalEntity = goldenRecord.legalEntity
        val site = goldenRecord.site?.takeIf { it.intent == UpsertIntent.AlwaysWrite }
        val legalEntityScriptCodes = legalEntity.header.scriptVariants.map { it.scriptCode }
        val siteScriptCodes = site?.header?.scriptVariants?.map { it.scriptCode }.orEmpty()

        // A site whose main address is the legal address is covered by that one address, so both partners' script codes
        // end up on it.
        val siteSharesLegalAddress = site is SiteUpsertRaw.WithLegalAddressAsMain
        val legalAddress = legalEntity.legalAddress.reference
            .takeIf { legalEntity.intent == UpsertIntent.AlwaysWrite || siteSharesLegalAddress }
            ?.let { findAddress(it, references) }
        val legalAddressScriptCodes =
            if (siteSharesLegalAddress) legalEntityScriptCodes.plus(siteScriptCodes).distinct() else legalEntityScriptCodes

        val siteMainAddress = (site as? SiteUpsertRaw.WithOwnMainAddress)?.let { findAddress(it.mainAddress.reference, references) }

        return listOfNotNull(
            legalAddress?.let { it to legalAddressScriptCodes },
            siteMainAddress?.let { it to siteScriptCodes }
        )
    }

    private fun rewrittenPartnerBpns(goldenRecord: GoldenRecordRaw, references: GoldenRecordTaskReferencesParsed): Set<String> {
        val legalEntityBpn = goldenRecord.legalEntity
            .takeIf { it.intent == UpsertIntent.AlwaysWrite }
            ?.let { references.bpnOf(it.reference) }
        val siteBpn = goldenRecord.site
            ?.takeIf { it.intent == UpsertIntent.AlwaysWrite }
            ?.let { references.bpnOf(it.reference) }

        return setOfNotNull(legalEntityBpn, siteBpn)
    }

    private fun findAddress(reference: BpnReferenceRaw, references: GoldenRecordTaskReferencesParsed): LogisticAddressDb? =
        references.bpnOf(reference)?.let { addressBpnParser.parse(listOf(it)).single().parsedOrNull() }
}
