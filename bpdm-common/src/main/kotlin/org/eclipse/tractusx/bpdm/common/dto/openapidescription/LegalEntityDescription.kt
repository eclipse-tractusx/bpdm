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

package org.eclipse.tractusx.bpdm.common.dto.openapidescription

object LegalEntityDescription {
    const val header = "In general, a legal entity is a juridical person that has legal rights and duties related to " +
            "contracts, agreements, and obligations. The term especially applies to any kind of " +
            "organization (such as an enterprise or company, university, association, etc.) established " +
            "under the law applicable to a country." +
            "In Catena-X, a legal entity is a type of business partner representing a legally registered " +
            "organization with its official registration information, such as legal name (including legal form, " +
            "if registered), legal address and tax number." +
            "A legal entity has exactly one legal address, but it is possible to specify additional addresses " +
            "that a legal entity owns. Thus, at least one address is assigned to a legal entity. A legal entity " +
            "can own sites. Thus, many or no sites are assigned to a legal entity. A legal entity is uniquely " +
            "identified by the BPNL."
    const val headerCreateRequest = "Request for creating new business partner record of type legal entity. $header"
    const val headerUpdateRequest = "Request for updating a business partner record of type legal entity. $header"
    const val headerUpsertRequest = "Request for creating/updating a business partner record of type legal entity. $header"
    const val headerUpsertResponse = "Created/updated business partner of type legal entity. $header"
    const val headerMatchResponse = "Match with score for a business partner record of type legal entity. $header"

    const val bpnl = "A BPNL represents and uniquely identifies a legal entity, which is defined by its legal name (including legal form, if registered), " +
            "legal address and tax number."
    const val currentness = "The date the business partner data was last indicated to be still current."

    const val legalName = "The name of the legal entity according to official registers."
    const val legalNameParts = "The list of name parts of the legal entity to accommodate the different number of name fields in different systems."
    const val legalShortName = "The abbreviated name of the legal entity."
    const val legalForm = "The legal form of the legal entity."

    const val identifiers = "The list of identifiers of the legal entity."
    const val states = "The list of (temporary) states of the legal entity."
    const val classifications = "The list of classifications of the legal entity, such as a specific industry."
    const val relations = "Relations to other business partners."
    const val roles = "Roles this business partner takes in relation to the sharing member."
}
