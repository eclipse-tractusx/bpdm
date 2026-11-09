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

# The countries a golden record can be located in are the ones the Pool's country catalogue lists. The Gate
# no longer knows that catalogue: it takes a business partner with any country code, and the golden record
# process rejects one whose country the Pool does not support.
@CXTPM-1093
Feature: Country Catalogue Applies In The Golden Record Process

  #h3. Test Objective:
  #
  #* Verify a business partner whose country the Pool does not support is accepted by the Gate and rejected in the golden record process.
  #
  #h3. Description:
  #
  #* The sharing member shares a record with an unsupported country.
  #* The golden record process refines it to a legal entity with that unsupported country.
  #* The record's sharing fails in the golden record process.
  @TEST_CXTPM-1083 @BPDM
  Scenario: Unsupported Country Is Rejected In The Golden Record Process
    When the sharing member shares record "nowhere-record" with an unsupported country
    And the golden record process refines record "nowhere-record" to legal entity "nowhere" with an unsupported country
    Then "nowhere-record" sharing fails in the golden record process
