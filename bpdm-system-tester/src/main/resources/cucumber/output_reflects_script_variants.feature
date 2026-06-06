Feature: Output Reflects Golden Record Script Variants

  # A sharing member may attach script variants to a record. Each variant has a script code (the
  # available codes come from the Pool's script-code metadata) and carries alternative script
  # renderings of the record's text properties (legal entity, site and address text).
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

  # -- Each business partner type reflects its golden record's script variants --

  Scenario: Legal Entity Script Variants In Output
    When the sharing member shares record "acme-record"
    And the golden record process refines record "acme-record" to legal entity "acme" with script variant "acme-content"
    Then "acme-record" output reflects the script variants of legal entity "acme"

  Scenario: Site-Based Legal Entity Script Variants In Output
    When the sharing member shares record "acme-sbl-record"
    And the golden record process refines record "acme-sbl-record" to site-based legal entity "acme-sbl" with site "acme-sbl-site" with script variant "acme-sbl-content"
    Then "acme-sbl-record" output reflects the script variants of site-based legal entity "acme-sbl" with site "acme-sbl-site"

  Scenario: Site Script Variants In Output
    When the sharing member shares record "acme-site-record"
    And the golden record process refines record "acme-site-record" to site "acme-site" of legal entity "acme-site-le" with script variant "acme-site-content"
    Then "acme-site-record" output reflects the script variants of site "acme-site" of legal entity "acme-site-le"

  Scenario: Additional Address Of Legal Entity Script Variants In Output
    When the sharing member shares record "acme-address-record"
    And the golden record process refines record "acme-address-record" to additional address "acme-branch" of legal entity "acme-addr-le" with script variant "acme-address-content"
    Then "acme-address-record" output reflects the script variants of additional address "acme-branch" of legal entity "acme-addr-le"

  Scenario: Additional Address Of Site Script Variants In Output
    When the sharing member shares record "acme-site-address-record"
    And the golden record process refines record "acme-site-address-record" to additional address "acme-dock" of site "acme-sa-site" of legal entity "acme-sa-le" with script variant "acme-site-address-content"
    Then "acme-site-address-record" output reflects the script variants of additional address "acme-dock" of site "acme-sa-site" of legal entity "acme-sa-le"

  # -- Updated script variants are reflected when the record is re-shared and re-refined --

  Scenario: Updated Legal Entity Script Variants In Output
    Given record "acme-record" reflects legal entity "acme" with script variant "acme-content"
    When the sharing member updates record "acme-record"
    And the golden record process refines record "acme-record" to legal entity "acme" with script variant "acme-updated-content"
    Then "acme-record" output reflects the script variants of legal entity "acme"

  Scenario: Updated Site-Based Legal Entity Script Variants In Output
    Given record "acme-sbl-record" reflects site-based legal entity "acme-sbl" with site "acme-sbl-site" with script variant "acme-sbl-content"
    When the sharing member updates record "acme-sbl-record"
    And the golden record process refines record "acme-sbl-record" to site-based legal entity "acme-sbl" with site "acme-sbl-site" with script variant "acme-sbl-updated-content"
    Then "acme-sbl-record" output reflects the script variants of site-based legal entity "acme-sbl" with site "acme-sbl-site"

  Scenario: Updated Site Script Variants In Output
    Given record "acme-site-record" reflects site "acme-site" of legal entity "acme-site-le" with script variant "acme-site-content"
    When the sharing member updates record "acme-site-record"
    And the golden record process refines record "acme-site-record" to site "acme-site" of legal entity "acme-site-le" with script variant "acme-site-updated-content"
    Then "acme-site-record" output reflects the script variants of site "acme-site" of legal entity "acme-site-le"

  Scenario: Updated Additional Address Of Legal Entity Script Variants In Output
    Given record "acme-address-record" reflects additional address "acme-branch" of legal entity "acme-addr-le" with script variant "acme-address-content"
    When the sharing member updates record "acme-address-record"
    And the golden record process refines record "acme-address-record" to additional address "acme-branch" of legal entity "acme-addr-le" with script variant "acme-address-updated-content"
    Then "acme-address-record" output reflects the script variants of additional address "acme-branch" of legal entity "acme-addr-le"

  Scenario: Updated Additional Address Of Site Script Variants In Output
    Given record "acme-site-address-record" reflects additional address "acme-dock" of site "acme-sa-site" of legal entity "acme-sa-le" with script variant "acme-site-address-content"
    When the sharing member updates record "acme-site-address-record"
    And the golden record process refines record "acme-site-address-record" to additional address "acme-dock" of site "acme-sa-site" of legal entity "acme-sa-le" with script variant "acme-site-address-updated-content"
    Then "acme-site-address-record" output reflects the script variants of additional address "acme-dock" of site "acme-sa-site" of legal entity "acme-sa-le"

  # -- Merge: a legal entity's variant and its additional address's variant are merged in the output --

  Scenario: Output Merges The Script Variants Of A Legal Entity And Its Additional Address
    # The legal entity "acme" is shared with script code CHINESE_SIMPLIFIED (variant A) and one of its
    # additional addresses is shared with a different script code KANJI (variant B). The additional
    # address record reflects both golden records, so its output merges both variants: under
    # CHINESE_SIMPLIFIED the legal entity properties are filled and the address properties empty, and
    # under KANJI the address properties are filled and the legal entity properties empty.
    Given record "acme-le-record" reflects legal entity "acme" with script code "CHINESE_SIMPLIFIED"
    When the sharing member shares record "acme-address-record"
    And the golden record process refines record "acme-address-record" to additional address "acme-branch" with script code "KANJI" of existing legal entity "acme"
    Then "acme-address-record" output reflects the merged script variants of additional address "acme-branch" of legal entity "acme"
