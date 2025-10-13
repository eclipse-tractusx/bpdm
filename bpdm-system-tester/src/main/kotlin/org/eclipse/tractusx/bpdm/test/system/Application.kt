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

package org.eclipse.tractusx.bpdm.test.system

import io.cucumber.junit.Cucumber
import org.junit.runner.RunWith
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan

@RunWith(Cucumber::class)
class CucumberTestRunConfiguration

@SpringBootApplication(exclude=[DataSourceAutoConfiguration::class])
@ConfigurationPropertiesScan
class SpringApplicationConfiguration

fun main(args: Array<String>) {
    //use parallel execution default if not overwritten
    //can't use cucumber.properties for this as the properties parser currently does not support it
    //see https://github.com/cucumber/cucumber-jvm/issues/2833
    val cucumberArgs =  if(!args.contains("--threads")) args.plus(listOf("--threads", "16")) else args
    io.cucumber.core.cli.Main.main(*cucumberArgs)
}