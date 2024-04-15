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

package org.eclipse.tractusx.bpdm.gate.config

import org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties.Companion.PREFIX
import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties(prefix = PREFIX)
data class PermissionConfigProperties(
    val readInputPartner: String = "read_input_partner",
    val writeInputPartner: String = "write_input_partner",
    val readOutputPartner: String = "read_output_partner",
    val readInputChangelog: String = "read_input_changelog",
    val readOutputChangelog: String = "read_output_changelog",
    val readSharingState: String = "read_sharing_state",
    val writeSharingState: String = "write_sharing_state",
    val readStats: String = "read_stats"
) {
    companion object {
        const val PREFIX = "bpdm.security.permissions"

        //Keep the fully qualified name up to date here
        private const val QUALIFIED_NAME = "org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties"
        private const val BEAN_QUALIFIER = "'$PREFIX-$QUALIFIED_NAME'"

        const val READ_INPUT_PARTNER = "@$BEAN_QUALIFIER.getReadInputPartner()"
        const val WRITE_INPUT_PARTNER = "@$BEAN_QUALIFIER.getWriteInputPartner()"
        const val READ_OUTPUT_PARTNER = "@$BEAN_QUALIFIER.getReadOutputPartner()"
        const val READ_INPUT_CHANGELOG = "@$BEAN_QUALIFIER.getReadInputChangelog()"
        const val READ_OUTPUT_CHANGELOG = "@$BEAN_QUALIFIER.getReadOutputChangelog()"
        const val READ_SHARING_STATE = "@$BEAN_QUALIFIER.getReadSharingState()"
        const val WRITE_SHARING_STATE = "@$BEAN_QUALIFIER.getWriteSharingState()"
        const val READ_STATS = "@$BEAN_QUALIFIER.getReadStats()"
    }
}
