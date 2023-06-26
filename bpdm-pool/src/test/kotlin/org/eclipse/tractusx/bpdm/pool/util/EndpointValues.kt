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


    const val OPENSEARCH_SYNC_PATH = "api/opensearch/business-partner"

    const val CATENA_METADATA_IDENTIFIER_TYPE_PATH = "$CATENA_PATH/identifier-types"
    const val CATENA_METADATA_LEGAL_FORM_PATH = "$CATENA_PATH/legal-forms"

}