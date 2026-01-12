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

package org.eclipse.tractusx.bpdm.pool.api

import org.eclipse.tractusx.bpdm.common.util.CommonApiPathNames

object ApiCommons {

    const val BASE_PATH_V6 = "/v6"
    const val BASE_PATH_V7 = "/v7"

    const val LEGAL_ENTITY_BASE_PATH_V6 = "${BASE_PATH_V6}/legal-entities"
    const val LEGAL_ENTITY_BASE_PATH_V7 = "${BASE_PATH_V7}/legal-entities"

    const val ADDRESS_BASE_PATH_V6 = "${BASE_PATH_V6}/addresses"
    const val ADDRESS_BASE_PATH_V7 = "${BASE_PATH_V7}/addresses"

    const val SITE_BASE_PATH_V6 = "${BASE_PATH_V6}/sites"
    const val SITE_BASE_PATH_V7 = "${BASE_PATH_V7}/sites"

    const val MEMBERS_LEGAL_ENTITIES_SEARCH_PATH_V6 = "${BASE_PATH_V6}/members/legal-entities${CommonApiPathNames.SUBPATH_SEARCH}"
    const val MEMBERS_SITES_SEARCH_PATH_V6 = "${BASE_PATH_V6}/members/sites${CommonApiPathNames.SUBPATH_SEARCH}"
    const val MEMBERS_ADDRESSES_SEARCH_PATH_V6 = "${BASE_PATH_V6}/members/addresses${CommonApiPathNames.SUBPATH_SEARCH}"
    const val MEMBERS_CHANGELOG_SEARCH_PATH_V6 = "${BASE_PATH_V6}/members/changelog${CommonApiPathNames.SUBPATH_SEARCH}"

    const val MEMBERS_LEGAL_ENTITIES_SEARCH_PATH_V7 = "${BASE_PATH_V7}/participants/legal-entities${CommonApiPathNames.SUBPATH_SEARCH}"
    const val MEMBERS_SITES_SEARCH_PATH_V7 = "${BASE_PATH_V7}/participants/sites${CommonApiPathNames.SUBPATH_SEARCH}"
    const val MEMBERS_ADDRESSES_SEARCH_PATH_V7 = "${BASE_PATH_V7}/participants/addresses${CommonApiPathNames.SUBPATH_SEARCH}"
    const val MEMBERS_CHANGELOG_SEARCH_PATH_V7 = "${BASE_PATH_V7}/participants/changelog${CommonApiPathNames.SUBPATH_SEARCH}"

    const val CHANGELOG_BASE_PATH_V6 = "${BASE_PATH_V6}/business-partners/changelog"
    const val CHANGELOG_BASE_PATH_V7 = "${BASE_PATH_V7}/business-partners/changelog"

    const val BPN_BASE_PATH_V6 = "${BASE_PATH_V6}/bpn"
    const val BPN_BASE_PATH_V7 = "${BASE_PATH_V7}/bpn"

    const val MEMBERSHIP_BASE_PATH_V6 = "${BASE_PATH_V6}/cx-memberships"
    const val MEMBERSHIP_BASE_PATH_V7 = "${BASE_PATH_V7}/data-space-participants"

    const val LEGAL_ENTITIES_NAME = "Legal Entity Controller"
    const val LEGAL_ENTITIES_DESCRIPTION = "Read, create and update business partner of type legal entity"

    const val SITE_NAME = "Site Controller"
    const val SITE_DESCRIPTION ="Read, create and update business partner of type site"

    const val ADDRESS_NAME = "Address Controller"
    const val ADDRESS_DESCRIPTION = "Read, create and update business partner of type address"

    const val METADATA_NAME = "Metadata Controller"
    const val METADATA_DESCRIPTION = "Read and create supporting data that is referencable in business partner data"

    const val CHANGELOG_NAME = "Changelog Controller"
    const val CHANGELOG_DESCRIPTION = "Read change events of business partner data"

    const val BPN_NAME = "Bpn Controller"
    const val BPN_DESCRIPTION = "Support functionality for BPN operations"

    const val BUSINESS_PARTNERS_NAME = "Business Partners Controller"
    const val BUSINESS_PARTNERS_DESCRIPTION = "Look-up business partner"

}