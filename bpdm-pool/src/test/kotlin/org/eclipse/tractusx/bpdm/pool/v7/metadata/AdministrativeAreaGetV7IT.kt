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

package org.eclipse.tractusx.bpdm.pool.v7.metadata

import com.neovisionaries.i18n.CountryCode
import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.junit.jupiter.api.Test

class AdministrativeAreaGetV7IT : UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN administrative areas level 1
     * WHEN participant requests available administrative areas
     * THEN participant gets administrative areas
     */
    @Test
    fun `search administrative areas level 1`() {
        //WHEN
        val response = poolClient.metadata.getAdminAreasLevel1(PaginationRequest())

        //THEN
        Assertions.assertThat(response.content).isNotEmpty
    }

    /**
     * GIVEN administrative areas level 1
     * WHEN participant searches with page size 1
     * THEN participant gets exactly one administrative area
     */
    @Test
    fun `search administrative areas level 1 with pagination`() {
        //WHEN
        val response = poolClient.metadata.getAdminAreasLevel1(PaginationRequest(page = 0, size = 1))

        //THEN
        Assertions.assertThat(response.content).hasSize(1)
        Assertions.assertThat(response.totalElements).isGreaterThan(1)
    }

    /**
     * GIVEN administrative areas level 1
     * WHEN participant requests administrative areas
     * THEN all areas have valid country codes and non-blank codes
     */
    @Test
    fun `search administrative areas level 1 returns valid entries`() {
        //WHEN
        val response = poolClient.metadata.getAdminAreasLevel1(PaginationRequest(size = 100))

        //THEN
        response.content.forEach { area ->
            Assertions.assertThat(area.countryCode).isNotNull
            Assertions.assertThat(area.countryCode).isIn(*CountryCode.entries.toTypedArray())
            Assertions.assertThat(area.code).isNotBlank
            Assertions.assertThat(area.name).isNotBlank
        }
    }
}
