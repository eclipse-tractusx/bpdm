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

data class UpsertRequest(
    val datasource: String,
    val businessPartners: Collection<BusinessPartnerCdq>,
    val featuresOn: Collection<CdqFeatures> = emptyList(),
    val featuresOff: Collection<CdqFeatures> = emptyList()
) {
    enum class CdqFeatures {
        UPSERT_BY_EXTERNAL_ID,
        API_ERROR_ON_FAILURES,
        LAB_USE_QUEUES,
        ENABLE_PRECURATION,
        TRANSFORM_RECORD,
        ENABLE_SETTINGS,
        ENABLE_ASYNC
    }
}
