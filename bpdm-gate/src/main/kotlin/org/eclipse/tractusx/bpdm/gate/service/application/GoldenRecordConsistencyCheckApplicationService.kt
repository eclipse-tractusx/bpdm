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
import org.eclipse.tractusx.bpdm.gate.model.parsed.goldenrecord.GoldenRecordConsistencyCheckError
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.SharedBusinessPartnerRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.GoldenRecordConsistencyCheckPageRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.union
import org.eclipse.tractusx.bpdm.gate.service.gateway.PoolGoldenRecordGateway
import org.eclipse.tractusx.bpdm.gate.service.operation.goldenrecord.GoldenRecordConsistencyCheckOperationService
import org.eclipse.tractusx.bpdm.gate.service.operation.sharingstate.SharingStateOperationService
import org.eclipse.tractusx.bpdm.gate.service.parser.goldenrecord.GoldenRecordConsistencyCheckParser
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * Fails the shared business partners whose output references a golden record the Pool does not know.
 */
@Service
class GoldenRecordConsistencyCheckApplicationService(
    private val poolGateway: PoolGoldenRecordGateway,
    private val consistencyCheckParser: GoldenRecordConsistencyCheckParser,
    private val sharingStateOperation: SharingStateOperationService,
    private val consistencyCheckOperation: GoldenRecordConsistencyCheckOperationService,
    private val taskProperties: GoldenRecordTaskConfigProperties,
    transactionManager: PlatformTransactionManager
) {
    private val logger = KotlinLogging.logger { }

    // The Pool reads stay outside the transaction, so each page commits through a template rather than @Transactional.
    private val pageTransaction = TransactionTemplate(transactionManager).apply {
        setName("${GoldenRecordConsistencyCheckApplicationService::class.qualifiedName}.checkSharedBusinessPartners")
    }

    /**
     * Checks the golden records of every successfully shared business partner against the Pool, committing one page at a time.
     */
    fun checkSharedBusinessPartners() {
        var shared = findSharedAfter(Long.MIN_VALUE)
        while (shared.isNotEmpty()) {
            checkPage(GoldenRecordConsistencyCheckPageRaw(shared, poolGateway.fetchExistingBpns(shared.map { it.referencedBpns }.union())))
            shared = findSharedAfter(shared.last().sharingStateId)
        }
    }

    private fun findSharedAfter(sharingStateId: Long): List<SharedBusinessPartnerRaw> =
        sharingStateOperation.findSharedAfter(sharingStateId, taskProperties.consistencyCheck.batchSize)

    private fun checkPage(page: GoldenRecordConsistencyCheckPageRaw) {
        pageTransaction.executeWithoutResult {
            val checks = consistencyCheckParser.parse(page).mapNotNull { result ->
                when (result) {
                    is ParseResult.Success -> result.parsed
                    is ParseResult.Failure -> {
                        result.errors.forEach(::log)
                        null
                    }
                }
            }
            consistencyCheckOperation.failInconsistent(checks)
        }
    }

    private fun log(error: GoldenRecordConsistencyCheckError) {
        when (error) {
            is GoldenRecordConsistencyCheckError.SharingStateMissing ->
                logger.debug { "Skipped the consistency check of sharing state ${error.sharingStateId}: its business partner was deleted while its golden records were being checked" }
        }
    }
}
