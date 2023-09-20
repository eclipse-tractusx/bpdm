/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.api.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.ClassificationDto
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.CommonDescription


interface IBaseBusinessPartnerDto {

    @get:Schema(description = CommonDescription.externalId)
    val externalId: String

    @get:ArraySchema(arraySchema = Schema(description = "The list of name parts to accommodate the different number of name fields in different systems."))
    val nameParts: List<String>

    @get:Schema(description = "Abbreviated name or shorthand.")
    val shortName: String?

    @get:ArraySchema(arraySchema = Schema(description = "The list of identifiers of the business partner. Sorted and duplicates are removed."))
    val identifiers: Collection<BusinessPartnerIdentifierDto>

    @get:Schema(description = "Technical key of the legal form.")
    val legalForm: String?

    @get:ArraySchema(arraySchema = Schema(description = "The list of (temporary) states of the business partner. Sorted and duplicates are removed."))
    val states: Collection<BusinessPartnerStateDto>

    @get:ArraySchema(arraySchema = Schema(description = "The list of classifications of the business partner, such as a specific industry. Sorted and duplicates are removed."))
    val classifications: Collection<ClassificationDto>

    @get:ArraySchema(arraySchema = Schema(description = "Roles this business partner takes in relation to the sharing member. Sorted and duplicates are removed."))
    val roles: Collection<BusinessPartnerRole>

    @get:Schema(name = "isOwner", description = "True if the sharing member declares itself as the owner of the business partner.")
    val isOwner: Boolean

    @get:Schema(description = "BPNL")
    val bpnL: String?

    @get:Schema(description = "BPNS")
    val bpnS: String?

    @get:Schema(description = "BPNA")
    val bpnA: String?
}
