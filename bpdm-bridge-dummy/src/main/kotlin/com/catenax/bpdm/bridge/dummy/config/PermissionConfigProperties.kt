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

package com.catenax.bpdm.bridge.dummy.config

import com.catenax.bpdm.bridge.dummy.config.PermissionConfigProperties.Companion.PREFIX
import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties(prefix = PREFIX)
class PermissionConfigProperties(
    @Suppress("unused") val sync: String = "sync_company_data"
) {
    companion object {
        const val PREFIX = "bpdm.security.permissions"
        private const val QUALIFIED_NAME = "com.catenax.bpdm.bridge.dummy.config.PermissionConfigProperties"
        private const val BEAN_QUALIFIER = "'$PREFIX-$QUALIFIED_NAME'"

        const val SYNC = "@$BEAN_QUALIFIER.getSync()"
    }
}
