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

import org.eclipse.tractusx.bpdm.gate.mapper.record.SharingStateRecordMapper
import org.eclipse.tractusx.bpdm.gate.model.search.SharingStateSearchRequest
import org.eclipse.tractusx.bpdm.gate.model.sharingstate.SharingStateRecord
import org.eclipse.tractusx.bpdm.gate.service.operation.sharingstate.SharingStateSearchOperationService
import org.eclipse.tractusx.bpdm.gate.service.parser.sharingstate.SharingStateSearchParser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Searches business partner sharing states for every API version.
 */
@Service
class SharingStateSearchApplicationService(
    private val parser: SharingStateSearchParser,
    private val operationService: SharingStateSearchOperationService,
    private val recordMapper: SharingStateRecordMapper
) {

    /**
     * Returns the page of [tenantBpnl]'s sharing states that match [request].
     */
    @Transactional(readOnly = true)
    fun search(request: SharingStateSearchRequest, tenantBpnl: String?, pageable: Pageable): Page<SharingStateRecord> =
        operationService.search(parser.parse(request), tenantBpnl, pageable)
            .map(recordMapper::toRecord)
}
