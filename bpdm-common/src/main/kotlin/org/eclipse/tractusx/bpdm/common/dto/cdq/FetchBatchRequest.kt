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

package org.eclipse.tractusx.bpdm.common.dto.cdq

data class FetchBatchRequest(
    val cdqIds: Collection<String>,
    val featuresOn: Collection<Features> = emptyList(),
    val featuresOff: Collection<Features> = emptyList()

) {
    enum class Features {
        ENABLE_SETTINGS,
        SHOW_DEBUG_INFO,
        SHOW_RAW_DATA,
        SHOW_RAW_DATA_JSON,
        FORCE_EXTERNAL_CALL,
        SCREEN_BUSINESS_NAMES,
        ACTIVATE_DATASOURCE_BVD,
        ACTIVATE_DATASOURCE_DNB,
        ACTIVATE_MASTER_DATA_BASIC,
        ACTIVATE_MASTER_DATA_EXTENDED,
        ACTIVATE_LINKAGE_LNKELI
    }
}
