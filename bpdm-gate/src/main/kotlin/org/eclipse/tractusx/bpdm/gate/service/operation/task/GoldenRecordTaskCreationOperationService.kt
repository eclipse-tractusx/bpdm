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

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.model.StageType
import org.eclipse.tractusx.bpdm.gate.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.mapper.record.BusinessPartnerInputContentMapper
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.SharingStateType
import org.eclipse.tractusx.bpdm.gate.model.task.GoldenRecordTaskCandidate
import org.eclipse.tractusx.bpdm.gate.repository.SharingStateRepository
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Finds the business partners ready for a golden record task to be created for them.
 */
@Service
class GoldenRecordTaskCreationOperationService(
    private val sharingStateRepository: SharingStateRepository,
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val contentMapper: BusinessPartnerInputContentMapper,
    private val bpnConfigProperties: BpnConfigProperties
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Returns up to [limit] business partners ready to be shared, each with its input and the company owning it,
     * leaving out those without input.
     */
    @Transactional(readOnly = true)
    fun findCandidates(limit: Int): List<GoldenRecordTaskCandidate> {
        val readySharingStates = sharingStateRepository.findBySharingStateType(SharingStateType.Ready, Pageable.ofSize(limit)).content
        val inputs = businessPartnerRepository.findBySharingStateInAndStage(readySharingStates, StageType.Input)
        logger.debug { "Found ${readySharingStates.size} business partners ready to be shared, ${inputs.size} of them with input" }

        return inputs.map { input ->
            GoldenRecordTaskCandidate(
                sharingStateId = input.sharingState.id,
                externalId = input.sharingState.externalId,
                recordId = input.sharingState.orchestratorRecordId?.toString(),
                input = contentMapper.toContent(input),
                owningCompanyBpnl = findOwningCompanyBpnl(input)
            )
        }
    }

    private fun findOwningCompanyBpnl(input: BusinessPartnerDb): String? {
        if (!input.isOwnCompanyData) return null

        return input.sharingState.tenantBpnl
            ?: bpnConfigProperties.ownerBpnL.takeIf { it.isNotBlank() }
            ?: run {
                logger.warn { "Business partner '${input.sharingState.externalId}' is own company data, but neither its tenant nor the configuration states an owner BPNL" }
                null
            }
    }
}
