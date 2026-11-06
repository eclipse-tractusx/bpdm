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

package org.eclipse.tractusx.bpdm.pool.service.parser.task

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.RelationUpsertError
import org.eclipse.tractusx.bpdm.pool.model.parsed.relation.RelationUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.relation.RelationRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.relation.RelationType
import org.eclipse.tractusx.bpdm.pool.model.raw.task.GoldenRecordRelationTaskRaw
import org.eclipse.tractusx.bpdm.pool.service.parser.relation.AddressRelationUpsertParser
import org.eclipse.tractusx.bpdm.pool.service.parser.relation.LegalEntityRelationUpsertParser
import org.eclipse.tractusx.bpdm.pool.service.parser.relation.SiteRelationUpsertParser
import org.springframework.stereotype.Service

/**
 * Decides which relation a golden record relation task writes, or finds every reason it cannot be carried out.
 */
@Service
class GoldenRecordRelationTaskParser(
    private val legalEntityRelationParser: LegalEntityRelationUpsertParser,
    private val siteRelationParser: SiteRelationUpsertParser,
    private val addressRelationParser: AddressRelationUpsertParser
) {

    /**
     * Reports the relation [task] writes, or every problem that stops it.
     */
    fun parse(task: GoldenRecordRelationTaskRaw): ParseResult<RelationUpsertParsed, RelationUpsertError> {
        val relation = task.relation
        return when {
            relation.connects(LEGAL_ENTITY_BPN_PREFIX) -> legalEntityRelationParser.parse(relation)
            relation.connects(ADDRESS_BPN_PREFIX) && relation.type == RelationType.IsReplacedBy -> addressRelationParser.parse(relation)
            relation.connects(SITE_BPN_PREFIX) && relation.type == RelationType.IsReplacedBy -> siteRelationParser.parse(relation)
            else -> ParseResult.ofSingleFailure(RelationUpsertError.UnsupportedRelation(relation.sourceBpn, relation.targetBpn, relation.type))
        }
    }

    private fun RelationRaw.connects(bpnPrefix: String) =
        sourceBpn.startsWith(bpnPrefix, ignoreCase = true) && targetBpn.startsWith(bpnPrefix, ignoreCase = true)

    companion object {
        private const val LEGAL_ENTITY_BPN_PREFIX = "BPNL"
        private const val SITE_BPN_PREFIX = "BPNS"
        private const val ADDRESS_BPN_PREFIX = "BPNA"
    }
}
