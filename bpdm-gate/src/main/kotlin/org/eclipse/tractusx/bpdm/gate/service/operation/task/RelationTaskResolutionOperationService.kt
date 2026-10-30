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

package org.eclipse.tractusx.bpdm.gate.service.operation.task

import org.eclipse.tractusx.bpdm.gate.model.parsed.task.RelationTaskResolutionParsed
import org.eclipse.tractusx.bpdm.gate.service.operation.relation.RelationOutputOperationService
import org.eclipse.tractusx.bpdm.gate.service.operation.relation.RelationSharingStateOperationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Applies the outcome of finished golden record tasks to relation output and sharing states.
 */
@Service
class RelationTaskResolutionOperationService(
    private val outputOperation: RelationOutputOperationService,
    private val sharingStateOperation: RelationSharingStateOperationService
) {

    /**
     * Writes the output of each succeeded resolution in [resolutions] and sets every sharing state to the outcome of its
     * task.
     */
    @Transactional
    fun resolve(resolutions: List<RelationTaskResolutionParsed>) {
        val succeeded = resolutions.filterIsInstance<RelationTaskResolutionParsed.Succeeded>()
        outputOperation.write(succeeded.map { it.output })
        sharingStateOperation.succeed(succeeded.map { it.relation })
        resolutions.filterIsInstance<RelationTaskResolutionParsed.Failed>()
            .forEach { sharingStateOperation.fail(it.relation, it.errorCode, it.errorMessage) }
    }
}
