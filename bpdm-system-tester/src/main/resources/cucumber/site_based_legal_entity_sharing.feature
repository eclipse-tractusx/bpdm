Feature: Share Site-Based Legal Entity

  Scenario: Sharing member shares new site-based legal entity
    When the sharing member shares a business partner record "acme-record" based on content "acme-input"
    And the golden record process refines "acme-record" to site-based legal entity "acme" with site "acme-site" and legal address "acme-hq" based on content "acme-input"
    Then record "acme-record" reaches sharing success
    And "acme-record" matches to the legal address "acme-hq" of site "acme-site" of legal entity "acme" based on content "acme-input"

  Scenario: Sharing member updates a record and the site-based legal entity is updated accordingly
    Given "acme-record" matched to the legal address "acme-hq" of site "acme-site" of legal entity "acme" based on content "acme-given"
    When the sharing member updates "acme-record" with content "acme-update"
    And the golden record process refines "acme-record" to site-based legal entity "acme" with site "acme-site" and legal address "acme-hq" based on content "acme-update"
    Then record "acme-record" reaches sharing success
    And "acme-record" matches to the legal address "acme-hq" of site "acme-site" of legal entity "acme" based on content "acme-update"

  Scenario: Sharing member shares record and record is matched to existing site-based legal entity without update
    Given Pool contains site-based legal entity "acme" with site "acme-site" and legal address "acme-hq" based on content "acme-input"
    When the sharing member shares a business partner record "acme-record" based on content "acme-other"
    And the golden record process refines "acme-record" to site-based legal entity "acme" with site "acme-site" and legal address "acme-hq" based on content "acme-input"
    Then record "acme-record" reaches sharing success
    And "acme-record" matches to the legal address "acme-hq" of site "acme-site" of legal entity "acme" based on content "acme-input"
