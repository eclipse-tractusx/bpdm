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

package org.eclipse.tractusx.bpdm.gate.api.v7.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.common.dto.IBaseLogisticAddressDto
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.LogisticAddressDescription

@Schema(description = LogisticAddressDescription.header)
data class LogisticAddressV7Dto(

    @get:ArraySchema(arraySchema = Schema(description = LogisticAddressDescription.nameParts))
    val nameParts: Collection<String> = emptyList(),

    override val states: Collection<AddressStateV7Dto> = emptyList(),
    override val identifiers: Collection<AddressIdentifierV7Dto> = emptyList(),
    override val physicalPostalAddress: PhysicalPostalAddressV7Dto,
    override val alternativePostalAddress: AlternativePostalAddressV7Dto? = null,

    @get:ArraySchema(arraySchema = Schema(description = LogisticAddressDescription.roles))
    val roles: Collection<BusinessPartnerRole> = emptyList(),

    override val confidenceCriteria: ConfidenceCriteriaV7Dto? = null

) : IBaseLogisticAddressDto
