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

package org.eclipse.tractusx.bpdm.gate.service.operation.sharingstate

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeLogger
import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeType
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.model.businesspartner.BusinessPartnerChange
import org.eclipse.tractusx.bpdm.gate.model.sharingstate.GoldenRecordCountedSyncParsed
import org.eclipse.tractusx.bpdm.gate.model.sharingstate.GoldenRecordCountedUpdate
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Maintains which business partners count towards the sharing member count of their golden record, and which of those
 * flags the Orchestrator has received.
 */
@Service
class GoldenRecordCountedOperationService(
    private val sharingStateRepository: SharingStateRepository,
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val changeLogger: PersistedChangeLogger
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Recounts the golden records of [outputs], so that of all outputs sharing an address golden record only the oldest
     * counts.
     */
    @Transactional
    fun recalculate(outputs: List<BusinessPartnerDb>) {
        val outputsByBpnA = businessPartnerRepository.findByStageAndBpnAIn(StageType.Output, outputs.map { it.bpnA!! }).groupBy { it.bpnA!! }

        outputs.forEach { output ->
            val sameGoldenRecordOutputs = outputsByBpnA[output.bpnA!!].orEmpty().plus(output).toSet().sortedBy { it.createdAt }
            setGoldenRecordCounted(sameGoldenRecordOutputs.first().sharingState, true)
            sameGoldenRecordOutputs.drop(1).forEach { setGoldenRecordCounted(it.sharingState, false) }
        }
    }

    /**
     * Returns up to [limit] golden record counted flags the Orchestrator has not received yet, in the order of their
     * sharing state id and starting after [sharingStateId].
     */
    @Transactional(readOnly = true)
    fun findUnsyncedAfter(sharingStateId: Long, limit: Int): List<GoldenRecordCountedUpdate> =
        sharingStateRepository.findGoldenRecordCountedUnsyncedAfter(sharingStateId, Pageable.ofSize(limit))
            .map { GoldenRecordCountedUpdate(it.id, checkNotNull(it.orchestratorRecordId).toString(), checkNotNull(it.isGoldenRecordCounted)) }

    /**
     * Records for each of [syncs] that the Orchestrator received the flag sent for it.
     */
    @Transactional
    fun markSynced(syncs: List<GoldenRecordCountedSyncParsed>) {
        val changed = syncs.filter { it.sharingState.syncedIsGoldenRecordCounted != it.sentIsGoldenRecordCounted }
        logger.debug { "Sent ${syncs.size} golden record counted flags to the Orchestrator, ${changed.size} of them not yet recorded as received" }

        changed.forEach { sync ->
            sync.sharingState.syncedIsGoldenRecordCounted = sync.sentIsGoldenRecordCounted
            sharingStateRepository.save(sync.sharingState)
            logChange(sync.sharingState)
        }
    }

    private fun setGoldenRecordCounted(sharingState: SharingStateDb, isGoldenRecordCounted: Boolean) {
        if (sharingState.isGoldenRecordCounted == isGoldenRecordCounted) return

        sharingState.isGoldenRecordCounted = isGoldenRecordCounted
        sharingStateRepository.save(sharingState)
        logChange(sharingState)
    }

    private fun logChange(sharingState: SharingStateDb) {
        changeLogger.log(
            BusinessPartnerChange(PersistedChangeType.Updated, sharingState.externalId, sharingState.tenantBpnl, BusinessPartnerChange.Aspect.SharingState)
        )
    }
}
