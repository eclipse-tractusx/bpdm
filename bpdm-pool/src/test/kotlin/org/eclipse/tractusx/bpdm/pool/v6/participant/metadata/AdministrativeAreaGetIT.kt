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

package org.eclipse.tractusx.bpdm.pool.v6.participant.metadata

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.v6.participant.ParticipantTest
import org.junit.jupiter.api.Test

class AdministrativeAreaGetIT: ParticipantTest() {

    /**
     * GIVEN administrative areas level 1
     * WHEN participant requests available administrative areas
     * THEN participant gets administrative areas
     */
    @Test
    fun `search administrative areas level 1`(){
        //WHEN
        val response = poolClient.metadata.getAdminAreasLevel1(PaginationRequest())

        //THEN
        Assertions.assertThat(response.content).isNotEmpty
    }
}