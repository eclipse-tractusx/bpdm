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

package org.eclipse.tractusx.bpdm.common.util

import org.springframework.boot.autoconfigure.condition.ConditionMessage
import org.springframework.boot.autoconfigure.condition.ConditionOutcome
import org.springframework.boot.autoconfigure.condition.SpringBootCondition
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.env.Environment
import org.springframework.core.type.AnnotatedTypeMetadata

class ActiveClientsConfiguredCondition : SpringBootCondition() {

    override fun getMatchOutcome(context: ConditionContext, metadata: AnnotatedTypeMetadata): ConditionOutcome {

        val enabledClients = getClientConfigurationProperties(context.environment).client.filter { it.value.securityEnabled }

        val message = ConditionMessage.forCondition("BPDM OAuth2 Clients Configured Condition", *arrayOfNulls(0))
        return if (enabledClients.isNotEmpty()) {
            ConditionOutcome.match(message.foundExactly("registered clients ${enabledClients.keys.joinToString()}"))
        } else {
            ConditionOutcome.noMatch(message.notAvailable("registered clients"))
        }
    }

    private fun getClientConfigurationProperties(environment: Environment): BpdmClientConfigProperties {
        return Binder.get(environment).bind("bpdm", BpdmClientConfigProperties::class.java).orElse(BpdmClientConfigProperties(emptyMap()))
    }

    data class BpdmClientConfigProperties(
        val client: Map<String, OAuth2Client>
    )

    data class OAuth2Client(
        val securityEnabled: Boolean,
    )
}