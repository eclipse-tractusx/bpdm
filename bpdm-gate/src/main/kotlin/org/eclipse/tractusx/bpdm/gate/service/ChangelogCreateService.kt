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

package org.eclipse.tractusx.bpdm.gate.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.common.util.joinIdentifiersForLog
import org.eclipse.tractusx.bpdm.gate.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntryDb
import org.eclipse.tractusx.bpdm.gate.entity.GoldenRecordType
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * The single authority for writing changelog entries of business partners and relations.
 *
 * An entry is written straight away but only reported once the transaction commits, so a write that is rolled back is
 * never reported as one.
 */
@Service
class ChangelogCreateService(
    private val changelogRepository: ChangelogRepository
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Writes the given changelog entry, reporting the change under the given identifier once the transaction commits.
     */
    fun record(entry: ChangelogEntryDb, identifier: String = entry.externalId) {
        changelogRepository.save(entry)
        buffer().add(RecordedChange(entry.goldenRecordType, entry.stage, entry.changelogType, identifier))
    }

    private fun buffer(): MutableList<RecordedChange> {
        openBuffer()?.let { return it }

        check(TransactionSynchronizationManager.isActualTransactionActive()) {
            "Changelog entries can only be recorded within an active transaction"
        }

        val buffer = mutableListOf<RecordedChange>()

        TransactionSynchronizationManager.bindSynchronizedResource(this, buffer)
        TransactionSynchronizationManager.registerSynchronization(ChangeReport(buffer))

        return buffer
    }

    @Suppress("UNCHECKED_CAST")
    private fun openBuffer(): MutableList<RecordedChange>? =
        TransactionSynchronizationManager.getResource(this) as MutableList<RecordedChange>?

    private fun summarize(changes: List<RecordedChange>): String {
        val subject = changes.first()
        val verb = when (subject.changelogType) {
            ChangelogType.CREATE -> "Created"
            ChangelogType.UPDATE -> "Updated"
        }
        val noun = when (subject.goldenRecordType) {
            GoldenRecordType.BusinessPartner -> "business partner"
            GoldenRecordType.Relation -> "relation"
        }
        val stage = when (subject.stage) {
            StageType.Input -> "input"
            StageType.Output -> "output"
        }
        val plural = if (changes.size > 1) "s" else ""

        return "$verb ${changes.size} $noun $stage$plural: ${changes.map { it.identifier }.joinIdentifiersForLog()}"
    }

    private data class RecordedChange(
        val goldenRecordType: GoldenRecordType,
        val stage: StageType,
        val changelogType: ChangelogType,
        val identifier: String
    )

    private inner class ChangeReport(
        private val recorded: List<RecordedChange>
    ) : TransactionSynchronization {

        override fun afterCompletion(status: Int) {
            if (status != TransactionSynchronization.STATUS_COMMITTED) {
                logger.debug { "Discarded ${recorded.size} changelog entries: transaction did not commit" }
                return
            }

            recorded.groupBy { Triple(it.goldenRecordType, it.stage, it.changelogType) }
                .forEach { (_, changes) -> logger.info { summarize(changes) } }
            recorded.forEach {
                logger.debug { "Created ${it.changelogType} changelog entry for ${it.goldenRecordType} ${it.stage} ${it.identifier}" }
            }
        }
    }
}
