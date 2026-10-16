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

package org.eclipse.tractusx.bpdm.gate.model.sharingstate

import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerSharingError
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import java.time.Instant
import java.time.LocalDateTime

data class SharingStateRecord(
    val externalId: String,
    val sharingStateType: SharingStateType,
    val sharingErrorCode: BusinessPartnerSharingError?,
    val sharingErrorMessage: String?,
    val sharingProcessStarted: LocalDateTime?,
    val taskId: String?,
    val updatedAt: Instant
)
