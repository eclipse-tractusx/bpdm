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

import org.eclipse.tractusx.bpdm.gate.mapper.record.RelationInputRecordMapper
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationInputRecord
import org.eclipse.tractusx.bpdm.gate.model.search.RelationInputSearchRequest
import org.eclipse.tractusx.bpdm.gate.service.operation.relation.RelationInputSearchOperationService
import org.eclipse.tractusx.bpdm.gate.service.parser.relation.RelationInputSearchParser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Searches relation inputs for every API version.
 */
@Service
class RelationInputSearchApplicationService(
    private val parser: RelationInputSearchParser,
    private val operationService: RelationInputSearchOperationService,
    private val recordMapper: RelationInputRecordMapper
) {

    /**
     * Returns the page of [tenantBpnl]'s relation inputs that match [request].
     */
    @Transactional(readOnly = true)
    fun search(request: RelationInputSearchRequest, tenantBpnl: String, pageable: Pageable): Page<RelationInputRecord> =
        operationService.search(parser.parse(request), tenantBpnl, pageable)
            .map(recordMapper::toRecord)
}
