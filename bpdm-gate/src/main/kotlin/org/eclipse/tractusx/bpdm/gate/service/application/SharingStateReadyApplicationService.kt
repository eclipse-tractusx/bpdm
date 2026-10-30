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

package org.eclipse.tractusx.bpdm.gate.service.application

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.gate.model.output.sharingstate.SharingStateReadyResult
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.SharingStateReadyRaw
import org.eclipse.tractusx.bpdm.gate.service.operation.sharingstate.SharingStateReadyOperationService
import org.eclipse.tractusx.bpdm.gate.service.parser.sharingstate.SharingStateReadyParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Sets a batch of business partners ready to be shared for every API version, all or nothing.
 */
@Service
class SharingStateReadyApplicationService(
    private val parser: SharingStateReadyParser,
    private val operationService: SharingStateReadyOperationService
) {

    /**
     * Sets the business partners [request] names ready for [tenantBpnl], or changes nothing and returns every
     * violation if any of them cannot be set ready.
     */
    @Transactional
    fun setReady(request: SharingStateReadyRaw, tenantBpnl: String?): SharingStateReadyResult =
        when (val parseResult = parser.parse(request, tenantBpnl)) {
            is ParseResult.Failure -> SharingStateReadyResult.Rejected(parseResult.errors)
            is ParseResult.Success -> {
                operationService.setReady(parseResult.parsed)
                SharingStateReadyResult.Readied
            }
        }
}
