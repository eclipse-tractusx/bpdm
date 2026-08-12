# A legal entity is succeeded by another legal entity, expressed as an IsReplacedBy relation between the
# predecessor (source) and the successor (target). Unlike the address-level IsReplacedBy, which relocates a
# legal entity's headquarters (headquarter_relocation.feature), a succession has no side effects on the
# involved legal entities: nothing is inherited and no state changes. This feature gathers the scenarios for
# legal entity successions.
@CXTPM-1039
Feature: Legal Entity Succession

  #h3. Test Objective:
  #
  #* Verify the sharing member's relation output reflects an established IsReplacedBy golden record relation between two legal entities.
  #
  #h3. Preconditions:
  #
  ## Two records each reflect a legal entity (predecessor and successor).
  #
  #h3. Description:
  #
  ## The sharing member shares an IsReplacedBy relation from the predecessor record to the successor record.
  ## The golden record process establishes the relation.
  ## The relation output reflects the established golden record relation.
  @BPDM
  Scenario: IsReplacedBy Relation Between Legal Entities Reflected In Sharing Member Relation Output
    Given record "predecessor-record" reflects legal entity "predecessor"
    And record "successor-record" reflects legal entity "successor"
    When the sharing member shares relation "succession" of type "IsReplacedBy" from "predecessor-record" to "successor-record"
    And the golden record process establishes relation "succession"
    Then relation "succession" output reflects the established golden record relation
