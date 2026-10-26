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

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.gate.config.GoldenRecordTaskConfigProperties
import org.eclipse.tractusx.bpdm.gate.entity.SyncTypeDb
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.GoldenRecordAlignmentError
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.GoldenRecordUpdatePageRequest
import org.eclipse.tractusx.bpdm.gate.service.gateway.PoolGoldenRecordGateway
import org.eclipse.tractusx.bpdm.gate.service.operation.businesspartner.BusinessPartnerOutputOperationService
import org.eclipse.tractusx.bpdm.gate.service.operation.sync.SyncRecordOperationService
import org.eclipse.tractusx.bpdm.gate.service.parser.goldenrecord.GoldenRecordUpdateParser
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * Brings business partner outputs up to date with the golden record changes the Pool records.
 */
@Service
class GoldenRecordUpdateApplicationService(
    private val poolGateway: PoolGoldenRecordGateway,
    private val updateParser: GoldenRecordUpdateParser,
    private val outputOperation: BusinessPartnerOutputOperationService,
    private val syncRecordOperation: SyncRecordOperationService,
    private val taskProperties: GoldenRecordTaskConfigProperties,
    transactionManager: PlatformTransactionManager
) {
    private val logger = KotlinLogging.logger { }

    // The Pool reads stay outside the transaction, so each page commits through a template rather than @Transactional.
    private val pageTransaction = TransactionTemplate(transactionManager).apply {
        setName("${GoldenRecordUpdateApplicationService::class.qualifiedName}.updateFromPoolChanges")
    }

    /**
     * Applies every golden record change the Pool recorded since the previous update, committing one page at a time.
     */
    fun updateFromPoolChanges() {
        do {
            val page = poolGateway.fetchChangesAfter(
                syncRecordOperation.getSyncedUntil(SyncTypeDb.POOL_TO_GATE_OUTPUT),
                taskProperties.creation.fromPool.batchSize
            )
            update(page)
        } while (page.changes.hasMore)
    }

    private fun update(page: GoldenRecordUpdatePageRequest) {
        pageTransaction.executeWithoutResult {
            val parsed = updateParser.parse(page).mapNotNull { result ->
                when (result) {
                    is ParseResult.Success -> result.parsed
                    is ParseResult.Failure -> {
                        result.errors.forEach(::log)
                        null
                    }
                }
            }
            outputOperation.write(parsed)
            syncRecordOperation.advance(SyncTypeDb.POOL_TO_GATE_OUTPUT, page.changes.changedUntil)
        }
    }

    private fun log(error: GoldenRecordAlignmentError) {
        val externalId = error.output.sharingState.externalId
        when (error) {
            is GoldenRecordAlignmentError.GoldenRecordMissing ->
                logger.debug { "Skipped the update of business partner output '$externalId': the Pool does not return golden record '${error.bpn}'" }
            is GoldenRecordAlignmentError.AdoptedSiteMissing ->
                logger.warn { "Skipped the update of business partner output '$externalId': its address belongs to site '${error.siteBpn}' which the Pool does not return" }
            is GoldenRecordAlignmentError.AddressTypeMissing ->
                logger.warn { "Skipped the update of business partner output '$externalId': neither the Pool nor the output holds an address type for address '${error.addressBpn}'" }
        }
    }
}
