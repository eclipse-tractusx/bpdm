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

package org.eclipse.tractusx.bpdm.common.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class RequestIdTaskSchedulerTest {

    @Test
    fun `each scheduled execution runs under its own request id`() {
        val scheduler = RequestIdTaskScheduler().apply { initialize() }
        val requestIds = CopyOnWriteArrayList<String?>()
        val executed = CountDownLatch(2)

        try {
            repeat(2) {
                scheduler.schedule({
                    requestIds.add(MDC.get("request"))
                    executed.countDown()
                }, Instant.now())
            }

            assertThat(executed.await(10, TimeUnit.SECONDS)).isTrue()
        } finally {
            scheduler.shutdown()
        }

        assertThat(requestIds).doesNotContainNull()
        assertThat(requestIds.toSet()).hasSize(2)
    }

    @Test
    fun `a scheduling application runs its schedules on the request id scheduler`() {
        ApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    TaskSchedulingAutoConfiguration::class.java,
                    SchedulerLoggingAutoConfiguration::class.java
                )
            )
            .withUserConfiguration(SchedulingApplication::class.java)
            .run { context ->
                assertThat(context).hasSingleBean(TaskScheduler::class.java)
                assertThat(context.getBean(TaskScheduler::class.java)).isInstanceOf(RequestIdTaskScheduler::class.java)
            }
    }

    @Configuration
    @EnableScheduling
    class SchedulingApplication
}
