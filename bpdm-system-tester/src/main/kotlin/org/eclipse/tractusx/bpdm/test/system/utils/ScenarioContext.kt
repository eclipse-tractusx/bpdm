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

package org.eclipse.tractusx.bpdm.test.system.utils

import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.pool.api.model.SiteVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalEntityVerboseDto
import org.eclipse.tractusx.bpdm.test.testdata.pool.PoolMockDataFactory.SiteWithLegalEntityParent
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartner

class ScenarioContext {
    val siteLegalEntities: MutableMap<String, SiteBasedLegalEntity> = mutableMapOf()
    val taskData:      MutableMap<String, BusinessPartner> = mutableMapOf()
    val inputData:     MutableMap<String, BusinessPartnerInputRequest> = mutableMapOf()
    val outputData:    MutableMap<String, BusinessPartnerOutputDto> = mutableMapOf()
    val taskIds:       MutableMap<String, String> = mutableMapOf()
}

data class SiteBasedLegalEntity(
    val legalEntity: LegalEntityWithLegalAddressVerboseDto,
    val site: SiteVerboseDto
)
