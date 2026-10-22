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

import org.eclipse.tractusx.bpdm.gate.mapper.record.RelationSharingStateRecordMapper
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationSharingStateRecord
import org.eclipse.tractusx.bpdm.gate.model.search.RelationSharingStateSearchRequest
import org.eclipse.tractusx.bpdm.gate.service.operation.relation.RelationSharingStateSearchOperationService
import org.eclipse.tractusx.bpdm.gate.service.parser.relation.RelationSharingStateSearchParser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Searches relation sharing states.
 */
@Service
class RelationSharingStateSearchApplicationService(
    private val parser: RelationSharingStateSearchParser,
    private val operationService: RelationSharingStateSearchOperationService,
    private val recordMapper: RelationSharingStateRecordMapper
) {

    /**
     * Returns the page of [tenantBpnl]'s relation sharing states that match [request].
     */
    @Transactional(readOnly = true)
    fun search(request: RelationSharingStateSearchRequest, tenantBpnl: String, pageable: Pageable): Page<RelationSharingStateRecord> =
        operationService.search(parser.parse(request), tenantBpnl, pageable)
            .map(recordMapper::toRecord)
}
