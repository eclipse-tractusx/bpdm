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
import com.neovisionaries.i18n.LanguageCode
import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.LegalFormDto
import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalFormRequest
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClientResponseException

class LegalFormCreateV7IT : UnscheduledPoolTestBaseV7() {

    /**
     * WHEN operator creates valid legal form
     * THEN created legal form is returned
     */
    @Test
    fun `create new legal form v7`() {
        //WHEN
        val request = createLegalFormRequest(testName)
        val response = poolClient.metadata.createLegalForm(request)

        //THEN
        Assertions.assertThat(response).isEqualTo(createExpectedLegalForm(request))
    }

    /**
     * WHEN operator creates valid legal form
     * THEN operator can find new legal form in available metadata
     */
    @Test
    fun `create new legal form and find it v7`() {
        //WHEN
        val request = createLegalFormRequest(testName)
        poolClient.metadata.createLegalForm(request)

        val expectedResponse = createExpectedLegalForm(request)

        //THEN
        var currentPage = 0
        var found = false
        do {
            val response = poolClient.metadata.getLegalForms(PaginationRequest(currentPage, 100))

            val foundLegalForm = response.content.find { it.technicalKey == request.technicalKey }
            if (foundLegalForm != null) {
                Assertions.assertThat(foundLegalForm).isEqualTo(expectedResponse)
                found = true
            }

            currentPage++
        } while (response.content.isNotEmpty())

        Assertions.assertThat(found).isTrue
    }

    /**
     * WHEN operator tries to create legal form with existing technical key
     * THEN 409 CONFLICT returned
     */
    @Test
    fun `try create new legal form with duplicate technical key v7`() {
        //WHEN
        val request = createLegalFormRequest(testName)
        poolClient.metadata.createLegalForm(request)

        //THEN
        Assertions.assertThatThrownBy { poolClient.metadata.createLegalForm(request) }
            .isInstanceOf(WebClientResponseException.Conflict::class.java)
    }

    /**
     * WHEN operator tries to create legal form for unknown administrative area
     * THEN 400 BAD REQUEST returned
     */
    @Test
    fun `try create new legal form with unknown admin area v7`() {
        //WHEN
        val request = createLegalFormRequest(testName).copy(administrativeAreaLevel1 = "UNKNOWN")

        //THEN
        Assertions.assertThatThrownBy { poolClient.metadata.createLegalForm(request) }
            .isInstanceOf(WebClientResponseException.BadRequest::class.java)
    }

    private fun createLegalFormRequest(seed: String): LegalFormRequest {
        return LegalFormRequest(
            technicalKey = seed,
            name = "$seed ${LegalFormRequest::name.name}",
            transliteratedName = "$seed ${LegalFormRequest::transliteratedName.name}",
            abbreviations = "$seed ${LegalFormRequest::abbreviations.name}",
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
            abbreviations = request.abbreviations,
            country = request.country,
            language = request.language,
            administrativeAreaLevel1 = request.administrativeAreaLevel1,
            transliteratedAbbreviations = request.transliteratedAbbreviations,
            isActive = request.isActive
        )
    }
}
