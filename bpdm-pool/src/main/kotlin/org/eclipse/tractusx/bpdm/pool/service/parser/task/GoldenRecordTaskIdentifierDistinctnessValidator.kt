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

import org.eclipse.tractusx.bpdm.pool.model.error.AddressConstraintParseError
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.AdditionalAddressUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.GoldenRecordTaskError
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.LegalEntityUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.task.SiteUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.task.GoldenRecordRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.task.SiteUpsertRaw
import org.eclipse.tractusx.bpdm.pool.model.request.LogisticAddressRequest
import org.springframework.stereotype.Service

/**
 * Reports an address identifier a golden record task states on two of the addresses it writes.
 *
 * The node parsers check identifiers against the database, which does not hold yet what the task writes, so an address
 * written later in the task is checked here against the identifiers of the addresses written before it.
 */
@Service
class GoldenRecordTaskIdentifierDistinctnessValidator {

    /**
     * Reports every identifier the site main address or the additional address of [goldenRecord] shares with an address
     * the task writes earlier; a node that could not be parsed is left out.
     */
    fun validate(
        goldenRecord: GoldenRecordRaw,
        legalEntity: LegalEntityUpsertParsed?,
        site: SiteUpsertParsed?,
        additionalAddress: AdditionalAddressUpsertParsed?
    ): List<GoldenRecordTaskError> {
        val legalAddress = when (legalEntity) {
            is LegalEntityUpsertParsed.Create -> WrittenAddress(target = null, goldenRecord.legalEntity.legalAddress.content)
            is LegalEntityUpsertParsed.Update -> WrittenAddress(legalEntity.update.target.legalAddress.bpn, goldenRecord.legalEntity.legalAddress.content)
            is LegalEntityUpsertParsed.Unchanged, null -> null
        }
        val siteMainContent = (goldenRecord.site as? SiteUpsertRaw.WithOwnMainAddress)?.mainAddress?.content
        val siteMainAddress = siteMainContent?.let { content ->
            when (site) {
                is SiteUpsertParsed.CreateWithOwnMainAddress -> WrittenAddress(target = null, content)
                is SiteUpsertParsed.CreateOnExistingAddress -> WrittenAddress(site.create.mainAddress.bpn, content)
                is SiteUpsertParsed.Update -> WrittenAddress(site.update.target.mainAddress.bpn, content)
                is SiteUpsertParsed.CreateOnLegalAddress, is SiteUpsertParsed.Unchanged, null -> null
            }
        }
        val additional = additionalAddress?.let { WrittenAddress(it.existingAddress?.bpn, goldenRecord.additionalAddress!!.content) }

        val siteMainCollisions = siteMainAddress?.collisionsWith(listOfNotNull(legalAddress)).orEmpty()
        val additionalCollisions = additional?.collisionsWith(listOfNotNull(legalAddress, siteMainAddress)).orEmpty()

        return siteMainCollisions.map {
            if (site is SiteUpsertParsed.Update) GoldenRecordTaskError.SiteUpdateInvalid(it) else GoldenRecordTaskError.SiteCreateInvalid(it)
        } + additionalCollisions.map {
            if (additionalAddress is AdditionalAddressUpsertParsed.Update) GoldenRecordTaskError.AdditionalAddressUpdateInvalid(it)
            else GoldenRecordTaskError.AdditionalAddressCreateInvalid(it)
        }
    }

    private data class WrittenAddress(val target: String?, val content: LogisticAddressRequest) {

        fun collisionsWith(earlier: List<WrittenAddress>): List<AddressConstraintParseError.DuplicateIdentifier> {
            val takenIdentifiers = earlier
                .filter { it.target == null || it.target != target }
                .flatMap { it.content.identifiers }
                .mapNotNull { identifier -> identifier.type?.let { type -> identifier.value?.let { type to it } } }
                .toSet()

            return content.identifiers.mapIndexedNotNull { index, identifier ->
                val type = identifier.type ?: return@mapIndexedNotNull null
                val value = identifier.value ?: return@mapIndexedNotNull null
                AddressConstraintParseError.DuplicateIdentifier(index, type, value).takeIf { (type to value) in takenIdentifiers }
            }
        }
    }
}
