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

package org.eclipse.tractusx.bpdm.pool.v6.operator.metadata

import com.neovisionaries.i18n.CountryCode
import com.neovisionaries.i18n.LanguageCode
import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.v6.client.PoolApiClient
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LegalFormDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.request.LegalFormRequest
import org.eclipse.tractusx.bpdm.pool.v6.operator.OperatorTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.reactive.function.client.WebClientResponseException

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
class LegalFormIT @Autowired constructor(
    private val poolApiClient: PoolApiClient
): OperatorTest(){

    /**
     * WHEN operator creates valid legal form
     * THEN operator can find new legal forms in available metadata
     */
    @Test
    fun `create new legal form and find it`(){
        val request = createLegalFormRequest(testName)
        poolApiClient.metadata.createLegalForm(request)

        val expectedResponse = createExpectedLegalForm(request)

        var currentPage = 0
        var found = false
        do{
            val response = poolApiClient.metadata.getLegalForms(PaginationRequest(currentPage, 100))

            val foundLegalForm = response.content.find { it.technicalKey == request.technicalKey }
            if(foundLegalForm != null){
                Assertions.assertThat(foundLegalForm).isEqualTo(expectedResponse)
                found = true
            }

            currentPage++
        }while (response.content.isNotEmpty())

        Assertions.assertThat(found).isTrue
    }

    /**
     * WHEN operator creates valid legal form
     * THEN created legal form returned
     */
    @Test
    fun `create new legal form`(){
        val request = createLegalFormRequest(testName)
        val response = poolApiClient.metadata.createLegalForm(request)

        val expectedResponse = createExpectedLegalForm(request)
        Assertions.assertThat(response).isEqualTo(expectedResponse)
    }

    /**
     * WHEN operator tries to create legal form with existing technical key
     * THEN 409 CONFLICT returned
     */
    @Test
    fun `try create new legal form with duplicate technical key`(){
        val request = createLegalFormRequest(testName)
        poolApiClient.metadata.createLegalForm(request)

        Assertions.assertThatThrownBy { poolApiClient.metadata.createLegalForm(request) }
            .isInstanceOf(WebClientResponseException.Conflict::class.java)
    }

    /**
     * WHEN operator tries to create legal form for unknown administrative area
     * THEN 400 BAD REQUEST returned
     *
     * ToDo:
     * Currently not fulfilled!
     * Bug Ticket for tracking: https://github.com/eclipse-tractusx/bpdm/issues/1446
     */
    @Test
    @Disabled
    fun `try create new legal form with unknown admin area`(){
        val request = createLegalFormRequest(testName)
            .copy(administrativeAreaLevel1 = "UNKNOWN")

        Assertions.assertThatThrownBy { poolApiClient.metadata.createLegalForm(request) }
            .isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }



    private fun createLegalFormRequest(seed: String): LegalFormRequest {
        return LegalFormRequest(
            technicalKey = seed,
            name = "$seed ${LegalFormRequest::name.name}",
            transliteratedName = "$seed ${LegalFormRequest::transliteratedName.name}",
            abbreviation = "$seed ${LegalFormRequest::abbreviation.name}",
            transliteratedAbbreviations = "$seed ${LegalFormRequest::transliteratedAbbreviations.name}",
            country = CountryCode.DE,
            language = LanguageCode.de,
            administrativeAreaLevel1 = "DE-BW",
            isActive = true
        )
    }

    private fun createExpectedLegalForm(request: LegalFormRequest): LegalFormDto {
        return LegalFormDto(
            technicalKey = request.technicalKey,
            name = request.name,
            transliteratedName = request.transliteratedName,
            abbreviation = request.abbreviation,
            country = request.country,
            language = request.language,
            administrativeAreaLevel1 = request.administrativeAreaLevel1,
            transliteratedAbbreviations = request.transliteratedAbbreviations,
            isActive = request.isActive
        )
    }
}