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

package org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.inbound

import org.eclipse.tractusx.bpdm.pool.model.request.RelationValidityPeriodRequest
import org.eclipse.tractusx.bpdm.pool.model.request.SuccessionUpsertRequest
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.springframework.stereotype.Component

/**
 * Turns the succession a golden record relation task states into the request the Pool parses.
 */
@Component
class SuccessionUpsertRequestMapper {

    /**
     * Reads the relation as a succession, in which the source is the partner being replaced.
     */
    fun toRequest(relations: BusinessPartnerRelations): SuccessionUpsertRequest =
        SuccessionUpsertRequest(
            predecessorBpn = relations.businessPartnerSourceBpn,
            successorBpn = relations.businessPartnerTargetBpn,
            validityPeriods = relations.validityPeriods.map { RelationValidityPeriodRequest(it.validFrom, it.validTo) },
            reasonCode = relations.reasonCode
        )
}
