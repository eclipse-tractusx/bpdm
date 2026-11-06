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

import org.eclipse.tractusx.bpdm.pool.model.output.address.AddressRecord
import org.eclipse.tractusx.bpdm.pool.model.output.legalentity.LegalEntityRecord
import org.eclipse.tractusx.bpdm.pool.model.output.site.SiteRecord
import org.eclipse.tractusx.bpdm.pool.model.parsed.error.GoldenRecordTaskError
import org.eclipse.tractusx.bpdm.pool.model.raw.task.GoldenRecordPassThroughRaw

/**
 * The outcome of one golden record task: its golden records, the reasons it was rejected, or an unexpected failure.
 */
sealed interface GoldenRecordTaskResultRecord {
    val taskId: String

    data class Resolved(
        override val taskId: String,
        val legalEntity: LegalEntityRecord,
        val isLegalEntityWritten: Boolean,
        val site: SiteRecord?,
        val isSiteWritten: Boolean,
        val additionalAddress: AddressRecord?,
        val additionalSites: List<AdditionalSiteRecord>,
        val passThrough: GoldenRecordPassThroughRaw
    ) : GoldenRecordTaskResultRecord

    data class Rejected(
        override val taskId: String,
        val errors: List<GoldenRecordTaskError>
    ) : GoldenRecordTaskResultRecord

    data class Failed(
        override val taskId: String
    ) : GoldenRecordTaskResultRecord
}

data class AdditionalSiteRecord(
    val bpn: String,
    val name: String
)
