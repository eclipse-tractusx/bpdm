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

package org.eclipse.tractusx.bpdm.pool.service.gateway

import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.inbound.OrchestratorInboundMapper
import org.eclipse.tractusx.bpdm.pool.model.raw.participation.SharingMemberRecordPageRaw
import org.eclipse.tractusx.orchestrator.api.client.OrchestrationApiClient
import org.eclipse.tractusx.orchestrator.api.model.SharingMemberRecordQueryRequest
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Reads the sharing member records the Orchestrator holds.
 */
@Component
class SharingMemberRecordGateway(
    private val orchestrationApiClient: OrchestrationApiClient,
    private val inboundMapper: OrchestratorInboundMapper
) {

    /**
     * Returns the oldest page of sharing member records the Orchestrator updated after [after].
     */
    fun fetchChangesAfter(after: Instant, pageSize: Int): SharingMemberRecordPageRaw {
        val page = orchestrationApiClient.sharingMemberRecords.queryRecords(
            SharingMemberRecordQueryRequest(after),
            PaginationRequest(size = pageSize)
        )

        return SharingMemberRecordPageRaw(
            records = page.content.map { inboundMapper.toRaw(it) },
            // Resuming after the last entry of the page is only lossless because the Orchestrator returns the records
            // oldest update first: every record this page has not seen was updated after the one the cursor stops at.
            changedUntil = page.content.lastOrNull()?.updatedAt,
            hasMore = page.totalElements > page.contentSize
        )
    }
}
