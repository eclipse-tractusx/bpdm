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

import org.eclipse.tractusx.bpdm.gate.model.parsed.task.GoldenRecordTaskResolutionParsed
import org.eclipse.tractusx.bpdm.gate.service.operation.businesspartner.BusinessPartnerOutputOperationService
import org.eclipse.tractusx.bpdm.gate.service.operation.sharingstate.GoldenRecordCountedOperationService
import org.eclipse.tractusx.bpdm.gate.service.operation.sharingstate.SharingStateOperationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Applies the outcome of finished golden record tasks to business partner output and sharing states.
 */
@Service
class GoldenRecordTaskResolutionOperationService(
    private val outputOperation: BusinessPartnerOutputOperationService,
    private val sharingStateOperation: SharingStateOperationService,
    private val goldenRecordCountedOperation: GoldenRecordCountedOperationService
) {

    /**
     * Writes the output of each succeeded resolution in [resolutions], sets every sharing state to the outcome of its
     * task and recalculates which of the written outputs counts as golden record.
     */
    @Transactional
    fun resolve(resolutions: List<GoldenRecordTaskResolutionParsed>) {
        val succeeded = resolutions.filterIsInstance<GoldenRecordTaskResolutionParsed.Succeeded>()
        val written = outputOperation.write(succeeded.mapNotNull { it.output })
        sharingStateOperation.succeed(succeeded.map { it.sharingState })
        resolutions.filterIsInstance<GoldenRecordTaskResolutionParsed.Failed>()
            .forEach { sharingStateOperation.fail(it.sharingState, it.errorCode, it.errorMessage) }
        goldenRecordCountedOperation.recalculate(written)
    }
}
