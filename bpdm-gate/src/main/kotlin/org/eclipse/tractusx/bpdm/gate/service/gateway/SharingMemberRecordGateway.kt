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

package org.eclipse.tractusx.bpdm.gate.service.gateway

import org.eclipse.tractusx.bpdm.gate.mapper.orchestrator.outbound.SharingMemberRecordRequestMapper
import org.eclipse.tractusx.bpdm.gate.model.sharingstate.GoldenRecordCountedUpdate
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.springframework.stereotype.Component

/**
 * Exchanges the sharing member records of business partners with the Orchestrator.
 */
@Component
class SharingMemberRecordGateway(
    private val orchestrationApiClient: OrchestrationApiClient,
    private val requestMapper: SharingMemberRecordRequestMapper
) {

    /**
     * Sends whether the record of each of [updates] counts towards the sharing member count of its golden record.
     */
    fun updateGoldenRecordCounted(updates: List<GoldenRecordCountedUpdate>) {
        updates.forEach { update ->
            orchestrationApiClient.sharingMemberRecords.update(requestMapper.toRequest(update))
        }
    }
}
