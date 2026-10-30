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

package org.eclipse.tractusx.bpdm.gate.mapper.record

import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.model.output.businesspartner.BusinessPartnerInputRecord
import org.springframework.stereotype.Component

/**
 * Maps a stored business partner input to its record.
 */
@Component
class BusinessPartnerInputRecordMapper(
    private val contentMapper: BusinessPartnerInputContentMapper
) {

    /**
     * Returns the record of [entity].
     */
    fun toRecord(entity: BusinessPartnerDb): BusinessPartnerInputRecord =
        BusinessPartnerInputRecord(
            externalId = entity.sharingState.externalId,
            externalSequenceTimestamp = entity.externalSequenceTimestamp,
            content = contentMapper.toContent(entity),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
}
