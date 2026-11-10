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

package org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner

import io.swagger.v3.oas.annotations.media.Schema

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
    val additionalAddress: PostalAddressWithScriptVariants?,
    @get:Schema(description = "The further sites the address of this business partner data belongs to, next to the site in 'site'. " +
            "Together with 'site' this states the address's complete site membership: a site the address currently belongs to but this field leaves out is unlinked from it. " +
            "Consolidating the records that share an address into that complete list is the golden record process's responsibility, not the Pool's. " +
            "An entry that resolves to an existing site links that site to the address; an entry that does not resolve creates a new site with this address as its main address. " +
            "An already existing site is never updated through this field. A site the address is the main address of cannot be unlinked and has to be stated.")
    val additionalSites: List<AdditionalSite> = emptyList(),
){
    companion object{
        val empty = BusinessPartner(
            nameParts = emptyList(),
            owningCompany = null,
            uncategorized = UncategorizedProperties.empty,
            legalEntity = LegalEntity.empty,
            site = null,
            additionalAddress = null,
            additionalSites = emptyList()
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
