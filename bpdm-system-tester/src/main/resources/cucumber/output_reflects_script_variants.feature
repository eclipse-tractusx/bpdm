# A sharing member may attach script variants to a record. Each variant has a script code (the
# available codes come from the Pool's script-code metadata) and carries alternative script
# versions of the record's text properties (legal entity, site and address text).
#
# The golden record process refines the shared script variants onto the resulting golden record and
# its parents. In these scenarios the process applies a variant to the matched entity AND to the
# parents it determines (e.g. an additional address record may also carry variant information for its
# site and/or legal entity parent, which those parents then receive). This is a behaviour of these
# test scenarios, not a standardised behaviour of every golden record process implementation - other
# implementations may only keep the portion that matches the refined business partner type.
#
# What IS standardised, and what these scenarios assert, is the OUTPUT: after sharing completes, the
# record's output reflects the MERGED script variants of all the golden records it reflects (the
# matched address and its parents), keyed by script code. When a script code is present on only one of
# those golden records, the output variant for that code fills only that entity's properties and
# leaves the others empty. For example, a legal entity carrying variant A and its additional address
# carrying variant B yield two output variants: A with the legal entity properties filled and the
# address properties empty, and B with the address properties filled and the legal entity properties
# empty.
#
# Only script variants are compared here; master data, identifiers, states, BPNs and confidence
# criteria are covered by their own features. The "script variant" argument of the refine steps is the
# seed that determines the generated variant content (a different seed produces different variants).
@CXTPM-1039
Feature: Output Reflects Golden Record Script Variants

  # -- Each business partner type reflects its golden record's script variants --

  #h3. Test Objective:
  #
  #* Verify a record refined to a legal entity reflects that legal entity's script variants in its output.
  #
  #h3. Description:
  #
  ## The sharing member shares a record.
  ## The golden record process refines it to a legal entity with a script variant.
  ## The record's output reflects the legal entity's script variants.
  @TEST_CXTPM-1022 @BPDM
  Scenario: Legal Entity Script Variants In Output
    When the sharing member shares record "acme-record"
    And the golden record process refines record "acme-record" to legal entity "acme" with script variant "acme-content"
    Then "acme-record" output reflects the script variants of legal entity "acme"

  #h3. Test Objective:
  #
  #* Verify a record refined to a site-based legal entity reflects its script variants in its output.
  #
  #h3. Description:
  #
  ## The sharing member shares a record.
  ## The golden record process refines it to a site-based legal entity with a site and a script variant.
  ## The record's output reflects the site-based legal entity's script variants.
  @TEST_CXTPM-1023 @BPDM
  Scenario: Site-Based Legal Entity Script Variants In Output
    When the sharing member shares record "acme-sbl-record"
    And the golden record process refines record "acme-sbl-record" to site-based legal entity "acme-sbl" with site "acme-sbl-site" with script variant "acme-sbl-content"
    Then "acme-sbl-record" output reflects the script variants of site-based legal entity "acme-sbl" with site "acme-sbl-site"

  #h3. Test Objective:
  #
  #* Verify a record refined to a site reflects that site's script variants in its output.
  #
  #h3. Description:
  #
  ## The sharing member shares a record.
  ## The golden record process refines it to a site of a legal entity with a script variant.
  ## The record's output reflects the site's script variants.
  @TEST_CXTPM-1020 @BPDM
  Scenario: Site Script Variants In Output
    When the sharing member shares record "acme-site-record"
    And the golden record process refines record "acme-site-record" to site "acme-site" of legal entity "acme-site-le" with script variant "acme-site-content"
    Then "acme-site-record" output reflects the script variants of site "acme-site" of legal entity "acme-site-le"

  #h3. Test Objective:
  #
  #* Verify a record refined to an additional address of a legal entity reflects its script variants in its output.
  #
  #h3. Description:
  #
  ## The sharing member shares a record.
  ## The golden record process refines it to an additional address of a legal entity with a script variant.
  ## The record's output reflects the additional address's script variants.
  @TEST_CXTPM-1028 @BPDM
  Scenario: Additional Address Of Legal Entity Script Variants In Output
    When the sharing member shares record "acme-address-record"
    And the golden record process refines record "acme-address-record" to additional address "acme-branch" of legal entity "acme-addr-le" with script variant "acme-address-content"
    Then "acme-address-record" output reflects the script variants of additional address "acme-branch" of legal entity "acme-addr-le"

  #h3. Test Objective:
  #
  #* Verify a record refined to an additional address of a site reflects its script variants in its output.
  #
  #h3. Description:
  #
  ## The sharing member shares a record.
  ## The golden record process refines it to an additional address of a site with a script variant.
  ## The record's output reflects the additional address's script variants.
  @TEST_CXTPM-1024 @BPDM
  Scenario: Additional Address Of Site Script Variants In Output
    When the sharing member shares record "acme-site-address-record"
    And the golden record process refines record "acme-site-address-record" to additional address "acme-dock" of site "acme-sa-site" of legal entity "acme-sa-le" with script variant "acme-site-address-content"
    Then "acme-site-address-record" output reflects the script variants of additional address "acme-dock" of site "acme-sa-site" of legal entity "acme-sa-le"

  # -- Updated script variants are reflected when the record is re-shared and re-refined --

  #h3. Test Objective:
  #
  #* Verify a record's output reflects updated legal entity script variants after the record is re-shared.
  #
  #h3. Preconditions:
  #
  ## A record already reflects a legal entity with its script variant.
  #
  #h3. Description:
  #
  ## The sharing member updates the record.
  ## The golden record process refines it to the same legal entity with an updated script variant.
  ## The record's output reflects the updated legal entity script variants.
  @TEST_CXTPM-1027 @BPDM
  Scenario: Updated Legal Entity Script Variants In Output
    Given record "acme-record" reflects legal entity "acme" with script variant "acme-content"
    When the sharing member updates record "acme-record"
    And the golden record process refines record "acme-record" to legal entity "acme" with script variant "acme-updated-content"
    Then "acme-record" output reflects the script variants of legal entity "acme"

  #h3. Test Objective:
  #
  #* Verify a record's output reflects updated site-based legal entity script variants after the record is re-shared.
  #
  #h3. Preconditions:
  #
  ## A record already reflects a site-based legal entity with a site and its script variant.
  #
  #h3. Description:
  #
  ## The sharing member updates the record.
  ## The golden record process refines it to the same site-based legal entity with an updated script variant.
  ## The record's output reflects the updated site-based legal entity script variants.
  @TEST_CXTPM-1026 @BPDM
  Scenario: Updated Site-Based Legal Entity Script Variants In Output
    Given record "acme-sbl-record" reflects site-based legal entity "acme-sbl" with site "acme-sbl-site" with script variant "acme-sbl-content"
    When the sharing member updates record "acme-sbl-record"
    And the golden record process refines record "acme-sbl-record" to site-based legal entity "acme-sbl" with site "acme-sbl-site" with script variant "acme-sbl-updated-content"
    Then "acme-sbl-record" output reflects the script variants of site-based legal entity "acme-sbl" with site "acme-sbl-site"

  #h3. Test Objective:
  #
  #* Verify a record's output reflects updated site script variants after the record is re-shared.
  #
  #h3. Preconditions:
  #
  ## A record already reflects a site of a legal entity with its script variant.
  #
  #h3. Description:
  #
  ## The sharing member updates the record.
  ## The golden record process refines it to the same site with an updated script variant.
  ## The record's output reflects the updated site script variants.
  @TEST_CXTPM-1029 @BPDM
  Scenario: Updated Site Script Variants In Output
    Given record "acme-site-record" reflects site "acme-site" of legal entity "acme-site-le" with script variant "acme-site-content"
    When the sharing member updates record "acme-site-record"
    And the golden record process refines record "acme-site-record" to site "acme-site" of legal entity "acme-site-le" with script variant "acme-site-updated-content"
    Then "acme-site-record" output reflects the script variants of site "acme-site" of legal entity "acme-site-le"

  #h3. Test Objective:
  #
  #* Verify a record's output reflects updated additional address script variants after the record is re-shared.
  #
  #h3. Preconditions:
  #
  ## A record already reflects an additional address of a legal entity with its script variant.
  #
  #h3. Description:
  #
  ## The sharing member updates the record.
  ## The golden record process refines it to the same additional address with an updated script variant.
  ## The record's output reflects the updated additional address script variants.
  @TEST_CXTPM-1021 @BPDM
  Scenario: Updated Additional Address Of Legal Entity Script Variants In Output
    Given record "acme-address-record" reflects additional address "acme-branch" of legal entity "acme-addr-le" with script variant "acme-address-content"
    When the sharing member updates record "acme-address-record"
    And the golden record process refines record "acme-address-record" to additional address "acme-branch" of legal entity "acme-addr-le" with script variant "acme-address-updated-content"
    Then "acme-address-record" output reflects the script variants of additional address "acme-branch" of legal entity "acme-addr-le"

  #h3. Test Objective:
  #
  #* Verify a record's output reflects updated script variants of an additional address of a site after the record is re-shared.
  #
  #h3. Preconditions:
  #
  ## A record already reflects an additional address of a site with its script variant.
  #
  #h3. Description:
  #
  ## The sharing member updates the record.
  ## The golden record process refines it to the same additional address with an updated script variant.
  ## The record's output reflects the updated additional address script variants.
  @TEST_CXTPM-1030 @BPDM
  Scenario: Updated Additional Address Of Site Script Variants In Output
    Given record "acme-site-address-record" reflects additional address "acme-dock" of site "acme-sa-site" of legal entity "acme-sa-le" with script variant "acme-site-address-content"
    When the sharing member updates record "acme-site-address-record"
    And the golden record process refines record "acme-site-address-record" to additional address "acme-dock" of site "acme-sa-site" of legal entity "acme-sa-le" with script variant "acme-site-address-updated-content"
    Then "acme-site-address-record" output reflects the script variants of additional address "acme-dock" of site "acme-sa-site" of legal entity "acme-sa-le"

  # -- Merge: a legal entity's variant and its additional address's variant are merged in the output --

  #h3. Test Objective:
  #
  #* Verify a record's output merges the script variants of all golden records it reflects, keyed by script code, filling only the properties owned by each entity.
  #
  #h3. Preconditions:
  #
  ## A legal entity is shared with one script code, filling its legal entity properties.
  #
  #h3. Description:
  #
  ## The sharing member shares a record for one of the legal entity's additional addresses with a different script code.
  ## The golden record process refines it to that additional address of the existing legal entity.
  ## The address record's output merges both variants: each script code fills only its own entity's properties and leaves the others empty.
  @TEST_CXTPM-1025 @BPDM
  Scenario: Output Merges The Script Variants Of A Legal Entity And Its Additional Address
    Given record "acme-le-record" reflects legal entity "acme" with script code "CHINESE_SIMPLIFIED"
    When the sharing member shares record "acme-address-record"
    And the golden record process refines record "acme-address-record" to additional address "acme-branch" with script code "KANJI" of existing legal entity "acme"
    Then "acme-address-record" output reflects the merged script variants of additional address "acme-branch" of legal entity "acme"
