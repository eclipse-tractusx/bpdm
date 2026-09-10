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

package org.eclipse.tractusx.bpdm.pool.model.error

/**
 * The error wording the golden record task path reports back to the Orchestrator.
 */
enum class GoldenRecordTaskErrorMessage(val message: String) {
    LEGAL_NAME_IS_NULL("Legal name is null"),
    MAINE_ADDRESS_IS_NULL("Main address is null"),
    BPNA_IS_NULL("BpnA Reference is null"),
    PHYSICAL_ADDRESS_COUNTRY_MISSING("Physical Address has no country"),
    PHYSICAL_ADDRESS_CITY_MISSING("Physical Address has no city"),
    ALTERNATIVE_ADDRESS_COUNTRY_MISSING("Alternative Address has no country"),
    ALTERNATIVE_ADDRESS_CITY_MISSING("Alternative Address has no city"),
    ALTERNATIVE_ADDRESS_DELIVERY_SERVICE_TYPE_MISSING("Alternative Address has no delivery service type"),
    ALTERNATIVE_ADDRESS_DELIVERY_SERVICE_NUMBER_MISSING("Alternative Address has no delivery service number"),
    ADDRESS_CONFIDENCE_CRITERIA_MISSING("Logistic address is missing confidence criteria"),
    SITE_CONFIDENCE_CRITERIA_MISSING("Site is missing confidence criteria"),
    SITE_NAME_MISSING("Site has no name"),
    LEGAL_ENTITY_CONFIDENCE_CRITERIA_MISSING("Legal Entity has no confidence criteria"),
    SITE_WRONG_LEGAL_ENTITY_REFERENCE("The legal entity is not the parent of the site"),
    ADDITIONAL_ADDRESS_WRONG_LEGAL_ENTITY_REFERENCE("The legal entity is not the parent of the additional address")
}
