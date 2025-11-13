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

package org.eclipse.tractusx.orchestrator.api.v6.model

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.orchestrator.api.model.*

@Schema(description = "Generic business partner data for golden record processing. " +
        "Typically a sharing member shares incomplete and/or uncategorized business partner data to the golden record process. " +
        "The golden record process categorizes and completes the data in order to create and update the resulting golden records. " +
        "The golden records are found in the legalEntity, site and additionalAddress fields. " +
        "The business partner data needs to contain the full golden record parent relationship. " +
        "This means, if an additional address is specified in the business partner data, also its legal entity and also its site parent (if a site exists) needs to be specified. ")
data class BusinessPartner(
    @Schema(description = "Fully categorized and cleaned name parts based on the uncategorized name parts provided")
    val nameParts: List<NamePart>,
    @Schema(description = "The BPNL of the legal entity to which this business partner data belongs to")
    val owningCompany: String?,
    val uncategorized: UncategorizedProperties,
    val legalEntity: LegalEntity,
    val site: Site?,
    val additionalAddress: PostalAddress?,
){
    companion object{
        val empty = BusinessPartner(
            nameParts = emptyList(),
            owningCompany = null,
            uncategorized = UncategorizedProperties.empty,
            legalEntity = LegalEntity.empty,
            site = null,
            additionalAddress = null
        )
    }

    @Schema(description = "The recognized golden record type this business partner data contains.\n" +
            "* `Legal Entity`: The business partner data only contains legal entity and legal address information.\n" +
            "* `Site`: The business partner data contains site, site main address and its parent legal entity information.\n" +
            "* `Additional Address`: The business partner data contains an additional address, (optional) parent site and parent legal entity information.\n" +
            "* `Null`: No clear type determined, undecided. The golden record process will not create golden record from this business partner data.")
    val type: GoldenRecordType? = when{
        additionalAddress != null -> GoldenRecordType.Address
        site != null -> GoldenRecordType.Site
        legalEntity != LegalEntity.empty -> GoldenRecordType.LegalEntity
        else -> null
    }
}

@Schema(description = "Legal entity information for this business partner data. " +
        "Every business partner either is a legal entity or belongs to a legal entity." +
        "There, a legal entity property is not allowed to be 'null'. " )
data class LegalEntity(
    val bpnReference: BpnReference,
    @Schema(description = "The legal name of this legal entity according to official registers")
    val legalName: String?,
    @Schema(description = "The abbreviated name of this legal entity, if it exists")
    val legalShortName: String?,
    @Schema(description = "The legal form of this legal entity")
    val legalForm: String?,
    @Schema(description = "Identifiers for this legal entity (in addition to the BPNL)")
    val identifiers: List<Identifier>,
    @Schema(description = "The business state history of this legal entity")
    val states: List<BusinessState>,
    val confidenceCriteria: ConfidenceCriteria,
    @Schema(description = "Whether this legal entity is part of the Catena-X network")
    val isCatenaXMemberData: Boolean?,
    @Schema(description = "Whether this legal entity information differs from its golden record counterpart in the Pool. +" +
            "The Pool will not update the legal entity if it is set to false. " +
            "However, if this legal entity constitutes a new legal entity golden record, it is still created independent of this flag.")
    val hasChanged: Boolean?,
    val legalAddress: PostalAddress
){
    companion object{
        val empty = LegalEntity(
            bpnReference = BpnReference.empty,
            legalName = null,
            legalShortName = null,
            legalForm = null,
            identifiers = emptyList(),
            states = emptyList(),
            confidenceCriteria = ConfidenceCriteria.empty,
            isCatenaXMemberData = null,
            hasChanged = null,
            legalAddress = PostalAddress.empty
        )
    }
}