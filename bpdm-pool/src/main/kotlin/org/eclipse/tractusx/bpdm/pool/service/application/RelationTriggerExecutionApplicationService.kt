/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.pool.service.application

import org.eclipse.tractusx.bpdm.pool.service.operation.relation.AddressRelationTriggerService
import org.eclipse.tractusx.bpdm.pool.service.operation.relation.LegalEntityRelationTriggerService
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate

/**
 * Applies the state changes that relations scheduled for the day their validity period starts or ends.
 */
@Service
class RelationTriggerExecutionApplicationService(
    private val addressRelationTriggerService: AddressRelationTriggerService,
    private val legalEntityRelationTriggerService: LegalEntityRelationTriggerService,
    transactionManager: PlatformTransactionManager
) {

    // One transaction per trigger instead of one per batch: each trigger walks a whole succession or ownership graph, and
    // the backlog of due triggers is unbounded.
    private val triggerTransaction = TransactionTemplate(transactionManager).apply {
        setName("${RelationTriggerExecutionApplicationService::class.qualifiedName}.executeDueTriggers")
    }

    /**
     * Executes every pending trigger due today or earlier, address relation triggers first, oldest first, committing each
     * one on its own.
     */
    fun executeDueTriggers() {
        val today = LocalDate.now()
        executeOneByOne { addressRelationTriggerService.findNextDue(today)?.also(addressRelationTriggerService::execute) != null }
        executeOneByOne { legalEntityRelationTriggerService.findNextDue(today)?.also(legalEntityRelationTriggerService::execute) != null }
    }

    private fun executeOneByOne(executeNext: () -> Boolean) {
        do {
            val executed = triggerTransaction.execute { executeNext() } ?: false
        } while (executed)
    }
}
