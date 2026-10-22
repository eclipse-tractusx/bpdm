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

package org.eclipse.tractusx.bpdm.gate.model.relation

import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateErrorCode
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType
import java.time.Instant

data class RelationSharingStateRecord(
    val externalId: String,
    val sharingStateType: RelationSharingStateType,
    val sharingErrorCode: RelationSharingStateErrorCode?,
    val sharingErrorMessage: String?,
    val taskId: String?,
    val updatedAt: Instant
)
