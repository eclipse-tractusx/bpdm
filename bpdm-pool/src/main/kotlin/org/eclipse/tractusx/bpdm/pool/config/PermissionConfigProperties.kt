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

package org.eclipse.tractusx.bpdm.pool.config

import org.eclipse.tractusx.bpdm.pool.config.PermissionConfigProperties.Companion.PREFIX
import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties(prefix = PREFIX)
data class PermissionConfigProperties(
    val readPartner: String = "read_partner",
    val writePartner: String = "write_partner",
    val readMetaData: String = "read_metadata",
    val writeMetaData: String = "write_metadata",
    val readMemberPartner: String = "read_partner_member",
    val readChangelog: String = "read_changelog",
    val readMemberChangelog: String = "read_changelog_member"
) {
    companion object {
        const val PREFIX = "bpdm.security.permissions"

        //Keep the fully qualified name up to date here
        private const val QUALIFIED_NAME = "org.eclipse.tractusx.bpdm.pool.config.PermissionConfigProperties"
        private const val BEAN_QUALIFIER = "'$PREFIX-$QUALIFIED_NAME'"

        const val READ_PARTNER = "@${BEAN_QUALIFIER}.getReadPartner()"
        const val WRITE_PARTNER = "@${BEAN_QUALIFIER}.getWritePartner()"
        const val READ_METADATA = "@${BEAN_QUALIFIER}.getReadMetaData()"
        const val WRITE_METADATA = "@${BEAN_QUALIFIER}.getWriteMetaData()"
        const val READ_MEMBER_PARTNER = "@${BEAN_QUALIFIER}.getReadMemberPartner()"
        const val READ_CHANGELOG = "@${BEAN_QUALIFIER}.getReadChangelog()"
        const val READ_MEMBER_CHANGELOG = "@${BEAN_QUALIFIER}.getReadMemberChangelog()"
    }
}


