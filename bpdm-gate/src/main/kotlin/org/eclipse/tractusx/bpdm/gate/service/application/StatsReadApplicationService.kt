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

import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.model.output.stats.AddressTypeStats
import org.eclipse.tractusx.bpdm.gate.model.output.stats.ConfidenceCriteriaStats
import org.eclipse.tractusx.bpdm.gate.model.output.stats.SharingStateStats
import org.eclipse.tractusx.bpdm.gate.model.output.stats.StageStats
import org.eclipse.tractusx.bpdm.gate.service.operation.stats.StatsReadOperationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Reads the business partner statistics for every API version.
 */
@Service
class StatsReadApplicationService(
    private val operationService: StatsReadOperationService
) {

    /**
     * Returns the number of business partners in each sharing state.
     */
    @Transactional(readOnly = true)
    fun countSharingStates(): SharingStateStats = operationService.countSharingStates()

    /**
     * Returns the number of business partners in each stage.
     */
    @Transactional(readOnly = true)
    fun countStages(): StageStats = operationService.countStages()

    /**
     * Returns the number of business partner addresses of each address type in [stage].
     */
    @Transactional(readOnly = true)
    fun countAddressTypes(stage: StageType): AddressTypeStats = operationService.countAddressTypes(stage)

    /**
     * Returns the averages and totals of the confidence criteria of all business partners.
     */
    @Transactional(readOnly = true)
    fun summarizeConfidenceCriteria(): ConfidenceCriteriaStats = operationService.summarizeConfidenceCriteria()
}
