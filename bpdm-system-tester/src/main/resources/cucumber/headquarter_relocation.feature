# A legal entity relocates its headquarters to one of its existing additional addresses via an IsReplacedBy
# relation. The Pool reclassifies (swaps) the two addresses: the additional address becomes the legal
# address and the former legal address becomes an additional address. This feature asserts that address
# reclassification outcome; relation reflection in the output is covered in
# output_reflects_golden_record_relations.feature.

Feature: Headquarter Relocation

  #h3. Test Objective:
  #
  #* Verify the Pool reclassifies addresses when a legal entity relocates its headquarters to an existing additional address via an IsReplacedBy relation.
  #
  #h3. Preconditions:
  #
  ## A legal entity exists with a legal address and a previously shared additional address.
  #
  #h3. Description:
  #
  ## The sharing member shares an IsReplacedBy relation from the current legal address to the additional address, effective immediately.
  ## The golden record process establishes the relation.
  ## The additional address becomes the new legal address and the former legal address becomes an additional address.
  Scenario: Legal entity's headquarters moves to a previously shared address
    Given record "old-hq" reflects legal entity "acme" with legal address "acme-old-hq"
    And record "new-hq" reflects additional address "acme-new-hq" of the existing legal entity "acme"
    When the sharing member shares relation "relocation" of type "IsReplacedBy" from "old-hq" to "new-hq" effective immediately
    And the golden record process establishes relation "relocation"
    Then record "new-hq" reflects "acme-new-hq" as legal address of "acme"
    And record "old-hq" reflects "acme-old-hq" as additional address of "acme"