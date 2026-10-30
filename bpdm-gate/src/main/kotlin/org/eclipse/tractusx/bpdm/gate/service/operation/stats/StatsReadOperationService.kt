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

package org.eclipse.tractusx.bpdm.gate.service.operation.stats

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.SharingStateType
import org.eclipse.tractusx.bpdm.gate.model.stats.AddressTypeStats
import org.eclipse.tractusx.bpdm.gate.model.stats.ConfidenceCriteriaStats
import org.eclipse.tractusx.bpdm.gate.model.stats.SharingStateStats
import org.eclipse.tractusx.bpdm.gate.model.stats.StageStats
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.ConfidenceCriteriaRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.PostalAddressRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Aggregates statistics over all business partners stored in the Gate, across tenants.
 */
@Service
class StatsReadOperationService(
    private val sharingStateRepository: SharingStateRepository,
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val postalAddressRepository: PostalAddressRepository,
    private val confidenceCriteriaRepository: ConfidenceCriteriaRepository
) {

    /**
     * Returns the number of business partners in each sharing state.
     */
    @Transactional(readOnly = true)
    fun countSharingStates(): SharingStateStats {
        val countsByType = sharingStateRepository.countSharingStateTypes().associate { it.type to it.count }

        return SharingStateStats(
            initialTotal = countsByType[SharingStateType.Initial] ?: 0,
            readyTotal = countsByType[SharingStateType.Ready] ?: 0,
            pendingTotal = countsByType[SharingStateType.Pending] ?: 0,
            successTotal = countsByType[SharingStateType.Success] ?: 0,
            errorTotal = countsByType[SharingStateType.Error] ?: 0
        )
    }

    /**
     * Returns the number of business partners in each stage.
     */
    @Transactional(readOnly = true)
    fun countStages(): StageStats {
        val countsByStage = businessPartnerRepository.countPerStages().associate { it.stage to it.count }

        return StageStats(
            inputTotal = countsByStage[StageType.Input] ?: 0,
            outputTotal = countsByStage[StageType.Output] ?: 0
        )
    }

    /**
     * Returns the number of business partner addresses of each address type in [stage].
     */
    @Transactional(readOnly = true)
    fun countAddressTypes(stage: StageType): AddressTypeStats {
        val countsByType = postalAddressRepository.countAddressTypes(stage).associate { it.type to it.count }

        return AddressTypeStats(
            legalAndSiteTotal = countsByType[AddressType.LegalAndSiteMainAddress] ?: 0,
            legalTotal = countsByType[AddressType.LegalAddress] ?: 0,
            siteTotal = countsByType[AddressType.SiteMainAddress] ?: 0,
            additionalTotal = countsByType[AddressType.AdditionalAddress] ?: 0
        )
    }

    /**
     * Returns the averages and totals of the confidence criteria of all business partners, which are zero when none
     * are stored.
     */
    @Transactional(readOnly = true)
    fun summarizeConfidenceCriteria(): ConfidenceCriteriaStats =
        ConfidenceCriteriaStats(
            numberOfBusinessPartnersAverage = confidenceCriteriaRepository.averageNumberOfBusinessPartners() ?: 0F,
            uniqueTotal = confidenceCriteriaRepository.countUnique() ?: 0L,
            sharedByOwnerTotal = confidenceCriteriaRepository.countSharedByOwner() ?: 0L,
            checkedByExternalDataSourceTotal = confidenceCriteriaRepository.countCheckedByExternalDataSource() ?: 0L,
            confidenceLevelAverage = confidenceCriteriaRepository.averageConfidenceLevel() ?: 0F
        )
}
