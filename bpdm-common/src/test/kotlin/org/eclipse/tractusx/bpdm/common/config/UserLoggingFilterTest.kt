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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.security.Principal

class UserLoggingFilterTest {

    @Test
    fun `a line break in the user name can not forge a log record`() {
        //GIVEN
        val request = requestOf("admin\r\nERROR forged line")

        //WHEN
        val loggedUserName = loggedUserNameOf(request)

        //THEN
        assertThat(loggedUserName).isEqualTo("admin__ERROR forged line")
    }

    @Test
    fun `a user name longer than the configured maximum is shortened`() {
        //GIVEN
        val request = requestOf("a".repeat(100))

        //WHEN
        val loggedUserName = loggedUserNameOf(request, LogConfigProperties(userMaxLength = 12))

        //THEN
        assertThat(loggedUserName).isEqualTo("a".repeat(12))
    }

    @Test
    fun `a user name without control characters is logged unchanged`() {
        //GIVEN
        val request = requestOf("Müller & Söhne")

        //WHEN
        val loggedUserName = loggedUserNameOf(request)

        //THEN
        assertThat(loggedUserName).isEqualTo("Müller & Söhne")
    }

    @Test
    fun `a request without a principal is logged under the configured unknown user`() {
        //GIVEN
        val request = MockHttpServletRequest("GET", "/api/catena/legal-entities")

        //WHEN
        val loggedUserName = loggedUserNameOf(request, LogConfigProperties(unknownUser = "Nobody"))

        //THEN
        assertThat(loggedUserName).isEqualTo("Nobody")
    }

    private fun requestOf(userName: String) =
        MockHttpServletRequest("GET", "/api/catena/legal-entities").apply {
            userPrincipal = Principal { userName }
        }

    private fun loggedUserNameOf(request: MockHttpServletRequest, logConfigProperties: LogConfigProperties = LogConfigProperties()): String? {
        var loggedUserName: String? = null
        val chain = FilterChain { _, _ -> loggedUserName = MDC.get(USER_CONTEXT_KEY) }

        UserLoggingFilter(logConfigProperties).doFilter(request, MockHttpServletResponse(), chain)

        return loggedUserName
    }

    companion object {
        private const val USER_CONTEXT_KEY = "user"
    }
}
