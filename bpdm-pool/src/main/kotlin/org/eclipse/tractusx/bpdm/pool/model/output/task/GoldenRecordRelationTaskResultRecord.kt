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

package org.eclipse.tractusx.bpdm.pool.model.output.task

import org.eclipse.tractusx.bpdm.pool.model.output.address.AddressRelationRecord
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityRelationRecord
import org.eclipse.tractusx.bpdm.pool.model.output.site.SiteRelationRecord
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.RelationUpsertError
import org.eclipse.tractusx.bpdm.pool.model.raw.relation.RelationRaw

/**
 * The outcome of one golden record relation task: the relation as written, the reasons it was rejected, or an unexpected failure.
 */
sealed interface GoldenRecordRelationTaskResultRecord {
    val taskId: String

    data class LegalEntityRelationResolved(
        override val taskId: String,
        val relation: LegalEntityRelationRecord
    ) : GoldenRecordRelationTaskResultRecord

    data class SiteRelationResolved(
        override val taskId: String,
        val relation: SiteRelationRecord
    ) : GoldenRecordRelationTaskResultRecord

    data class AddressRelationResolved(
        override val taskId: String,
        val relation: AddressRelationRecord
    ) : GoldenRecordRelationTaskResultRecord

    data class Rejected(
        override val taskId: String,
        val relation: RelationRaw,
        val errors: List<RelationUpsertError>
    ) : GoldenRecordRelationTaskResultRecord

    data class Failed(
        override val taskId: String,
        val relation: RelationRaw
    ) : GoldenRecordRelationTaskResultRecord
}
