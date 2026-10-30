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

package org.eclipse.tractusx.bpdm.gate.mapper.apiv6

import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.StatsAddressTypesResponse
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.StatsConfidenceCriteriaResponse
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.StatsSharingStatesResponse
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.StatsStagesResponse
import org.eclipse.tractusx.bpdm.gate.model.output.stats.AddressTypeStats
import org.eclipse.tractusx.bpdm.gate.model.output.stats.ConfidenceCriteriaStats
import org.eclipse.tractusx.bpdm.gate.model.output.stats.SharingStateStats
import org.eclipse.tractusx.bpdm.gate.model.output.stats.StageStats
import org.springframework.stereotype.Component

/**
 * Maps stats to their API v6 form.
 */
@Component
class ApiV6StatsMapper {

    /**
     * Returns the API response for [stats].
     */
    fun toDto(stats: SharingStateStats): StatsSharingStatesResponse =
        StatsSharingStatesResponse(
            initialTotal = stats.initialTotal,
            readyTotal = stats.readyTotal,
            pendingTotal = stats.pendingTotal,
            successTotal = stats.successTotal,
            errorTotal = stats.errorTotal
        )

    /**
     * Returns the API response for [stats].
     */
    fun toDto(stats: StageStats): StatsStagesResponse =
        StatsStagesResponse(
            inputTotal = stats.inputTotal,
            outputTotal = stats.outputTotal
        )

    /**
     * Returns the API response for [stats].
     */
    fun toDto(stats: AddressTypeStats): StatsAddressTypesResponse =
        StatsAddressTypesResponse(
            legalAndSiteTotal = stats.legalAndSiteTotal,
            legalTotal = stats.legalTotal,
            siteTotal = stats.siteTotal,
            additionalTotal = stats.additionalTotal
        )

    /**
     * Returns the API response for [stats].
     */
    fun toDto(stats: ConfidenceCriteriaStats): StatsConfidenceCriteriaResponse =
        StatsConfidenceCriteriaResponse(
            numberOfBusinessPartnersAverage = stats.numberOfBusinessPartnersAverage,
            uniqueTotal = stats.uniqueTotal,
            sharedByOwnerTotal = stats.sharedByOwnerTotal,
            checkedByExternalDataSourceTotal = stats.checkedByExternalDataSourceTotal,
            confidenceLevelAverage = stats.confidenceLevelAverage
        )
}
