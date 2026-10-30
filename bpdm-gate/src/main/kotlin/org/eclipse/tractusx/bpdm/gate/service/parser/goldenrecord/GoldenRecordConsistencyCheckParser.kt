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

package org.eclipse.tractusx.bpdm.gate.service.parser.goldenrecord

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.GoldenRecordBpns
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.GoldenRecordConsistencyCheckError
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.GoldenRecordConsistencyCheckPageRequest
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.GoldenRecordConsistencyCheckParsed
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.springframework.stereotype.Service

/**
 * Resolves the shared business partners whose golden records were checked against the Pool.
 */
@Service
class GoldenRecordConsistencyCheckParser(
    private val sharingStateRepository: SharingStateRepository,
    private val businessPartnerRepository: BusinessPartnerRepository
) {

    /**
     * Resolves the sharing state and current output of each checked business partner in [page], failing those whose
     * sharing state no longer exists.
     */
    fun parse(page: GoldenRecordConsistencyCheckPageRequest): List<ParseResult<GoldenRecordConsistencyCheckParsed, GoldenRecordConsistencyCheckError>> {
        val sharingStatesById = sharingStateRepository.findAllById(page.sharedBusinessPartners.map { it.sharingStateId }).associateBy { it.id }
        val outputsBySharingStateId = businessPartnerRepository.findBySharingStateInAndStage(sharingStatesById.values, StageType.Output)
            .associateBy { it.sharingState.id }

        return page.sharedBusinessPartners.map { shared ->
            val sharingState = sharingStatesById[shared.sharingStateId]
                ?: return@map ParseResult.ofSingleFailure(GoldenRecordConsistencyCheckError.SharingStateMissing(shared.sharingStateId))
            ParseResult.Success(
                GoldenRecordConsistencyCheckParsed(
                    sharingState = sharingState,
                    checkedBpns = shared.referencedBpns,
                    currentBpns = outputsBySharingStateId[sharingState.id]?.referencedBpns() ?: GoldenRecordBpns.NONE,
                    missingBpns = findMissing(shared.referencedBpns, page.existingBpns)
                )
            )
        }
    }

    private fun findMissing(checked: GoldenRecordBpns, existing: GoldenRecordBpns): List<String> =
        checked.legalEntities.minus(existing.legalEntities).toList() +
            checked.sites.minus(existing.sites) +
            checked.addresses.minus(existing.addresses)
}
