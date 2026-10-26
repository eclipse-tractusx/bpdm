/*******************************************************************************
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
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.common.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class DbTimestampTest {
    @Test
    fun `timestamps truncate to database precision and compare by value`() {
        val timestamp = Instant.parse("2026-01-01T00:00:00.123456789Z").toTimestamp()
        val sameTimestamp = Instant.parse("2026-01-01T00:00:00.123456Z").toTimestamp()

        assertThat(timestamp.instant).isEqualTo(sameTimestamp.instant)
        assertThat(timestamp).isEqualTo(sameTimestamp)
        assertThat(timestamp.hashCode()).isEqualTo(sameTimestamp.hashCode())
        assertThat(timestamp.compareTo(sameTimestamp)).isZero()
        assertThat(timestamp).isLessThan(Instant.parse("2026-01-01T00:00:00.123457Z").toTimestamp())
    }

    @Test
    fun `new entities start with identical creation and update timestamps`() {
        val entity = object : BaseEntityWithExplicitTimestamps() {}

        assertThat(entity.updatedAt).isEqualTo(entity.createdAt)
        assertThat(entity.createdAt.instant.nano % 1000).isZero()
    }
}
