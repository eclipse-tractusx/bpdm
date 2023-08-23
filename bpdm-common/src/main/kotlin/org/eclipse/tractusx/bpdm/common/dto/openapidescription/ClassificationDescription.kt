/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.common.dto.openapidescription

object ClassificationDescription {
    const val header = "A legal entity classification is an assignment of the legal entity to an industry. It does not " +
            "necessarily have to be the only industry the company is active in (see large companies " +
            "operating in different industries). Multiple assignments to several industries are possible per " +
            "classification type."

    const val value = "The name of the class belonging to the classification."
    const val code = "The identifier of the class belonging to the classification."
    const val type = "Type of classification."
}
