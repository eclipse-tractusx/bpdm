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

package org.eclipse.tractusx.bpdm.orchestrator.mapper

import org.eclipse.tractusx.bpdm.orchestrator.model.request.BusinessPartnerRelationsRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.RelationTypeRequest
import org.eclipse.tractusx.bpdm.orchestrator.model.request.RelationValidityPeriodRequest
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.eclipse.tractusx.orchestrator.api.model.RelationType
import org.springframework.stereotype.Component

@Component
class BusinessPartnerRelationsRequestMapper {

    fun toBusinessPartnerRelationsRequest(businessPartnerRelations: BusinessPartnerRelations): BusinessPartnerRelationsRequest =
        with(businessPartnerRelations) {
            BusinessPartnerRelationsRequest(
                relationType = when (relationType) {
                    RelationType.IsAlternativeHeadquarterFor -> RelationTypeRequest.IsAlternativeHeadquarterFor
                    RelationType.IsManagedBy -> RelationTypeRequest.IsManagedBy
                    RelationType.IsOwnedBy -> RelationTypeRequest.IsOwnedBy
                    RelationType.IsReplacedBy -> RelationTypeRequest.IsReplacedBy
                },
                businessPartnerSourceBpn = businessPartnerSourceBpn,
                businessPartnerTargetBpn = businessPartnerTargetBpn,
                validityPeriods = validityPeriods.map { validityPeriod ->
                    RelationValidityPeriodRequest(
                        validFrom = validityPeriod.validFrom,
                        validTo = validityPeriod.validTo
                    )
                },
                reasonCode = reasonCode
            )
        }
}
