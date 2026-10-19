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
import org.eclipse.tractusx.bpdm.gate.mapper.record.RelationInputRecordMapper
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationInputUpsertMode
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationInputUpsertRequest
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationInputUpsertResult
import org.eclipse.tractusx.bpdm.gate.service.operation.relation.RelationInputUpsertOperationService
import org.eclipse.tractusx.bpdm.gate.service.parser.relation.RelationInputUpsertParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Upserts a batch of relation inputs, all or nothing.
 */
@Service
class RelationInputUpsertApplicationService(
    private val parser: RelationInputUpsertParser,
    private val operationService: RelationInputUpsertOperationService,
    private val recordMapper: RelationInputRecordMapper
) {

    /**
     * Upserts [requests] for [tenantBpnl] as far as [mode] allows and returns every requested relation input, or writes
     * nothing and returns the violations of every entry if any entry is invalid.
     */
    @Transactional
    fun upsert(requests: List<RelationInputUpsertRequest>, tenantBpnl: String, mode: RelationInputUpsertMode): RelationInputUpsertResult =
        when (val parseResult = parser.parse(requests, tenantBpnl, mode)) {
            is ParseResult.Failure -> RelationInputUpsertResult.Rejected(parseResult.errors)
            is ParseResult.Success -> RelationInputUpsertResult.Written(
                operationService.upsert(parseResult.parsed, tenantBpnl).map { recordMapper.toRecord(it.input) }
            )
        }
}
