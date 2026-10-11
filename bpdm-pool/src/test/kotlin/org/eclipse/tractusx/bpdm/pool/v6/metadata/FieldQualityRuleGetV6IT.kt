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

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.pool.v6.UnscheduledPoolTestBaseV6
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClientResponseException

class FieldQualityRuleGetV6IT : UnscheduledPoolTestBaseV6() {

    @Test
    fun `search field quality rules for a maintained country`() {
        val rules = requireNotNull(poolClient.metadata.getFieldQualityRules("DE").body)

        assertThat(rules).isNotEmpty()
        assertThat(rules).allSatisfy { assertThat(it.country).isEqualTo("DE") }
    }

    @Test
    fun `search field quality rules rejects an unknown country by name`() {
        val error = assertThrows<WebClientResponseException.NotFound> {
            poolClient.metadata.getFieldQualityRules("ZZ")
        }

        assertThat(error.responseBodyAsString).contains("Country", "ZZ")
    }
}
