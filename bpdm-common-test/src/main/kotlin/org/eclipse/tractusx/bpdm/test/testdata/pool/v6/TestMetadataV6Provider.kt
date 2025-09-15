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

package org.eclipse.tractusx.bpdm.test.testdata.pool.v6

import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.v6.client.PoolApiClient as PoolApiClientV6

class TestMetadataV6Provider(
    private val poolClientV6: PoolApiClientV6,
    private val poolClient: PoolApiClient
) {

    fun createTestMetadata(): TestMetadataV6{
        val paginationRequest = PaginationRequest(page = 0, size = 100)

        val legalFormPage = poolClientV6.metadata.getLegalForms(paginationRequest)
        val legalEntityIdentifierPage = poolClientV6.metadata.getIdentifierTypes(paginationRequest, IdentifierBusinessPartnerType.LEGAL_ENTITY, null)
        val addressIdentifierPage =  poolClientV6.metadata.getIdentifierTypes(paginationRequest, IdentifierBusinessPartnerType.ADDRESS, null)
        val adminAreas = poolClient.metadata.getAdminAreasLevel1(paginationRequest)

        return TestMetadataV6(
            legalFormPage.content.toList(),
            legalEntityIdentifierPage.content.toList(),
            addressIdentifierPage.content.toList(),
            adminAreas.content.toList()
        )
    }

}