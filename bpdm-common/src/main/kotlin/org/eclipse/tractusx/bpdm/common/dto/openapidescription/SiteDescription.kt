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

package org.eclipse.tractusx.bpdm.common.dto.openapidescription

object SiteDescription {
    const val header = "In general, a site is a delimited geographical area in which an organization (such as an " +
            "enterprise or company, university, association, etc.) conducts business. " +
            "In Catena-X, a site is a type of business partner representing a physical location or area owned " +
            "by a legal entity, where a production plant, a warehouse, or an office building is located. " +
            "A site is owned by a legal entity. Thus, exactly one legal entity is assigned to a site. A site has " +
            "exactly one main address, but it is possible to specify additional addresses (such as different " +
            "gates), that belong to a site. Thus, at least one address is assigned to a site. A site can only be " +
            "uploaded and modified by the owner (the legal entity), because only the owner knows which " +
            "addresses belong to which site. A site is uniquely identified by the BPNS."
    const val headerCreateRequest = "Request for creating new business partner record of type site. $header"
    const val headerUpdateRequest = "Request for updating a business partner record of type site. $header"
    const val headerUpsertRequest = "Request for creating/updating a business partner record of type site. $header"
    const val headerUpsertResponse = "Created/updated business partner of type site. $header"
    const val headerMatchResponse = "Match for a business partner record of type site. $header"

    const val bpns = "A BPNS represents and uniquely identifies a site, which is where for example a production plant, " +
            "a warehouse, or an office building is located."
    const val name = "The name of the site. This is not according to official registers but according to the name the owner chooses."
    const val nameParts = "The list of name parts of the site to accommodate the different number of name fields in different systems. " +
            "This is not according to official registers but according to the name the owner chooses."
    const val states = "The list of the (temporary) states of the site."
    const val bpnLegalEntity = "The BPNL of the legal entity owning the site."
    const val mainAddress = "The address, where typically the main entrance or the reception is located, or where the mail is delivered to."
    const val roles = "Roles this business partner takes in relation to the sharing member."

    const val bpnlParent = "The BPNL of the legal entity owning the site."
    const val legalEntityExternalId = "The identifier which uniquely identifies (in the internal system landscape of the sharing member) " +
            "the business partner owning the site."
    const val site = "Site information"
}