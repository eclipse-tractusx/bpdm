# An address is succeeded by another address of the same legal entity, expressed as an IsReplacedBy relation between
# the predecessor (source) and the successor (target). Any pair of addresses can succeed each other, as long as the
# pair is not already a succession on a higher level: two records refined to legal entities succeed each other as
# legal entities (legal_entity_succession.feature) and two records refined to sites with their own main address as
# sites (site_succession.feature).
# Only a succession that starts at a legal entity's legal address carries a consequence, in which case it relocates
# the headquarters (headquarter_relocation.feature). Every other address succession leaves both addresses as they are:
# nothing is reclassified, nothing is inherited and no state changes. This feature gathers the scenarios for those
# successions.
@CXTPM-1043
Feature: Address Succession

  #h3. Test Objective:
  #
  #* Verify the sharing member's relation output reflects an established IsReplacedBy golden record relation between two addresses.
  #
  #h3. Preconditions:
  #
  #* Two records each reflect an additional address of the same legal entity (predecessor and successor).
  #
  #h3. Description:
  #
  #* The sharing member shares an IsReplacedBy relation from the predecessor address record to the successor address record, effective immediately.
  #* The golden record process establishes the relation between the two BPNA.
  #* The relation output reflects the established golden record relation.
  @TEST_CXTPM-1044 @BPDM
  Scenario: IsReplacedBy Relation Between Addresses Reflected In Sharing Member Relation Output
    Given record "predecessor-address-record" reflects additional address "predecessor-address" of legal entity "acme"
    And record "successor-address-record" reflects additional address "successor-address" of the existing legal entity "acme"
    When the sharing member shares relation "address-succession" of type "IsReplacedBy" from "predecessor-address-record" to "successor-address-record" effective immediately
    And the golden record process establishes relation "address-succession"
    Then relation "address-succession" output reflects the established golden record relation

  #h3. Test Objective:
  #
  #* Verify an established IsReplacedBy relation between two addresses surfaces on the address output of both involved records.
  #
  #h3. Preconditions:
  #
  #* Two records each reflect an additional address of the same legal entity (predecessor and successor).
  #
  #h3. Description:
  #
  #* The sharing member shares an IsReplacedBy relation from the predecessor address record to the successor address record, effective immediately.
  #* The golden record process establishes the relation between the two BPNA.
  #* Both records' outputs reflect the relation on their address.
  #* Both records still reflect their address as additional address, as an address succession outside the headquarters relocation reclassifies nothing.
  @TEST_CXTPM-1045 @BPDM
  Scenario: IsReplacedBy Relation Between Addresses Reflected In Address Outputs
    Given record "predecessor-address-record" reflects additional address "predecessor-address" of legal entity "acme"
    And record "successor-address-record" reflects additional address "successor-address" of the existing legal entity "acme"
    When the sharing member shares relation "address-succession" of type "IsReplacedBy" from "predecessor-address-record" to "successor-address-record" effective immediately
    And the golden record process establishes relation "address-succession"
    Then "predecessor-address-record" output reflects the address golden record relation "address-succession"
    And "successor-address-record" output reflects the address golden record relation "address-succession"
    And record "predecessor-address-record" reflects "predecessor-address" as additional address of "acme"
    And record "successor-address-record" reflects "successor-address" as additional address of "acme"
