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

package org.eclipse.tractusx.bpdm.gate.v6.util

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.springframework.stereotype.Component

/**
 * Offers util functionalities for comparing complex Gate API V6 objects under test
 */
@Component
class GateAssertRepositoryV6 {

    fun assertBusinessPartnerInput(actual: Collection<BusinessPartnerInputDto>, expected: Collection<BusinessPartnerInputDto>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                BusinessPartnerInputDto::createdAt.name,
                BusinessPartnerInputDto::updatedAt.name
            )
            .isEqualTo(expected)
    }

    fun assertBusinessPartnerInput(actual: PageDto<BusinessPartnerInputDto>, expected: PageDto<BusinessPartnerInputDto>){
        assertPageHeader(actual, expected)
        assertBusinessPartnerInput(actual.content, expected.content)
    }

    private fun assertPageHeader(actual: PageDto<*>, expected: PageDto<*>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                PageDto<*>::content.name
            )
            .isEqualTo(expected)
    }

}