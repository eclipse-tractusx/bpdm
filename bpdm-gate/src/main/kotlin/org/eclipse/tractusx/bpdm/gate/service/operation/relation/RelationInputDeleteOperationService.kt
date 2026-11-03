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

package org.eclipse.tractusx.bpdm.gate.service.operation.relation

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeLogger
import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeType
import org.eclipse.tractusx.bpdm.gate.model.parsed.relation.RelationInputDeleteParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationChangeRaw
import org.eclipse.tractusx.bpdm.gate.repository.RelationRepository
import org.eclipse.tractusx.bpdm.gate.repository.RelationStageRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Deletes relations of a tenant as a whole.
 */
@Service
class RelationInputDeleteOperationService(
    private val relationStageRepository: RelationStageRepository,
    private val relationRepository: RelationRepository,
    private val changeLogger: PersistedChangeLogger
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Deletes the relation of [parsed] as a whole, together with its output and sharing state.
     */
    @Transactional
    fun delete(parsed: RelationInputDeleteParsed) {
        val relation = parsed.input.relation
        relationStageRepository.delete(parsed.input)
        relationRepository.delete(relation)

        changeLogger.log(RelationChangeRaw(PersistedChangeType.Deleted, relation.externalId, relation.tenantBpnL, aspect = null))
        logger.debug { "Deleted relation ${relation.externalId}" }
    }
}
