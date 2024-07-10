Feature: Test Valid Generic Business Partner
  Scenario Outline: Validate business partner output based on Address Type without BPN
    Given the sharing member provides business partner input without BPN
    When the sharing member shares business partner with "<addressType>"
    Then the sharing member receives the expected address output with "<addressType>"

    Examples:
      | addressType               |
      | LegalAndSiteMainAddress    |
      | SiteMainAddress            |
      | AdditionalAddress          |