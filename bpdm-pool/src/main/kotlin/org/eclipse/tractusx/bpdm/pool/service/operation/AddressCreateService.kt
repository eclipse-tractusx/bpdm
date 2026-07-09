/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.service.operation

import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.service.writer.LogisticAddressWriter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Creates logistic addresses from already-resolved parents — the single owner of the address-create *operation*. It
 * consumes an [AddressCreateParsed] command (content already validated, parents already resolved to entities) and
 * persists the address. Content validation and parent resolution are the parsers' job
 * ([org.eclipse.tractusx.bpdm.pool.service.parser.TypedParentAddressCreateParser] /
 * [org.eclipse.tractusx.bpdm.pool.service.parser.UntypedParentAddressCreateParser]); in-transaction creators whose
 * parent is not yet persisted build the command themselves and call [create] directly. Order-preserving positional
 * contract (see [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 *
 * This is the single-call convenience over [org.eclipse.tractusx.bpdm.pool.service.writer.LogisticAddressWriter]: it stages and immediately commits. Callers that own
 * a cyclic parent relationship use the writer's stage/commit phases directly so they can wire the graph before committing.
 */
@Service
class AddressCreateService(
    private val addressWriter: LogisticAddressWriter
) {

    /**
     * Returns the persisted entities (within the caller's transaction) rather than a detached response model: the
     * write is a pure in-transaction collaborator, and turning entities into version-specific responses is the job of
     * the border/application service at the edge. No `UpsertType` here — a create always yields `Created`, unlike update
     * which can be a no-op.
     */
    @Transactional
    fun create(parsed: List<AddressCreateParsed>): List<LogisticAddressDb> =
        addressWriter.commit(addressWriter.stageCreate(parsed)).map { it.value }
}