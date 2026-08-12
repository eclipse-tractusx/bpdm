# A site is succeeded by another site of the same legal entity, expressed as an IsReplacedBy relation between
# the predecessor (source) and the successor (target). Both records must be refined to a site whose main
# address is its own; a site sharing its legal entity's address is shared as a legal entity relation instead.
# Like the legal entity succession (legal_entity_succession.feature) and unlike the address-level
# IsReplacedBy, which relocates a legal entity's headquarters (headquarter_relocation.feature), a site
# succession has no side effects on the involved sites: nothing is reclassified, nothing is inherited and no
# state changes, so the predecessor site keeps its addresses and its state. This feature gathers the scenarios
# for site successions.
@CXTPM-1039
Feature: Site Succession

  #h3. Test Objective:
  #
  #* Verify the sharing member's relation output reflects an established IsReplacedBy golden record relation between two sites.
  #
  #h3. Preconditions:
  #
  ## Two records each reflect a site of the same legal entity (predecessor and successor).
  #
  #h3. Description:
  #
  ## The sharing member shares an IsReplacedBy relation from the predecessor site record to the successor site record, effective immediately.
  ## The golden record process establishes the relation between the two BPNS.
  ## The relation output reflects the established golden record relation.
  @BPDM
  Scenario: IsReplacedBy Relation Between Sites Reflected In Sharing Member Relation Output
    Given record "predecessor-site-record" reflects site "predecessor-site" of legal entity "acme"
    And record "successor-site-record" reflects site "successor-site" of legal entity "acme"
    When the sharing member shares relation "site-succession" of type "IsReplacedBy" from "predecessor-site-record" to "successor-site-record" effective immediately
    And the golden record process establishes relation "site-succession"
    Then relation "site-succession" output reflects the established golden record relation

  #h3. Test Objective:
  #
  #* Verify an established IsReplacedBy relation between two sites surfaces on the site output of both involved records.
  #
  #h3. Preconditions:
  #
  ## Two records each reflect a site of the same legal entity (predecessor and successor).
  #
  #h3. Description:
  #
  ## The sharing member shares an IsReplacedBy relation from the predecessor site record to the successor site record, effective immediately.
  ## The golden record process establishes the relation between the two BPNS.
  ## Both records' outputs reflect the relation on their site.
  @BPDM
  Scenario: IsReplacedBy Relation Between Sites Reflected In Site Outputs
    Given record "predecessor-site-record" reflects site "predecessor-site" of legal entity "acme"
    And record "successor-site-record" reflects site "successor-site" of legal entity "acme"
    When the sharing member shares relation "site-succession" of type "IsReplacedBy" from "predecessor-site-record" to "successor-site-record" effective immediately
    And the golden record process establishes relation "site-succession"
    Then "predecessor-site-record" output reflects the site golden record relation "site-succession"
    And "successor-site-record" output reflects the site golden record relation "site-succession"
