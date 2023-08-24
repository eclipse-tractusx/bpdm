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
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.LegalEntityDescription


interface IBaseBusinessPartnerInputDto {

    @get:Schema(description = "ID the record has in the external system where the record originates from")
    val externalId: String

    @get:Schema(description = "")
    val nameParts: List<String>

    @get:Schema(description = "Abbreviated name or shorthand")
    val shortName: String?

    @get:ArraySchema(arraySchema = Schema(description = LegalEntityDescription.identifiers))
    val identifiers: Collection<BusinessPartnerIdentifierDto>

    @get:Schema(description = "Technical key of the legal form")
    val legalForm: String?

    @get:ArraySchema(arraySchema = Schema(description = LegalEntityDescription.states))
    val states: Collection<BusinessPartnerStateDto>

    @get:ArraySchema(arraySchema = Schema(description = LegalEntityDescription.classifications))
    val classifications: Collection<ClassificationDto>

    @get:Schema(description = "Which roles this business partner takes in relation to the sharing member")
    val roles: Collection<BusinessPartnerRole>

    @get:Schema(description = "")
    val isOwner: Boolean

    @get:Schema(description = "Address of the official seat of this legal entity")
    val postalAddress: BusinessPartnerPostalAddressDto


}


