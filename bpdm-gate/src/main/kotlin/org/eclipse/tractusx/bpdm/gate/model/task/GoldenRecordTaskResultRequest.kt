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

package org.eclipse.tractusx.bpdm.gate.model.task

import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.GoldenRecordBpns
import org.eclipse.tractusx.bpdm.gate.model.sharingstate.BusinessPartnerSharingError

sealed interface GoldenRecordTaskResultRequest {
    val taskId: String

    data class Pending(override val taskId: String) : GoldenRecordTaskResultRequest

    data class Succeeded(
        override val taskId: String,
        val output: GoldenRecordTaskOutputRequest,
        val bpns: GoldenRecordBpns
    ) : GoldenRecordTaskResultRequest

    data class Failed(
        override val taskId: String,
        val errorCode: BusinessPartnerSharingError,
        val errorMessage: String?
    ) : GoldenRecordTaskResultRequest
}
