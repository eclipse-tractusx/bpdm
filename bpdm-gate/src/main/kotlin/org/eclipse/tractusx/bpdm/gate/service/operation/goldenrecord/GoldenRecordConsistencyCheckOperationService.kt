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

package org.eclipse.tractusx.bpdm.gate.service.operation.goldenrecord

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.GoldenRecordConsistencyCheckParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.BusinessPartnerSharingErrorCode
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.SharingStateType
import org.eclipse.tractusx.bpdm.gate.service.operation.sharingstate.SharingStateOperationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Fails the shared business partners whose output references a golden record the Pool does not know.
 */
@Service
class GoldenRecordConsistencyCheckOperationService(
    private val sharingStateOperation: SharingStateOperationService
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Fails each sharing state in [checks] still shared with the golden records it was checked for, if the Pool does not
     * know any of them.
     */
    @Transactional
    fun failInconsistent(checks: List<GoldenRecordConsistencyCheckParsed>) {
        val inconsistent = checks.filter { it.missingBpns.isNotEmpty() && isStillSharedWithCheckedBpns(it) }
        logger.debug { "Checked the golden records of ${checks.size} shared business partners, ${inconsistent.size} of them missing in the Pool" }

        inconsistent.forEach { check ->
            val errorMessage = "Business partner with external-ID ${check.sharingState.externalId} references not existing golden record '${check.missingBpns.joinToString()}'"
            sharingStateOperation.fail(check.sharingState, BusinessPartnerSharingErrorCode.BpnNotInPool, errorMessage)
        }
    }

    private fun isStillSharedWithCheckedBpns(check: GoldenRecordConsistencyCheckParsed): Boolean =
        check.sharingState.sharingStateType == SharingStateType.Success && check.currentBpns == check.checkedBpns
}
