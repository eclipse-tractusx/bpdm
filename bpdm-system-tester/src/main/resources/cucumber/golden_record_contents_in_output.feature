Feature: Output Reflects Golden Record Master Data

  # "master data" here means the descriptive legal entity, site and address attributes:
  # legal name, short name, legal form, site name, address name, address type and postal addresses.
  # It deliberately excludes identifiers, states, BPNs, confidence criteria and golden record
  # relations, which are covered by dedicated tests.

  Scenario: Legal Entity Master Data In Output
    When the sharing member shares record "acme-record"
    And the golden record process refines record "acme-record" to legal entity "acme" with master data "acme-content"
    Then "acme-record" output reflects legal entity "acme" in its master data

  Scenario: Updated Legal Entity Master Data In Output
    Given record "acme-record" reflects legal entity "acme" with master data "acme-content"
    When the sharing member updates record "acme-record"
    And the golden record process refines record "acme-record" to legal entity "acme" with master data "acme-updated-content"
    Then "acme-record" output reflects legal entity "acme" in its master data

  Scenario: Additional Address Of Legal Entity Master Data In Output
    When the sharing member shares record "acme-address-record"
    And the golden record process refines record "acme-address-record" to additional address "acme-branch" of legal entity "acme" with master data "acme-address-content"
    Then "acme-address-record" output reflects additional address "acme-branch" of legal entity "acme" in its master data
