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

package org.eclipse.tractusx.bpdm.gate.util

object EndpointValues {

    const val SAAS_MOCK_DATA_EXCHANGE_API_PATH = "/test-saas-data-exchange-api/test-saas-storage"
    const val SAAS_MOCK_BUSINESS_PARTNER_PATH = "$SAAS_MOCK_DATA_EXCHANGE_API_PATH/businesspartners"
    const val SAAS_MOCK_RELATIONS_PATH = "$SAAS_MOCK_DATA_EXCHANGE_API_PATH/relations"
    const val SAAS_MOCK_DELETE_RELATIONS_PATH = "$SAAS_MOCK_RELATIONS_PATH/delete"

    const val SAAS_MOCK_DATA_CLINIC_API_PATH = "/test-saas-data-clinic-api/test-saas-storage"
    const val SAAS_MOCK_AUGMENTED_BUSINESS_PARTNER_PATH = "$SAAS_MOCK_DATA_CLINIC_API_PATH/augmentedbusinesspartners"

    const val SAAS_MOCK_DATA_VALIDATION_API_PATH = "/test-saas-data-validation-api"
    const val SAAS_MOCK_DATA_VALIDATION_BUSINESSPARTNER_PATH = "$SAAS_MOCK_DATA_VALIDATION_API_PATH/businesspartners/validate"

    const val GATE_API_PATH = "/api/catena"
    const val GATE_API_INPUT_PATH = "${GATE_API_PATH}/input"
    const val GATE_API_INPUT_LEGAL_ENTITIES_PATH = "${GATE_API_INPUT_PATH}/legal-entities"

    const val POOL_API_MOCK_LEGAL_ENTITIES_SEARCH_PATH = "/legal-entities/search"
    const val POOL_API_MOCK_LEGAL_ADDRESSES_SEARCH_PATH = "/legal-entities/legal-addresses/search"
    const val POOL_API_MOCK_SITES_SEARCH_PATH = "/sites/search"
    const val POOL_API_MOCK_SITES_MAIN_ADDRESSES_SEARCH_PATH = "/sites/main-addresses/search"
    const val POOL_API_MOCK_ADDRESSES_SEARCH_PATH = "/addresses/search"

}