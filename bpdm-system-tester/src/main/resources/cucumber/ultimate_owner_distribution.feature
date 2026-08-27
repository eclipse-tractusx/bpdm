# This feature covers how the ultimate owner flag is distributed through the business partner
# ownership hierarchy. When a legal entity is marked as the ultimate owner, this flag should be
# reflected in the output of all entities that are owned by it (directly or indirectly).
#
# The ultimate owner resolution follows these rules:
#   - An entity marked with ownershipUltimate = true is its own ultimate owner and is identified by that flag
#     alone: its own ultimateOwnerBpnl stays empty, only the entities below it carry its BPNL
#   - An entity owned by an ultimate owner inherits that ultimate owner's BPNL
#   - Only currently valid IsOwnedBy relations are followed
#   - When no ultimate owner exists in the chain, entities report null
@CXTPM-1043
Feature: Ultimate Owner Distribution

  #h3. Test Objective:
  #
  #* Verify that when a legal entity is marked as ultimate owner, owned entities report this ultimate owner.
  #
  #h3. Preconditions:
  #
  #* A legal entity hierarchy exists with an owner and owned entity.
  #
  #h3. Description:
  #
  #* The owner entity is marked as ownershipUltimate = true in the golden record.
  #* An IsOwnedBy relation is established from the child to the parent.
  #* The golden record process processes the updates.
  #* The child entity reports the parent's BPNL as its ultimate owner.
  @TEST_CXTPM-1060 @BPDM
  Scenario: Owned entity reports ultimate owner from ownership chain
    Given record "parent-record" reflects legal entity "parent"
    And record "child-record" reflects legal entity "child"
    When the sharing member shares relation "ownership" of type "IsOwnedBy" from "child-record" to "parent-record" effective immediately
    And the golden record process establishes relation "ownership"
    And the sharing member marks "parent-record" as the ultimate owner
    And the golden record process refines record "parent-record" to legal entity "parent" as the ultimate owner
    Then "child-record" output reflects "parent-record" as the ultimate owner

  #h3. Test Objective:
  #
  #* Verify that ultimate owner flag propagates through multi-level ownership hierarchy.
  #
  #h3. Preconditions:
  #
  #* A multi-level legal entity hierarchy exists (grandparent -> parent -> child).
  #
  #h3. Description:
  #
  #* The grandparent entity is marked as ownershipUltimate = true.
  #* IsOwnedBy relations connect child -> parent -> grandparent.
  #* The golden record process processes the updates.
  #* Both parent and child report the grandparent's BPNL as ultimate owner.
  @TEST_CXTPM-1057 @BPDM
  Scenario: Ultimate owner propagates through multi-level hierarchy
    Given record "grandparent-record" reflects legal entity "grandparent"
    And record "parent-record" reflects legal entity "parent"
    And record "child-record" reflects legal entity "child"
    When the sharing member shares relation "parent-ownership" of type "IsOwnedBy" from "parent-record" to "grandparent-record" effective immediately
    And the golden record process establishes relation "parent-ownership"
    And the sharing member shares relation "child-ownership" of type "IsOwnedBy" from "child-record" to "parent-record" effective immediately
    And the golden record process establishes relation "child-ownership"
    And the sharing member marks "grandparent-record" as the ultimate owner
    And the golden record process refines record "grandparent-record" to legal entity "grandparent" as the ultimate owner
    Then "parent-record" output reflects "grandparent-record" as the ultimate owner
    And "child-record" output reflects "grandparent-record" as the ultimate owner

  #h3. Test Objective:
  #
  #* Verify that when no ultimate owner exists in the ownership chain, entities report null.
  #
  #h3. Preconditions:
  #
  #* A legal entity hierarchy exists with no entity marked as ultimate owner.
  #
  #h3. Description:
  #
  #* No entity in the hierarchy is marked as ownershipUltimate = true.
  #* IsOwnedBy relations are established between the entities.
  #* The golden record process processes the hierarchy.
  #* All entities report null as their ultimate owner.
  @TEST_CXTPM-1058 @BPDM
  Scenario: No ultimate owner reported when flag holder does not exist in chain
    Given record "parent-record" reflects legal entity "parent"
    And record "child-record" reflects legal entity "child"
    When the sharing member shares relation "ownership" of type "IsOwnedBy" from "child-record" to "parent-record" effective immediately
    And the golden record process establishes relation "ownership"
    Then "child-record" output has no ultimate owner
    And "parent-record" output has no ultimate owner

  #h3. Test Objective:
  #
  #* Verify that an entity marked as ultimate owner reports itself as the ultimate owner.
  #
  #h3. Preconditions:
  #
  #* A legal entity is marked as ownershipUltimate = true in the golden record.
  #
  #h3. Description:
  #
  #* The entity is marked as ownershipUltimate = true in the golden record.
  #* The golden record process processes the update.
  #* The entity reports its own BPNL as the ultimate owner.
  @TEST_CXTPM-1059 @BPDM
  Scenario: Entity marked as ultimate owner reports itself
    Given record "self-owner-record" reflects legal entity "self-owner"
    When the sharing member marks "self-owner-record" as the ultimate owner
    And the golden record process refines record "self-owner-record" to legal entity "self-owner" as the ultimate owner
    Then "self-owner-record" output reports itself as the ultimate owner

  #h3. Test Objective:
  #
  #* Verify that ultimate owner flag change propagates to already-owned entities.
  #
  #h3. Preconditions:
  #
  #* A legal entity hierarchy exists with no entity marked as ultimate owner initially.
  #
  #h3. Description:
  #
  #* Initially, no entity is marked as ultimate owner.
  #* IsOwnedBy relations are established.
  #* Later, the parent entity is marked as ownershipUltimate = true.
  #* The golden record process processes the update.
  #* The child entity now reports the parent's BPNL as ultimate owner.
  @TEST_CXTPM-1061 @BPDM
  Scenario: Ultimate owner flag change propagates to existing owned entities
    Given record "parent-record" reflects legal entity "parent"
    And record "child-record" reflects legal entity "child"
    When the sharing member shares relation "ownership" of type "IsOwnedBy" from "child-record" to "parent-record" effective immediately
    And the golden record process establishes relation "ownership"
    And the sharing member marks "parent-record" as the ultimate owner
    And the golden record process refines record "parent-record" to legal entity "parent" as the ultimate owner
    Then "child-record" output reflects "parent-record" as the ultimate owner
    And "parent-record" output reports itself as the ultimate owner
