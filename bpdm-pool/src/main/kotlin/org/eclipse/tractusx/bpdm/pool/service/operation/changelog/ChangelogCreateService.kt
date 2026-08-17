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

package org.eclipse.tractusx.bpdm.pool.service.operation.changelog

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.entity.PartnerChangelogEntryDb
import org.eclipse.tractusx.bpdm.pool.model.ChangelogRecord
import org.eclipse.tractusx.bpdm.pool.repository.PartnerChangelogEntryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * The single authority for writing changelog entries of business partners.
 *
 * Entries are buffered for the current transaction and written when it commits — at most one per business partner, in a
 * fixed order — so a caller controls neither when nor in what sequence it records them.
 */
@Service
class ChangelogCreateService(
    private val partnerChangelogEntryRepository: PartnerChangelogEntryRepository
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Records the given changelog entries for writing when the current transaction commits.
     */
    fun record(records: Collection<ChangelogRecord>) {
        buffer().addAll(records)
    }

    /**
     * Records a single changelog entry for writing when the current transaction commits.
     */
    fun record(record: ChangelogRecord) {
        buffer().add(record)
    }

    private fun buffer(): MutableList<ChangelogRecord> {
        openBuffer()?.let { return it }

        check(TransactionSynchronizationManager.isActualTransactionActive()) {
            "Changelog entries can only be recorded within an active transaction"
        }

        val buffer = mutableListOf<ChangelogRecord>()
        val origin = TransactionSynchronizationManager.getCurrentTransactionName() ?: UNNAMED_TRANSACTION

        // The synchronized variant unbinds the buffer once the transaction completes and suspends it around a nested
        // transaction, neither of which a plain resource binding does.
        TransactionSynchronizationManager.bindSynchronizedResource(this, buffer)
        TransactionSynchronizationManager.registerSynchronization(ChangelogFlush(buffer, origin))

        return buffer
    }

    @Suppress("UNCHECKED_CAST")
    private fun openBuffer(): MutableList<ChangelogRecord>? =
        TransactionSynchronizationManager.getResource(this) as MutableList<ChangelogRecord>?

    private fun ChangelogRecord.toEntity(): PartnerChangelogEntryDb =
        PartnerChangelogEntryDb(bpn, businessPartnerType, changelogType)

    private fun summarize(emitted: List<ChangelogRecord>): String =
        emitted.groupingBy { it.businessPartnerType to it.changelogType }
            .eachCount()
            .entries
            .joinToString(", ") { (partnerAndChange, count) -> "$count ${partnerAndChange.first} ${partnerAndChange.second}" }

    private inner class ChangelogFlush(
        private val recorded: List<ChangelogRecord>,
        private val origin: String
    ) : TransactionSynchronization {

        private var plan: ChangelogEmissionPlan? = null

        override fun beforeCommit(readOnly: Boolean) {
            val emissionPlan = ChangelogEmissionPolicy.plan(recorded)
            partnerChangelogEntryRepository.saveAll(emissionPlan.emitted.map { it.toEntity() })
            plan = emissionPlan
        }

        override fun afterCompletion(status: Int) {
            val writtenPlan = plan
            if (status != TransactionSynchronization.STATUS_COMMITTED || writtenPlan == null) {
                logger.info { "Discarded ${recorded.size} changelog records of '$origin': transaction did not commit" }
                return
            }

            logger.info { "Created ${writtenPlan.emitted.size} changelog entries of '$origin': ${summarize(writtenPlan.emitted)}" }
            writtenPlan.emitted.forEach {
                logger.debug { "Created ${it.changelogType} changelog entry for ${it.businessPartnerType} ${it.bpn}" }
            }
            writtenPlan.suppressed.forEach {
                logger.debug { "Suppressed ${it.changelogType} changelog record for ${it.businessPartnerType} ${it.bpn}: superseded in the same transaction" }
            }
        }
    }

    companion object {
        private const val UNNAMED_TRANSACTION = "unnamed transaction"
    }
}
