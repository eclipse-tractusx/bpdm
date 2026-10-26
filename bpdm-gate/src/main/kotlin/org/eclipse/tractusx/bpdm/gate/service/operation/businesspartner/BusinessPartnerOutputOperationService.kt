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
     * Updates the stored output of each of [outputs] to its new data, leaving those whose data is unchanged untouched.
     */
    @Transactional
    fun write(outputs: List<BusinessPartnerOutputParsed>) {
        outputs.forEach { parsed ->
            val result = businessPartnerService.updateBusinessPartnerOutput(parsed.output, parsed.data)
            val sharingState = result.businessPartner.sharingState
            if (result.hadChanges && result.shouldUpdate) {
                changeLogger.log(
                    BusinessPartnerChange(PersistedChangeType.Updated, sharingState.externalId, sharingState.tenantBpnl, BusinessPartnerChange.Aspect.Output)
                )
                logger.debug { "Updated business partner output ${sharingState.externalId} from its golden records" }
            }
        }
    }
}
