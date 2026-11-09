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

# A legal entity is succeeded by another legal entity, expressed as an IsReplacedBy relation between the
# predecessor (source) and the successor (target). Unlike the address-level IsReplacedBy, which relocates a
# legal entity's headquarters (headquarter_relocation.feature), a succession has no side effects on the
# involved legal entities: nothing is inherited and no state changes. This feature gathers the scenarios for
# legal entity successions.
@CXTPM-1093
Feature: Legal Entity Succession

  #h3. Test Objective:
  #
  #* Verify the sharing member's relation output reflects an established IsReplacedBy golden record relation between two legal entities.
  #
  #h3. Preconditions:
  #
  #* Two records each reflect a legal entity (predecessor and successor).
  #
  #h3. Description:
  #
  #* The sharing member shares an IsReplacedBy relation from the predecessor record to the successor record.
  #* The golden record process establishes the relation.
  #* The relation output reflects the established golden record relation.
  @TEST_CXTPM-1046 @BPDM
  Scenario: IsReplacedBy Relation Between Legal Entities Reflected In Sharing Member Relation Output
    Given record "predecessor-record" reflects legal entity "predecessor"
    And record "successor-record" reflects legal entity "successor"
    When the sharing member shares relation "succession" of type "IsReplacedBy" from "predecessor-record" to "successor-record"
    And the golden record process establishes relation "succession"
    Then relation "succession" output reflects the established golden record relation
