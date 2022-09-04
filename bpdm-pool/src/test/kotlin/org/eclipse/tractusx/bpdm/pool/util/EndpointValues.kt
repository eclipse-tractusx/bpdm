/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

    const val CDQ_MOCK_STORAGE_PATH = "/test-cdq-api/storages/test-cdq-storage"
    const val CDQ_MOCK_BUSINESS_PARTNER_PATH = "$CDQ_MOCK_STORAGE_PATH/businesspartners"

    const val TEXT_PARAM_NAME = "text"

    const val CATENA_PATH = "/api/catena"
    const val CATENA_BUSINESS_PARTNER_PATH = "${CATENA_PATH}/business-partner"

    const val CATENA_CONFIRM_UP_TO_DATE_PATH_POSTFIX = "/confirm-up-to-date"
    const val CATENA_CHANGELOG_PATH_POSTFIX = "/changelog"
    const val CATENA_ADDRESSES_PATH_POSTFIX = "/addresses"
    const val CATENA_SITES_PATH_POSTFIX = "/sites"

    const val CATENA_BPN_SEARCH_PATH = "/api/catena/bpn/search"

    const val CATENA_LEGAL_ENTITY_PATH = "$CATENA_PATH/legal-entities"
    const val CATENA_LEGAL_ADDRESS_PATH = "$CATENA_LEGAL_ENTITY_PATH/legal-addresses"
    const val CATENA_LEGAL_ADDRESS_SEARCH_PATH = "$CATENA_LEGAL_ADDRESS_PATH/search"

    const val CATENA_ADDRESSES_PATH = "/api/catena/addresses"
    const val CATENA_ADDRESSES_SEARCH_PATH = "$CATENA_ADDRESSES_PATH/search"

    const val CATENA_SITES_PATH = "/api/catena/sites"
    const val CATENA_SITE_SEARCH_PATH = "$CATENA_SITES_PATH/search"
    const val CATENA_SITE_MAIN_ADDRESS_SEARCH_PATH = "$CATENA_SITES_PATH/main-addresses/search"

    const val CDQ_SYNCH_PATH = "/api/cdq/business-partner/sync"

    const val OPENSEARCH_SYNC_PATH = "api/opensearch/business-partner"

    const val CATENA_METADATA_IDENTIFIER_TYPE_PATH = "$CATENA_PATH/identifier-type"
    const val CATENA_METADATA_IDENTIFIER_STATUS_PATH = "$CATENA_PATH/identifier-status"
    const val CATENA_METADATA_ISSUING_BODY_PATH = "$CATENA_PATH/issuing-body"
    const val CATENA_METADATA_LEGAL_FORM_PATH = "$CATENA_PATH/legal-form"

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