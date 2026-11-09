################################################################################
# Copyright (c) 2026 Contributors to the Eclipse Foundation
#
# See the NOTICE file(s) distributed with this work for additional
# information regarding copyright ownership.
#
# This program and the accompanying materials are made available under the
# terms of the Apache License, Version 2.0 which is available at
# https://www.apache.org/licenses/LICENSE-2.0.
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
#
# SPDX-License-Identifier: Apache-2.0
################################################################################

# Reads one page per data offer and writes nothing, so an EDC setup can be shaken out as often as it needs.
# An API this run reaches directly skips its own scenario.
#
# The read shapes differ deliberately: a POST with a body and a GET with query parameters between them exercise
# all four proxy settings of the assets (proxyPath, proxyQueryParams, proxyMethod, proxyBody).
@CXTPM-1093
@EdcAccess
Feature: Reaching The BPDM APIs Over The EDC

  #h3. Test Objective:
  #
  #* Verify a sharing member's Gate answers a read of its input over the data offer it consumes it with.
  #* Verify the offer also carries a read whose filter travels as query parameters.
  @TEST_CXTPM-1087 @BPDM @Smoke
  Scenario: Gate Input Access Over Its Data Offer
    Then the Gate input of the first sharing member answers a read over the EDC
    And the Gate input of the first sharing member answers a read with query parameters over the EDC

  #h3. Test Objective:
  #
  #* Verify a sharing member's Gate answers a read of its output over the data offer it consumes it with.
  @TEST_CXTPM-1086 @BPDM @Smoke
  Scenario: Gate Output Access Over Its Data Offer
    Then the Gate output of the first sharing member answers a read over the EDC

  #h3. Test Objective:
  #
  #* Verify a further sharing member reaches its own Gate over its own connector.
  @TEST_CXTPM-1085 @TwoSharingMembers @BPDM
  Scenario: Second Sharing Member Gate Access Over Its Data Offers
    Then the Gate input of the second sharing member answers a read over the EDC
    And the Gate input of the second sharing member answers a read with query parameters over the EDC
    And the Gate output of the second sharing member answers a read over the EDC

  #h3. Test Objective:
  #
  #* Verify the third sharing member reaches its Gate over the connector that provides the offers.
  @TEST_CXTPM-1084 @ThreeSharingMembers @BPDM
  Scenario: Third Sharing Member Gate Access Over Its Data Offers
    Then the Gate input of the third sharing member answers a read over the EDC
    And the Gate input of the third sharing member answers a read with query parameters over the EDC
    And the Gate output of the third sharing member answers a read over the EDC
