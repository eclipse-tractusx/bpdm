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

import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.GoldenRecordBpnsRaw

data class GoldenRecordConsistencyCheckParsed(
    val sharingState: SharingStateDb,
    val checkedBpns: GoldenRecordBpnsRaw,
    val currentBpns: GoldenRecordBpnsRaw,
    val missingBpns: List<String>
)
