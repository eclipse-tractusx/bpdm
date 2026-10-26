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

package org.eclipse.tractusx.bpdm.gate.service.operation.businesspartner

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeLogger
import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeType
import org.eclipse.tractusx.bpdm.gate.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.model.businesspartner.BusinessPartnerChange
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.BusinessPartnerOutputParsed
import org.eclipse.tractusx.bpdm.gate.service.BusinessPartnerService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Writes the output stage of business partners.
 */
@Service
class BusinessPartnerOutputOperationService(
    private val businessPartnerService: BusinessPartnerService,
    private val changeLogger: PersistedChangeLogger
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Creates or updates the output of each of [outputs] and returns every output in the same order, leaving those whose
     * data is unchanged untouched.
     */
    @Transactional
    fun write(outputs: List<BusinessPartnerOutputParsed>): List<BusinessPartnerDb> =
        outputs.map { parsed ->
            val result = businessPartnerService.upsertBusinessPartnerOutput(parsed.storedOutput, parsed.sharingState, parsed.data)
            if (result.hadChanges && result.shouldUpdate) logChange(result)
            result.businessPartner
        }

    private fun logChange(result: BusinessPartnerService.UpsertResult) {
        val sharingState = result.businessPartner.sharingState
        val changeType = when (result.type) {
            ChangelogType.CREATE -> PersistedChangeType.Created
            ChangelogType.UPDATE -> PersistedChangeType.Updated
        }
        changeLogger.log(BusinessPartnerChange(changeType, sharingState.externalId, sharingState.tenantBpnl, BusinessPartnerChange.Aspect.Output))
        logger.debug { "Wrote business partner output ${sharingState.externalId} as ${result.type}" }
    }
}
