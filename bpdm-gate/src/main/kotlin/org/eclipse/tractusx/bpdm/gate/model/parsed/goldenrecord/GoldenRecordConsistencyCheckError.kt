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

package org.eclipse.tractusx.bpdm.gate.model.parsed.goldenrecord

/**
 * Reason a shared business partner whose golden records were checked cannot be resolved for the consistency check.
 */
sealed interface GoldenRecordConsistencyCheckError {
    val sharingStateId: Long

    data class SharingStateMissing(override val sharingStateId: Long) : GoldenRecordConsistencyCheckError
}
