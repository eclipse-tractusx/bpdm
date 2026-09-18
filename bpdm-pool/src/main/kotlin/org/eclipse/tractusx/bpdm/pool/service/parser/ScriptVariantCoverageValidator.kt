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

package org.eclipse.tractusx.bpdm.pool.service.parser

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.combine
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.model.AddressCoverageWrite
import org.eclipse.tractusx.bpdm.pool.model.PartnerScriptCodes
import org.eclipse.tractusx.bpdm.pool.model.error.ScriptVariantCoverageParseError
import org.eclipse.tractusx.bpdm.pool.model.error.ScriptVariantCoverageStillNeeded
import org.eclipse.tractusx.bpdm.pool.model.error.ScriptVariantNotCoveredByAddress
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressPartnerScriptCodeReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The rule that a business partner may only be named in a script its address is also written in, decided on the state
 * the whole write leaves behind.
 *
 * The rule is judged for a set of writes rather than for one of them, because several entries can write one address:
 * a partner another entry is about to rewrite must be judged on the script codes that entry states, not on the ones
 * it carries today.
 */
@Service
class ScriptVariantCoverageValidator(
    private val partnerReader: AddressPartnerScriptCodeReader
) {

    /**
     * Reports, per entry, every script code a business partner is named in that the address it is built on will not cover.
     */
    @Transactional(readOnly = true)
    fun validate(entries: List<List<AddressCoverageWrite>>): List<List<ScriptVariantCoverageParseError>> {
        val writesByAddress = entries.flatten().groupBy { writtenAddress(it)?.bpn }
        return entries.map { writes -> writes.flatMap { errorsFor(it, writesByAddress) }.distinct() }
    }

    /**
     * Reports each entry with the coverage its write takes away folded into it, leaving an entry that already failed
     * with the errors it has.
     */
    fun <T, E> applyTo(
        results: List<ParseResult<T, E>>,
        collectWrites: (T) -> List<AddressCoverageWrite>,
        toError: (ScriptVariantCoverageParseError) -> E
    ): List<ParseResult<T, E>> {
        val writes = results.map { result -> (result as? ParseResult.Success)?.parsed?.let(collectWrites).orEmpty() }
        return results.zip(validate(writes)) { result, errors -> result.combine(errors.map(toError)) { it } }
    }

    private fun errorsFor(
        write: AddressCoverageWrite,
        writesByAddress: Map<String?, List<AddressCoverageWrite>>
    ): List<ScriptVariantCoverageParseError> {
        val address = writtenAddress(write)
            ?: return check((write as AddressCoverageWrite.Created).scriptCodes, write.partners)

        val addressWrites = writesByAddress[address.bpn].orEmpty()
        // A write that leaves the address content alone can only strand the partner it adds: the ones already on the
        // address keep the coverage they have, however it stands today.
        val contentWrite = addressWrites.filterIsInstance<AddressCoverageWrite.Rewritten>().firstOrNull()
            ?: return check(address.scriptCodes(), write.partners)

        // Every write of this address states its partners anew, so they are judged on what they are about to become
        // and are left out of what is read from the database.
        val statedPartners = addressWrites.flatMap { it.partners }
        val storedPartners = partnerReader.storedPartners(address, statedPartners.mapNotNull { it.bpn }.toSet())

        return check(contentWrite.scriptCodes, statedPartners + storedPartners)
    }

    private fun writtenAddress(write: AddressCoverageWrite): LogisticAddressDb? =
        when (write) {
            is AddressCoverageWrite.Created -> null
            is AddressCoverageWrite.Rewritten -> write.address
            is AddressCoverageWrite.PartnerOnly -> write.address
        }

    private fun check(addressScriptCodes: Collection<String>, partners: List<PartnerScriptCodes>): List<ScriptVariantCoverageParseError> {
        val covered = addressScriptCodes.toSet()
        return partners.flatMap { partner ->
            partner.scriptCodes.filterNot { it in covered }.distinct().map { scriptCode ->
                if (partner.bpn == null) ScriptVariantNotCoveredByAddress(scriptCode)
                else ScriptVariantCoverageStillNeeded(scriptCode, partner.bpn)
            }
        }.distinct()
    }
}
