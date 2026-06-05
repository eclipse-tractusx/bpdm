Feature: Share Additional Address Of Site

  Scenario: Sharing member shares new additional address of site
    When the sharing member shares a business partner record "acme-add-record" based on content "acme-input"
    And the golden record process refines "acme-add-record" to additional address "acme-add" of site "acme-site" of legal entity "acme" based on content "acme-input"
    Then record "acme-add-record" reaches sharing success
    And "acme-add-record" matches to the additional address "acme-add" of site "acme-site" of legal entity "acme" based on content "acme-input"

  Scenario: Sharing member updates a record and the additional address is updated accordingly
    Given "acme-add-record" matched to the additional address "acme-add" of site "acme-site" of legal entity "acme" based on content "acme-given"
    When the sharing member updates "acme-add-record" with content "acme-update"
    And the golden record process refines "acme-add-record" to additional address "acme-add" of site "acme-site" of legal entity "acme" based on content "acme-update"
    Then record "acme-add-record" reaches sharing success
    And "acme-add-record" matches to the additional address "acme-add" of site "acme-site" of legal entity "acme" based on content "acme-update"

  Scenario: Sharing member shares record and record is matched to existing additional address without update
    Given Pool contains additional address "acme-add" of site "acme-site" of legal entity "acme" based on content "acme-input"
    When the sharing member shares a business partner record "acme-add-record" based on content "acme-other"
    And the golden record process refines "acme-add-record" to additional address "acme-add" of site "acme-site" of legal entity "acme" based on content "acme-input"
    Then record "acme-add-record" reaches sharing success
    And "acme-add-record" matches to the additional address "acme-add" of site "acme-site" of legal entity "acme" based on content "acme-input"
