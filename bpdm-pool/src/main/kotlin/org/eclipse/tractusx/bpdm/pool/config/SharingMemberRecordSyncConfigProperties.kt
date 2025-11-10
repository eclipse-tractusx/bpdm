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

package org.eclipse.tractusx.bpdm.pool.config

import org.eclipse.tractusx.bpdm.pool.config.SharingMemberRecordSyncConfigProperties.Companion.PREFIX
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = PREFIX)
data class SharingMemberRecordSyncConfigProperties(
    val cron: String = "0/10 * * * * *",
    val batchSize: Int = 100,
){
    companion object{
        const val PREFIX = "bpdm.sharing-member-records"
        private const val QUALIFIED_NAME = "org.eclipse.tractusx.bpdm.pool.config.SharingMemberRecordSyncConfigProperties"
        private const val BEAN_QUALIFIER = "'$PREFIX-$QUALIFIED_NAME'"

        const val GET_CRON = "@$BEAN_QUALIFIER.getCron()"
    }
}
