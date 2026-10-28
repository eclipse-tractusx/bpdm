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

package org.eclipse.tractusx.bpdm.gate.mapper.orchestrator.outbound

import org.eclipse.tractusx.bpdm.gate.model.sharingstate.GoldenRecordCountedUpdate
import org.eclipse.tractusx.orchestrator.api.model.SharingMemberRecordUpdateRequest
import org.springframework.stereotype.Component

/**
 * Maps sharing member record updates onto the requests the Orchestrator receives.
 */
@Component
class SharingMemberRecordRequestMapper {

    /**
     * Maps [update] onto a request setting whether its record counts towards the sharing member count of its golden
     * record.
     */
    fun toRequest(update: GoldenRecordCountedUpdate): SharingMemberRecordUpdateRequest =
        SharingMemberRecordUpdateRequest(update.recordId, update.isGoldenRecordCounted)
}
