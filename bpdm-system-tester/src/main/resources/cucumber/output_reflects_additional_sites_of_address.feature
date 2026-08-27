# This feature covers the "multiple sites per address" capability: a single additional address can
# belong to more than one site. When several records refine the SAME additional address, each under a
# DIFFERENT site of the SAME legal entity, that one address belongs to all of those sites instead of
# being duplicated. Each record still follows its own site, so its output's primary "site" is that site
# and the OTHER sites the address belongs to surface in the output's additional sites. The Pool address
# query independently returns the address's full site membership (its primary site plus the additional
# sites).
#
# The site memberships of an address are a full upsert: the golden record process hands the Pool the
# COMPLETE set of sites of the address and the Pool applies exactly that, so a site the set leaves out is
# unlinked. Building that set is the process's job, not the Pool's - it is the one that sees the whole
# stream of records. The test's refinement stands in for that: it keeps a ledger of which record puts
# which site on which address and consolidates it per task.
#
# The shape is driven with the established share -> refine flow: two (or more) records are refined to the
# same address label but distinct site labels under one legal entity label. Sharing the address label
# assigns the same BPN request identifier, so the Pool matches the same address golden record. Distinct
# site labels give distinct sites (and distinct site names, satisfying the Pool's duplicate-site-name
# constraint); the shared legal entity label keeps all sites under one legal entity, satisfying the
# Pool's same-legal-entity constraint for shared addresses.
#
# The shared address can be an ADDITIONAL address of the sites (first scenarios) or the sites' MAIN
# address (last scenario): the golden record flow lets a new site adopt an already-existing address as
# its main address, so several sites can share one main address.
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
  #* Verify that a new site on an address that already belongs to sites joins them, because the complete set the golden record process states keeps the sites already there.
  #
  #h3. Preconditions:
  #
  ## An additional address already belongs to two sites of a legal entity.
  #
  #h3. Description:
  #
  ## The sharing member shares a third record.
  ## The golden record process refines it to the same additional address under a third site of the same legal entity.
  ## The first record's output lists both the second and the third site as additional sites of the shared address.
  @TEST_CXTPM-XXXX @BPDM
  Scenario: New Site Joins The Sites Of An Address
    Given the sharing member shares record "dock-a-record"
    And the sharing member shares record "dock-b-record"
    And the golden record process refines record "dock-a-record" to additional address "shared-dock" of site "site-a" of legal entity "acme" with master data "dock-a-content"
    And the golden record process refines record "dock-b-record" to additional address "shared-dock" of site "site-b" of legal entity "acme" with master data "dock-b-content"
    When the sharing member shares record "dock-c-record"
    And the golden record process refines record "dock-c-record" to additional address "shared-dock" of site "site-c" of legal entity "acme" with master data "dock-c-content"
    Then "dock-a-record" output lists the sites of records "dock-b-record, dock-c-record" as additional sites of its address

  #h3. Test Objective:
  #
  #* Verify that a site the golden record process no longer states for an address comes off that address.
  #
  #h3. Preconditions:
  #
  ## An additional address already belongs to two sites of a legal entity.
  #
  #h3. Description:
  #
  ## The sharing member updates the second record.
  ## The golden record process refines it to the same additional address, but under a different site than before.
  ## The address belongs to the first record's site and to the second record's new site alone - the site it no longer uses is gone.
  @TEST_CXTPM-XXXX @BPDM
  Scenario: Site No Longer Stated Comes Off The Address
    Given the sharing member shares record "dock-a-record"
    And the sharing member shares record "dock-b-record"
    And the golden record process refines record "dock-a-record" to additional address "shared-dock" of site "site-a" of legal entity "acme" with master data "dock-a-content"
    And the golden record process refines record "dock-b-record" to additional address "shared-dock" of site "site-b" of legal entity "acme" with master data "dock-b-content"
    When the sharing member updates record "dock-b-record"
    And the golden record process refines record "dock-b-record" to additional address "shared-dock" of site "site-c" of legal entity "acme" with master data "dock-b-moved-content"
    Then the Pool address of "dock-a-record" belongs to the sites of records "dock-a-record, dock-b-record"
    And "dock-a-record" output lists the site of record "dock-b-record" as an additional site of its address

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

  #h3. Test Objective:
  #
  #* Verify that a sharing member can state a further site of its address by BPNS, joining an existing site.
  #
  #h3. Preconditions:
  #
  ## A site of a legal entity has been shared and refined, so it exists as a golden record.
  #
  #h3. Description:
  #
  ## The sharing member shares a second record, stating the first record's site as an additional site of its address.
  ## The golden record process refines the second record to an additional address of its own site of the same legal entity.
  ## The second record's output lists the stated site as an additional site of its address.
  @TEST_CXTPM-XXXX @BPDM
  Scenario: Sharing Member States An Existing Site By BPNS
    Given the sharing member shares record "werk-a-record"
    And the golden record process refines record "werk-a-record" to site "werk-a" of legal entity "acme" with master data "werk-a-content"
    When the sharing member shares record "werk-b-record" stating the site of record "werk-a-record" as an additional site of its address
    And the golden record process refines record "werk-b-record" to additional address "werk-b-dock" of site "werk-b" of legal entity "acme" with master data "werk-b-content"
    Then "werk-b-record" output lists the site of record "werk-a-record" as an additional site of its address

  #h3. Test Objective:
  #
  #* Verify that a sharing member can state a further site of its address by name alone, and the golden record process creates it.
  #
  #h3. Description:
  #
  ## The sharing member shares a record stating a site that does not exist yet, by name.
  ## The golden record process refines the record to an additional address of its own site.
  ## The record's output lists the stated site, now created, as an additional site of its address.
  @TEST_CXTPM-XXXX @BPDM
  Scenario: Sharing Member States A New Site By Name
    When the sharing member shares record "dock-record" stating a site named "werk-new" as an additional site of its address
    And the golden record process refines record "dock-record" to additional address "shared-dock" of site "dock-site" of legal entity "acme" with master data "dock-content"
    Then "dock-record" output lists a site named "werk-new" as an additional site of its address

  #h3. Test Objective:
  #
  #* Verify that when two sites share the same site main address, that address belongs to both sites: each site record's output lists the other site as an additional site, and the Pool address returns both sites.
  #
  #h3. Description:
  #
  ## The sharing member shares two records.
  ## The golden record process refines both to distinct sites of the same legal entity that share one main address.
  ## Each record's output lists the other record's site as an additional site of the shared main address, and the Pool address belongs to both sites.
  @TEST_CXTPM-XXXX @BPDM
  Scenario: Sites Sharing A Main Address Belong To Each Other
    When the sharing member shares record "hq-site-a-record"
    And the golden record process refines record "hq-site-a-record" to site "hq-site-a" with shared main address "shared-hq" of legal entity "acme" with master data "hq-a-content"
    And the sharing member shares record "hq-site-b-record"
    And the golden record process refines record "hq-site-b-record" to site "hq-site-b" with shared main address "shared-hq" of legal entity "acme" with master data "hq-b-content"
    Then "hq-site-a-record" output lists the site of record "hq-site-b-record" as an additional site of its address
    And "hq-site-b-record" output lists the site of record "hq-site-a-record" as an additional site of its address
    And the Pool address of "hq-site-a-record" belongs to the sites of records "hq-site-a-record, hq-site-b-record"
