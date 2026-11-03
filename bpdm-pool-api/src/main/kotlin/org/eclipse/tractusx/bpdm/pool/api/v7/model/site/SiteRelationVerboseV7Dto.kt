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

package org.eclipse.tractusx.bpdm.pool.api.v7.model.site

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.RelationDescription
import org.eclipse.tractusx.bpdm.pool.api.v7.model.relation.RelationValidityPeriodV7Dto

@Schema(
    name = "SiteRelationVerboseV7Dto",
    description = "Directed relation between two business partners of type site"
)
data class SiteRelationVerboseV7Dto(

    @get:Schema(description = "The type of relation between the sites")
    val type: SiteRelationTypeV7,

    @get:Schema(description = "BPNS of the site from which the relation starts (source)")
    val businessPartnerSourceBpns: String,

    @get:Schema(description = "BPNS of the site to which this relation points (target)")
    val businessPartnerTargetBpns: String,

    @get:Schema(description = "Validity periods of this site relation")
    val validityPeriods: Collection<RelationValidityPeriodV7Dto>,

    @get:Schema(description = RelationDescription.reasonCode)
    val reasonCode: String?
)
