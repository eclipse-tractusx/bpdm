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

package org.eclipse.tractusx.bpdm.pool.util

object EndpointValues {

    const val TEXT_PARAM_NAME = "text"

    const val CATENA_PATH = "/api/catena"

    const val CATENA_BUSINESS_PARTNERS_PATH = "$CATENA_PATH/business-partners"
    const val CATENA_BUSINESS_PARTNER_LEGACY_PATH = "$CATENA_PATH/business-partner"
    const val CATENA_CHANGELOG_PATH_POSTFIX = "/changelog"
    const val CATENA_LEGAL_ENTITY_PATH = "$CATENA_PATH/legal-entities"

    const val SAAS_SYNCH_PATH = "/api/saas/business-partner/sync"
    const val OPENSEARCH_SYNC_PATH = "api/opensearch/business-partner"

    const val CATENA_METADATA_IDENTIFIER_TYPE_PATH = "$CATENA_PATH/identifier-types"
    const val CATENA_METADATA_IDENTIFIER_STATUS_PATH = "$CATENA_PATH/identifier-status"
    const val CATENA_METADATA_ISSUING_BODY_PATH = "$CATENA_PATH/issuing-bodies"
    const val CATENA_METADATA_LEGAL_FORM_PATH = "$CATENA_PATH/legal-forms"
    const val CATENA_SUGGESTION_PATH = "$CATENA_PATH/suggestions"
    const val CATENA_SUGGESTION_LEGAL_ENTITIES_PATH = "$CATENA_SUGGESTION_PATH/legal-entities"
    const val CATENA_SUGGESTION_LE_NAME_PATH = "$CATENA_SUGGESTION_LEGAL_ENTITIES_PATH/names"
    const val CATENA_SUGGESTION_LE_LEGAL_FORM_PATH = "$CATENA_SUGGESTION_LEGAL_ENTITIES_PATH/legal-forms"
    const val CATENA_SUGGESTION_LE_STATUS_PATH = "$CATENA_SUGGESTION_LEGAL_ENTITIES_PATH/statuses"
    const val CATENA_SUGGESTION_LE_CLASSIFICATION_PATH = "$CATENA_SUGGESTION_LEGAL_ENTITIES_PATH/classifications"
    const val CATENA_SUGGESTION_SITES_PATH = "$CATENA_SUGGESTION_PATH/sites"
    const val CATENA_SUGGESTION_SITE_NAME_PATH = "$CATENA_SUGGESTION_SITES_PATH/names"
    const val CATENA_SUGGESTION_ADDRESS_PATH = "$CATENA_SUGGESTION_PATH/addresses"
    const val CATENA_SUGGESTION_ADDRESS_ADMIN_AREA_PATH = "$CATENA_SUGGESTION_ADDRESS_PATH/administrative-areas"
    const val CATENA_SUGGESTION_ADDRESS_POST_CODE_PATH = "$CATENA_SUGGESTION_ADDRESS_PATH/postcodes"
    const val CATENA_SUGGESTION_ADDRESS_LOCALITY_PATH = "$CATENA_SUGGESTION_ADDRESS_PATH/localities"
    const val CATENA_SUGGESTION_ADDRESS_THOROUGHFARE_PATH = "$CATENA_SUGGESTION_ADDRESS_PATH/thoroughfares"
    const val CATENA_SUGGESTION_ADDRESS_PREMISE_PATH = "$CATENA_SUGGESTION_ADDRESS_PATH/premises"
    const val CATENA_SUGGESTION_ADDRESS_POSTAL_DELIVERY_POINT_PATH = "$CATENA_SUGGESTION_ADDRESS_PATH/postal-delivery-points"

}