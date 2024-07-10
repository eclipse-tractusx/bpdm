Feature: Business Partner Creation and Update
  Scenario: Sharing member creates and updates a business partner
    Given the sharing member provides valid business partner input
    When the sharing member creates the business partner
    Then the sharing member should receive a BPN for the business partner
  Scenario: Sharing member updates the business partner data
    Given the sharing member has created a business partner
    And the sharing member provides updated data for the business partner
    When the sharing member updates the business partner
    Then the sharing member should receive updated business partner data with the same BPN
    And the updated data should reflect the changes