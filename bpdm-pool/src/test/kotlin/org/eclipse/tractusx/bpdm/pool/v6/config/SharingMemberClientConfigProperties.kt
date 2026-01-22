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

package org.eclipse.tractusx.bpdm.pool.v6.config

import org.eclipse.tractusx.bpdm.common.util.BpdmClientProperties
import org.eclipse.tractusx.bpdm.common.util.ClientConfigurationProperties
import org.eclipse.tractusx.bpdm.pool.v6.config.SharingMemberClientConfigProperties.Companion.PREFIX
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties

@ConditionalOnProperty(name = ["test.v6"], havingValue = "true", matchIfMissing = false)
@ConfigurationProperties(prefix = PREFIX)
data class SharingMemberClientConfigProperties(
    override val baseUrl: String,
    override val securityEnabled: Boolean,
    override val registration: OAuth2ClientProperties.Registration,
    override val provider: OAuth2ClientProperties.Provider
): BpdmClientProperties{
    companion object{
        const val PREFIX = "${ClientConfigurationProperties.PREFIX}.sharing-member"
    }

    override fun getId(): String = PREFIX
}
