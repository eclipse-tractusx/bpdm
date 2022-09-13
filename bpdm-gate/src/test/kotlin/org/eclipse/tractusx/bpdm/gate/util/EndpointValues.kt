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

package org.eclipse.tractusx.bpdm.gate.util

object EndpointValues {

    const val CDQ_MOCK_DATA_EXCHANGE_API_PATH = "/test-cdq-data-exchange-api/test-cdq-storage"
    const val CDQ_MOCK_BUSINESS_PARTNER_PATH = "$CDQ_MOCK_DATA_EXCHANGE_API_PATH/businesspartners"
    const val CDQ_MOCK_RELATIONS_PATH = "$CDQ_MOCK_DATA_EXCHANGE_API_PATH/relations"
    const val CDQ_MOCK_DELETE_RELATIONS_PATH = "$CDQ_MOCK_RELATIONS_PATH/delete"
    const val CDQ_MOCK_FETCH_BUSINESS_PARTNER_PATH = "$CDQ_MOCK_BUSINESS_PARTNER_PATH/fetch"

    const val CDQ_MOCK_DATA_CLINIC_API_PATH = "/test-cdq-data-clinic-api/test-cdq-storage"
    const val CDQ_MOCK_AUGMENTED_BUSINESS_PARTNER_PATH = "$CDQ_MOCK_DATA_CLINIC_API_PATH/augmentedbusinesspartners"
    const val CDQ_MOCK_FETCH_AUGMENTED_LEGAL_ENTITY_PATH =
        "$CDQ_MOCK_DATA_CLINIC_API_PATH/datasources/test-cdq-datasource-legal-entity/augmentedbusinesspartners/fetch"

    const val GATE_API_PATH = "/api/catena"
    const val GATE_API_INPUT_PATH = "${GATE_API_PATH}/input"
    const val GATE_API_INPUT_LEGAL_ENTITIES_PATH = "${GATE_API_INPUT_PATH}/legal-entities"
    const val GATE_API_INPUT_SITES_PATH = "${GATE_API_INPUT_PATH}/sites"
    const val GATE_API_INPUT_ADDRESSES_PATH = "${GATE_API_INPUT_PATH}/addresses"

    const val GATE_API_OUTPUT_PATH = "${GATE_API_PATH}/output"
    const val GATE_API_OUTPUT_LEGAL_ENTITIES_PATH = "${GATE_API_OUTPUT_PATH}/legal-entities"
    const val GATE_API_OUTPUT_SITES_PATH = "${GATE_API_OUTPUT_PATH}/sites"

    const val POOL_API_MOCK_LEGAL_ENTITIES_SEARCH_PATH = "/legal-entities/search"
    const val POOL_API_MOCK_LEGAL_ADDRESSES_SEARCH_PATH = "/legal-entities/legal-addresses/search"
    const val POOL_API_MOCK_SITES_SEARCH_PATH = "/sites/search"
    const val POOL_API_MOCK_SITES_MAIN_ADDRESSES_SEARCH_PATH = "/sites/main-addresses/search"
}