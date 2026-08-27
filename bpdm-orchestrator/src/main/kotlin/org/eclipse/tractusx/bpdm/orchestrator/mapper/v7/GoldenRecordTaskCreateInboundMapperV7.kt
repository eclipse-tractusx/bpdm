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

package org.eclipse.tractusx.bpdm.orchestrator.mapper.v7

import org.eclipse.tractusx.bpdm.orchestrator.model.BusinessPartnerRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.GoldenRecordTaskCreateRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateRequestEntry
import org.springframework.stereotype.Component
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartner as ApiBusinessPartner

@Component
class GoldenRecordTaskCreateInboundMapperV7 {

    fun toRequest(entry: TaskCreateRequestEntry): GoldenRecordTaskCreateRequest =
        GoldenRecordTaskCreateRequest(
            recordId = entry.recordId,
            businessPartner = toBusinessPartnerRequest(entry.businessPartner)
        )

    private fun toBusinessPartnerRequest(apiBp: ApiBusinessPartner): BusinessPartnerRequest =
        BusinessPartnerRequest(
            nameParts = apiBp.nameParts,
            owningCompany = apiBp.owningCompany,
            uncategorized = apiBp.uncategorized,
            legalEntity = apiBp.legalEntity,
            site = apiBp.site,
            additionalAddress = apiBp.additionalAddress,
            additionalSites = apiBp.additionalSites
        )
}
