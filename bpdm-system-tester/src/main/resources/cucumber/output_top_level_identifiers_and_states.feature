Feature: Output Top-Level Identifiers And States Reflect The Refined Entity

  # A Gate output record carries two top-level fields - identifiers and states - that do NOT simply mirror
  # the input. Instead they surface the identifiers and states of the golden record entity the record was
  # refined to, and which entity that is depends on what the record reflects:
  #
  #   - legal entity record    -> the LEGAL ENTITY's identifiers and states
  #   - site record            -> NO identifiers at all, and the SITE's states
  #   - additional address     -> the ADDRESS's identifiers and states
  #
  # This is a peculiar, easy-to-miss rule (note especially that a site record surfaces no top-level
  # identifiers), so it gets its own feature rather than being folded into the master data assertions. The
  # master data features ("output_reflects_own_shared_master_data.feature" and
  # "output_reflects_golden_record_master_data_changes.feature") deliberately IGNORE identifiers and states;
  # this feature is their counterpart and asserts ONLY the top-level identifiers and states, ignoring the
  # descriptive master data.

  Scenario: Legal Entity Record Surfaces The Legal Entity's Identifiers And States
    When the sharing member shares record "acme-record"
    And the golden record process refines record "acme-record" to legal entity "acme" with master data "acme-content"
    Then "acme-record" output top-level identifiers and states reflect legal entity "acme"

  Scenario: Site Record Surfaces No Identifiers And The Site's States
    When the sharing member shares record "acme-site-record"
    And the golden record process refines record "acme-site-record" to site "acme-site" of legal entity "acme" with master data "acme-site-content"
    Then "acme-site-record" output has no top-level identifiers and its states reflect site "acme-site" of legal entity "acme"

  Scenario: Additional Address Of Legal Entity Record Surfaces The Address's Identifiers And States
    When the sharing member shares record "acme-address-record"
    And the golden record process refines record "acme-address-record" to additional address "acme-branch" of legal entity "acme" with master data "acme-address-content"
    Then "acme-address-record" output top-level identifiers and states reflect additional address "acme-branch" of legal entity "acme"

  Scenario: Additional Address Of Site Record Surfaces The Address's Identifiers And States
    When the sharing member shares record "acme-site-address-record"
    And the golden record process refines record "acme-site-address-record" to additional address "acme-dock" of site "acme-site" of legal entity "acme" with master data "acme-site-address-content"
    Then "acme-site-address-record" output top-level identifiers and states reflect additional address "acme-dock" of site "acme-site" of legal entity "acme"
