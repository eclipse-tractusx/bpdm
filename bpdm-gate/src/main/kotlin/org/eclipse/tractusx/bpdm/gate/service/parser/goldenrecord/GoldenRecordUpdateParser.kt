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
import org.eclipse.tractusx.bpdm.gate.mapper.DbToParsedMapper
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerOutputParsed
import org.eclipse.tractusx.bpdm.gate.model.parsed.goldenrecord.GoldenRecordAlignmentError
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.GoldenRecordUpdatePageRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.sharingstate.SharingStateType
import org.eclipse.tractusx.bpdm.gate.repository.generic.BusinessPartnerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Decides how the outputs referencing changed golden records change.
 */
@Service
class GoldenRecordUpdateParser(
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val dbToParsedMapper: DbToParsedMapper,
    private val alignmentParser: GoldenRecordAlignmentParser
) {

    /**
     * Returns the output data each successfully shared output referencing a golden record changed in [page] takes on.
     */
    @Transactional(readOnly = true)
    fun parse(page: GoldenRecordUpdatePageRaw): List<ParseResult<BusinessPartnerOutputParsed, GoldenRecordAlignmentError>> {
        val changed = page.changes.changed
        val referencingOutputs = businessPartnerRepository.findByStageAndBpnLIn(StageType.Output, changed.legalEntities) +
                businessPartnerRepository.findByStageAndBpnSIn(StageType.Output, changed.sites) +
                businessPartnerRepository.findByStageAndBpnAIn(StageType.Output, changed.addresses)
        val sharedOutputs = referencingOutputs
            .filter { it.sharingState.sharingStateType == SharingStateType.Success }
            .distinctBy { it.id }

        return sharedOutputs.map { output ->
            alignmentParser.parse(output.sharingState, output, dbToParsedMapper.toUpsertData(output), page.goldenRecords)
        }
    }
}
