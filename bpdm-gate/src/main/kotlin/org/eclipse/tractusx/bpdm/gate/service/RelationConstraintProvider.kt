/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.service

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.springframework.stereotype.Service

@Service
class RelationConstraintProvider: IRelationConstraintProvider {

    private val isManagedByConstraints = IRelationConstraintProvider.RelationTypeConstraints(
        selfRelateAllowed = false,
        sourceMustBeOwnCompanyData = true,
        targetMustBeOwnCompanyData = true,
        sourceAllowedTypes = setOf(BusinessPartnerType.LEGAL_ENTITY),
        targetAllowedTypes = setOf(BusinessPartnerType.LEGAL_ENTITY),
        sourceCanBeTarget = false,
        multipleSourcesAllowed = true,
        multipleTargetsAllowed = false
    )

    private val isAlternativeHeadquarterForConstraints = IRelationConstraintProvider.RelationTypeConstraints(
        selfRelateAllowed = false,
        sourceMustBeOwnCompanyData = false,
        targetMustBeOwnCompanyData = false,
        sourceAllowedTypes = setOf(BusinessPartnerType.LEGAL_ENTITY),
        targetAllowedTypes = setOf(BusinessPartnerType.LEGAL_ENTITY),
        sourceCanBeTarget = true,
        multipleSourcesAllowed = true,
        multipleTargetsAllowed = true
    )

    override fun getConstraints(relationType: RelationType): IRelationConstraintProvider.RelationTypeConstraints {
        return when(relationType){
            RelationType.IsManagedBy -> isManagedByConstraints
            RelationType.IsAlternativeHeadquarterFor -> isAlternativeHeadquarterForConstraints
        }
    }


}