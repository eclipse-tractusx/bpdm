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

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.withLoggingContext
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration
import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder
import org.springframework.context.annotation.Bean
import org.springframework.core.task.TaskDecorator
import org.springframework.scheduling.Trigger
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.HtmlUtils
import java.time.Duration
import java.time.Instant
import java.util.*

private const val REQUEST_CONTEXT_KEY = "request"

@Component
class UserLoggingFilter(
    private val logConfigProperties: LogConfigProperties
) : OncePerRequestFilter() {

    // Health and metrics endpoints are polled by Kubernetes probes and by the services' own dependency checks, at a
    // rate that drowns out every other request in the log.
    override fun shouldNotFilter(request: HttpServletRequest) =
        request.requestURI.startsWith(ACTUATOR_PATH)

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val userName = request.userPrincipal?.name ?: logConfigProperties.unknownUser
        val escapedUserName = HtmlUtils.htmlEscape(userName)
        val escapedRequest = HtmlUtils.htmlEscape(request.requestURI)
        val escapedMethod = HtmlUtils.htmlEscape(request.method)

        withLoggingContext(
            "user" to escapedUserName,
        ) {
            val startedAt = System.nanoTime()
            try {
                filterChain.doFilter(request, response)
            } finally {
                if (logger.isDebugEnabled) {
                    val durationMillis = (System.nanoTime() - startedAt) / 1_000_000
                    logger.debug(
                        "User '$escapedUserName' requested $escapedMethod $escapedRequest: " +
                                "${response.status} in ${durationMillis}ms"
                    )
                }
            }
        }
    }

    companion object {
        private const val ACTUATOR_PATH = "/actuator"
    }
}

@Component
class RequestLoggingFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val requestId = UUID.randomUUID().toString()

        withLoggingContext(
            REQUEST_CONTEXT_KEY to requestId
        ) {
            filterChain.doFilter(request, response)
        }
    }
}

/**
 * Util class for copying the Mapped Diagnostic Context from an invoking thread to the invoked thread
 */
@Component
class MdcTaskDecorator : TaskDecorator {

    private val emptyContext = HashMap<String, String>()

    override fun decorate(runnable: Runnable): Runnable {
        val mdcCopy = MDC.getCopyOfContextMap() ?: emptyContext

        return Runnable {
            withLoggingContext(mdcCopy) {
                runnable.run()
            }
        }
    }
}

/**
 * Gives each scheduled execution its own request id, so that the lines one run writes correlate the way an API
 * request's do.
 *
 * Every schedule in a BPDM service runs on this one scheduler, whether it was registered by `@Scheduled` or against the
 * injected `TaskScheduler`.
 */
class RequestIdTaskScheduler : ThreadPoolTaskScheduler() {

    // Spring 6.1 offers no task decorator on this scheduler, so each scheduling method wraps its task itself. The
    // overrides collapse into `setTaskDecorator` once the framework supports it.
    override fun schedule(task: Runnable, trigger: Trigger) = super.schedule(withRequestId(task), trigger)

    override fun schedule(task: Runnable, startTime: Instant) = super.schedule(withRequestId(task), startTime)

    override fun scheduleAtFixedRate(task: Runnable, startTime: Instant, period: Duration) =
        super.scheduleAtFixedRate(withRequestId(task), startTime, period)

    override fun scheduleAtFixedRate(task: Runnable, period: Duration) = super.scheduleAtFixedRate(withRequestId(task), period)

    override fun scheduleWithFixedDelay(task: Runnable, startTime: Instant, delay: Duration) =
        super.scheduleWithFixedDelay(withRequestId(task), startTime, delay)

    override fun scheduleWithFixedDelay(task: Runnable, delay: Duration) = super.scheduleWithFixedDelay(withRequestId(task), delay)

    private fun withRequestId(task: Runnable) =
        Runnable {
            withLoggingContext(REQUEST_CONTEXT_KEY to UUID.randomUUID().toString()) {
                task.run()
            }
        }
}

/**
 * Configures every BPDM service to schedule on a scheduler that stamps a request id.
 */
@AutoConfiguration(before = [TaskSchedulingAutoConfiguration::class])
class SchedulerLoggingAutoConfiguration {

    /**
     * Provides the scheduler that carries a request id, in place of the plain one Spring Boot configures by default.
     */
    @Bean
    fun taskScheduler(builder: ThreadPoolTaskSchedulerBuilder): ThreadPoolTaskScheduler =
        builder.configure(RequestIdTaskScheduler())
}