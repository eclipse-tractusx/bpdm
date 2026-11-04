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
package org.eclipse.tractusx.bpdm.pool.mapper.apiv7

import org.eclipse.tractusx.bpdm.pool.api.v7.model.businesspartner.ConfidenceCriteriaV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.relation.RelationValidityPeriodV7Dto
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.ConfidenceCriteriaRecord
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.RelationValidityPeriodRecord
import org.springframework.stereotype.Component

/**
 * Maps the parts legal entity, site and address records share to their API v7 form.
 */
@Component
class ApiV7BusinessPartnerMapper {

    /**
     * Returns the API DTO for [record].
     */
    fun toDto(record: ConfidenceCriteriaRecord): ConfidenceCriteriaV7Dto =
        ConfidenceCriteriaV7Dto(
            sharedByOwner = record.sharedByOwner,
            checkedByExternalDataSource = record.checkedByExternalDataSource,
            numberOfSharingMembers = record.numberOfSharingMembers,
            lastConfidenceCheckAt = record.lastConfidenceCheckAt,
            nextConfidenceCheckAt = record.nextConfidenceCheckAt,
            confidenceLevel = record.confidenceLevel
        )

    /**
     * Returns the API DTO for [record].
     */
    fun toDto(record: RelationValidityPeriodRecord): RelationValidityPeriodV7Dto =
        RelationValidityPeriodV7Dto(validFrom = record.validFrom, validTo = record.validTo)
}
