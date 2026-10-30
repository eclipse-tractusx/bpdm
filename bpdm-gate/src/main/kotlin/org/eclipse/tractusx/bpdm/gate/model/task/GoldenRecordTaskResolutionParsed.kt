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

import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.BusinessPartnerOutputParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.BusinessPartnerSharingErrorCode

sealed interface GoldenRecordTaskResolutionParsed {
    val sharingState: SharingStateDb

    data class Succeeded(
        override val sharingState: SharingStateDb,
        val output: BusinessPartnerOutputParsed?
    ) : GoldenRecordTaskResolutionParsed

    data class Failed(
        override val sharingState: SharingStateDb,
        val errorCode: BusinessPartnerSharingErrorCode,
        val errorMessage: String?
    ) : GoldenRecordTaskResolutionParsed
}
