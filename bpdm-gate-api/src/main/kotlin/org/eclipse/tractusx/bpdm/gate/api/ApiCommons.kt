/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.api

object ApiCommons {
    const val BASE_PATH_V6 = "/v6"
    const val BASE_PATH_V7 = "/v7"

    const val SHARING_STATE_PATH_V6 = "$BASE_PATH_V6/sharing-state"
    const val SHARING_STATE_PATH_V7 = "$BASE_PATH_V7/sharing-state"

    const val STATS_PATH_V6 = "$BASE_PATH_V6/stats"
    const val STATS_PATH_V7 = "$BASE_PATH_V7/stats"

    const val RELATIONS_INPUT_PATH_V6 = "$BASE_PATH_V6/input/business-partner-relations"
    const val RELATIONS_INPUT_PATH_V7 = "$BASE_PATH_V7/input/business-partner-relations"

    const val RELATIONS_OUTPUT_PATH_V7 = "$BASE_PATH_V7/output/business-partner-relations"

    const val RELATION_SHARING_STATE_PATH_V7 = "$BASE_PATH_V7/relations/sharing-state"
}