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
    const val CDQ_MOCK_FETCH_BUSINESS_PARTNER_PATH = "$CDQ_MOCK_BUSINESS_PARTNER_PATH/fetch"

    const val CDQ_MOCK_DATA_CLINIC_API_PATH = "/test-cdq-data-clinic-api/test-cdq-storage"
    const val CDQ_MOCK_AUGMENTED_BUSINESS_PARTNER_PATH = "$CDQ_MOCK_DATA_CLINIC_API_PATH/augmentedbusinesspartners"

    const val CATENA_PATH = "/api/catena"
    const val CATENA_INPUT_PATH = "${CATENA_PATH}/input"
    const val CATENA_INPUT_LEGAL_ENTITIES_PATH = "${CATENA_INPUT_PATH}/legal-entities"
    const val CATENA_INPUT_SITES_PATH = "${CATENA_INPUT_PATH}/sites"

    const val CATENA_OUTPUT_PATH = "${CATENA_PATH}/output"
    const val CATENA_OUTPUT_LEGAL_ENTITIES_PATH = "${CATENA_OUTPUT_PATH}/legal-entities"
}