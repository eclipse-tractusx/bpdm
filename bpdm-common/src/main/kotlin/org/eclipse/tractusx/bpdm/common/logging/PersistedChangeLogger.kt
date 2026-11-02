/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.bpdm.common.logging

import mu.KLogger
import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.util.countForLog
import org.eclipse.tractusx.bpdm.common.util.joinIdentifiersForLog
import org.eclipse.tractusx.bpdm.common.util.parenthesizeIdentifiersForLog
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * Logs persisted changes at INFO once their transaction commits, one line per subject kind and change type.
 *
 * Changes logged for the same subject within one transaction merge into a single entry naming every changed aspect,
 * so each writer logs only the aspect it writes; the lines appear under the class whose method opened the transaction.
 */
@Component
class PersistedChangeLogger {
    private val logger = KotlinLogging.logger { }

    /**
     * Logs [change] at INFO once the current transaction commits, or immediately outside a transaction.
     */
    fun log(change: PersistedChange) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return log(listOf(change), logger)
        (openBuffer() ?: openNewBuffer()).add(change)
    }

    @Suppress("UNCHECKED_CAST")
    private fun openBuffer(): MutableList<PersistedChange>? =
        TransactionSynchronizationManager.getResource(this) as MutableList<PersistedChange>?

    private fun openNewBuffer(): MutableList<PersistedChange> {
        val buffer = mutableListOf<PersistedChange>()
        // The synchronized variant unbinds the buffer once the transaction completes and suspends it around a nested
        // transaction, neither of which a plain resource binding does.
        TransactionSynchronizationManager.bindSynchronizedResource(this, buffer)
        TransactionSynchronizationManager.registerSynchronization(LogOnCommit(buffer, transactionOwnerLogger()))
        return buffer
    }

    // A declarative transaction is named after the method that opened it, as in "com.example.SomeService.someMethod".
    private fun transactionOwnerLogger(): KLogger =
        TransactionSynchronizationManager.getCurrentTransactionName()
            ?.substringBeforeLast('.')
            ?.let { KotlinLogging.logger(it) }
            ?: logger

    private fun log(changes: List<PersistedChange>, owner: KLogger) {
        changes.groupBy { it.subject to it.identifierForLog() }
            .map { (subjectAndIdentifier, changesOfSubject) -> merge(subjectAndIdentifier.first, subjectAndIdentifier.second, changesOfSubject) }
            .groupBy { it.subject to it.type }
            .forEach { (subjectAndType, merged) -> log(subjectAndType.first, subjectAndType.second, merged, owner) }
    }

    private fun log(subject: PersistedChangeSubject, type: PersistedChangeType, changes: List<MergedChange>, owner: KLogger) {
        owner.info {
            "$type ${countForLog(changes.size, subject.singular, subject.plural)}: " +
                    changes.map { it.identifier + it.aspects.parenthesizeIdentifiersForLog() }.joinIdentifiersForLog()
        }
    }

    private fun merge(subject: PersistedChangeSubject, identifier: String, changes: List<PersistedChange>): MergedChange {
        val types = changes.map { it.type }.toSet()
        val type = when {
            PersistedChangeType.Deleted in types -> PersistedChangeType.Deleted
            PersistedChangeType.Created in types -> PersistedChangeType.Created
            else -> PersistedChangeType.Updated
        }
        return MergedChange(subject, identifier, type, changes.mapNotNull { it.aspectForLog() }.distinct())
    }

    private data class MergedChange(
        val subject: PersistedChangeSubject,
        val identifier: String,
        val type: PersistedChangeType,
        val aspects: List<String>
    )

    private inner class LogOnCommit(
        private val changes: List<PersistedChange>,
        private val owner: KLogger
    ) : TransactionSynchronization {

        override fun afterCommit() {
            log(changes, owner)
        }

        override fun afterCompletion(status: Int) {
            if (status != TransactionSynchronization.STATUS_COMMITTED)
                owner.debug { "Discarded ${changes.size} persisted changes: transaction did not commit" }
        }
    }
}
