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

import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeLogger
import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeType
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.model.businesspartner.BusinessPartnerChange
import org.eclipse.tractusx.bpdm.gate.model.sharingstate.ready.SharingStateReadyParsed
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Sets business partner sharing states ready to be shared.
 */
@Service
class SharingStateReadyOperationService(
    private val sharingStateRepository: SharingStateRepository,
    private val changeLogger: PersistedChangeLogger
) {

    /**
     * Sets the sharing states of [parsed] ready, clearing any error and task of a previous sharing attempt, and returns
     * them in the same order.
     */
    @Transactional
    fun setReady(parsed: SharingStateReadyParsed): List<SharingStateDb> {
        parsed.sharingStates.forEach { sharingState ->
            sharingState.sharingStateType = SharingStateType.Ready
            sharingState.sharingErrorCode = null
            sharingState.sharingErrorMessage = null
            sharingState.sharingProcessStarted = null
            sharingState.taskId = null
        }

        return sharingStateRepository.saveAll(parsed.sharingStates).toList()
            .onEach { changeLogger.log(BusinessPartnerChange(PersistedChangeType.Updated, it.externalId, it.tenantBpnl, BusinessPartnerChange.Aspect.SharingState)) }
    }
}
