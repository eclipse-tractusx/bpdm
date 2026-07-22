# This feature covers the "multiple sites per address" capability: a single additional address can
# belong to more than one site. When several records refine the SAME additional address, each under a
# DIFFERENT site of the SAME legal entity, the Pool merges the site memberships onto that one address
# instead of duplicating it. Each record still follows its own site, so its output's primary "site" is
# that site and the OTHER sites the address belongs to surface in the output's additional sites. The
# Pool address query independently returns the address's full site membership (its primary site plus the
# additional sites).
#
# The shape is driven with the established share -> refine flow: two (or more) records are refined to the
# same additional address label but distinct site labels under one legal entity label. Sharing the
# address label assigns the same BPN request identifier, so the Pool matches the same address golden
# record and adds each new site to it. Distinct site labels give distinct sites (and distinct site names,
# satisfying the Pool's duplicate-site-name constraint); the shared legal entity label keeps all sites
# under one legal entity, satisfying the Pool's same-legal-entity constraint for shared addresses.
#
# TODO: replace the placeholder @CXTPM-XXXX / @TEST_CXTPM-XXXX tags with the real Jira issue and
# test-case ids once they exist.
@CXTPM-XXXX
Feature: Output Reflects Additional Sites Of Address

  #h3. Test Objective:
  #
  #* Verify that when one additional address belongs to two sites, each record's output lists the other record's site as an additional site of that address.
  #
  #h3. Description:
  #
  ## The sharing member shares two records.
  ## The golden record process refines both to the same additional address, each under a different site of the same legal entity.
  ## Each record's output reflects its own site and lists the other record's site as an additional site of the shared address.
  @TEST_CXTPM-XXXX @BPDM
  Scenario: Additional Sites Of Shared Address In Output
    When the sharing member shares record "dock-a-record"
    And the golden record process refines record "dock-a-record" to additional address "shared-dock" of site "site-a" of legal entity "acme" with master data "dock-a-content"
    And the sharing member shares record "dock-b-record"
    And the golden record process refines record "dock-b-record" to additional address "shared-dock" of site "site-b" of legal entity "acme" with master data "dock-b-content"
    Then "dock-a-record" output lists the site of record "dock-b-record" as an additional site of its address
    And "dock-b-record" output lists the site of record "dock-a-record" as an additional site of its address

  #h3. Test Objective:
  #
  #* Verify the Pool address query returns the address's full site membership: its primary site plus all additional sites it belongs to.
  #
  #h3. Description:
  #
  ## The sharing member shares three records.
  ## The golden record process refines all three to the same additional address, each under a different site of the same legal entity.
  ## The Pool address query for that address returns all three sites it belongs to.
  @TEST_CXTPM-XXXX @BPDM
  Scenario: Pool Address Returns All Sites It Belongs To
    When the sharing member shares record "dock-a-record"
    And the golden record process refines record "dock-a-record" to additional address "shared-dock" of site "site-a" of legal entity "acme" with master data "dock-a-content"
    And the sharing member shares record "dock-b-record"
    And the golden record process refines record "dock-b-record" to additional address "shared-dock" of site "site-b" of legal entity "acme" with master data "dock-b-content"
    And the sharing member shares record "dock-c-record"
    And the golden record process refines record "dock-c-record" to additional address "shared-dock" of site "site-c" of legal entity "acme" with master data "dock-c-content"
    Then the Pool address of "dock-a-record" belongs to the sites of records "dock-a-record, dock-b-record, dock-c-record"

  #h3. Test Objective:
  #
  #* Verify that refining a new site onto an address that already belongs to sites adds the new site without removing the existing ones.
  #
  #h3. Preconditions:
  #
  ## An additional address already belongs to two sites of a legal entity.
  #
  #h3. Description:
  #
  ## The sharing member shares a third record.
  ## The golden record process refines it to the same additional address under a third site of the same legal entity.
  ## The first record's output still lists both the second and the third site as additional sites of the shared address.
  @TEST_CXTPM-XXXX @BPDM
  Scenario: New Site Merges Without Removing Existing Sites
    Given the sharing member shares record "dock-a-record"
    And the golden record process refines record "dock-a-record" to additional address "shared-dock" of site "site-a" of legal entity "acme" with master data "dock-a-content"
    And the sharing member shares record "dock-b-record"
    And the golden record process refines record "dock-b-record" to additional address "shared-dock" of site "site-b" of legal entity "acme" with master data "dock-b-content"
    When the sharing member shares record "dock-c-record"
    And the golden record process refines record "dock-c-record" to additional address "shared-dock" of site "site-c" of legal entity "acme" with master data "dock-c-content"
    Then "dock-a-record" output lists the sites of records "dock-b-record, dock-c-record" as additional sites of its address

  #h3. Test Objective:
  #
  #* Verify that an additional address belonging to only one site behaves as before: it lists no additional sites.
  #
  #h3. Description:
  #
  ## The sharing member shares a record.
  ## The golden record process refines it to an additional address of a single site.
  ## The record's output lists no additional sites and the Pool address belongs to just that one site.
  @TEST_CXTPM-XXXX @BPDM
  Scenario: Single Site Address Has No Additional Sites
    When the sharing member shares record "solo-dock-record"
    And the golden record process refines record "solo-dock-record" to additional address "solo-dock" of site "solo-site" of legal entity "acme" with master data "solo-dock-content"
    Then "solo-dock-record" output lists no additional sites
    And the Pool address of "solo-dock-record" belongs to a single site
