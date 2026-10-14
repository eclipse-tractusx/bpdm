/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.bpdm.pool.v6.metadata

import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.CountryDto
import org.eclipse.tractusx.bpdm.pool.v6.UnscheduledPoolTestBaseV6
import org.junit.jupiter.api.Test

class CountryGetV6IT : UnscheduledPoolTestBaseV6() {

    /**
     * GIVEN the country catalogue populated by Flyway
     * WHEN a client requests the first country page
     * THEN the page contains the maintained Germany entry and consistent pagination metadata
     */
    @Test
    fun `get countries returns the maintained catalogue`() {
        //GIVEN
        val pageSize = 100

        //WHEN
        val response = poolClient.metadata.getCountries(PaginationRequest(page = 0, size = pageSize))

        //THEN
        assertRepository.assertCountryPage(response, 0, pageSize)
        assertRepository.assertCountryCatalog(response.content, CountryDto("DE", "Germany", null))
    }

    /**
     * GIVEN the country catalogue contains more entries than one page
     * WHEN a client requests the second country page with one entry per page
     * THEN the response identifies page one and contains exactly one country
     */
    @Test
    fun `get countries returns the requested page`() {
        //GIVEN
        val requestedPage = 1
        val pageSize = 1

        //WHEN
        val response = poolClient.metadata.getCountries(PaginationRequest(page = requestedPage, size = pageSize))

        //THEN
        assertRepository.assertCountryPage(response, requestedPage, pageSize)
    }
}
