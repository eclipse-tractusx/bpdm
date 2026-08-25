# End-to-End Testing

Every BPDM release is validated by running the end-to-end test suite against a real deployment on the association's INT environment and reporting the result into Jira, where the Catena-X test management team tracks it.

This guide covers the maintainer's three duties around that: keeping the test definitions in Jira current, running the suite against the deployed environment, and uploading the test execution.

<!-- TOC -->
* [End-to-End Testing](#end-to-end-testing)
  * [The Test Suite](#the-test-suite)
  * [Jira and Xray](#jira-and-xray)
  * [How Tests Map to Jira](#how-tests-map-to-jira)
    * [The description block](#the-description-block)
  * [1. Upsert the Tests, Then Tag the New Ones](#1-upsert-the-tests-then-tag-the-new-ones)
    * [Pass 1 — upsert](#pass-1--upsert)
    * [Pass 2 — assign the new keys back to their scenarios](#pass-2--assign-the-new-keys-back-to-their-scenarios)
    * [Verify the loop closed](#verify-the-loop-closed)
  * [2. Run the Suite Against INT](#2-run-the-suite-against-int)
    * [Build the JAR](#build-the-jar)
    * [Run](#run)
    * [Scenarios that need further sharing members](#scenarios-that-need-further-sharing-members)
    * [Trying the suite on the snapshot deployment first](#trying-the-suite-on-the-snapshot-deployment-first)
    * [Testing another deployment](#testing-another-deployment)
    * [Test data is not cleaned up](#test-data-is-not-cleaned-up)
  * [3. Upload the Test Execution](#3-upload-the-test-execution)
    * [The release's Test Execution](#the-releases-test-execution)
    * [Upload](#upload)
    * [Link the execution in the release check issue](#link-the-execution-in-the-release-check-issue)
    * [What the execution records in Jira](#what-the-execution-records-in-jira)
    * [Automating the upload](#automating-the-upload)
  * [Relation to the CI Runs](#relation-to-the-ci-runs)
  * [Xray Reference](#xray-reference)
  * [NOTICE](#notice)
<!-- TOC -->

## The Test Suite

The end-to-end suite is the [system tester](../../bpdm-system-tester/README.md): a Cucumber suite packaged as an executable JAR that shares business partner data through a Gate, waits for the golden record process to complete, and asserts the output.
It knows nothing of the deployment beyond the three API base URLs and a set of client credentials.

The scenarios live as Gherkin feature files under `bpdm-system-tester/src/main/resources/cucumber/`.
**They are the source of truth.** The Jira Test issues are generated from them, not the other way round.

## Jira and Xray

| | |
|---|---|
| Jira project | [CXTPM](https://catena-x.atlassian.net/jira/software/c/projects/CXTPM/list) on `catena-x.atlassian.net` |
| Test management | Xray Cloud |
| API base | `https://xray.cloud.getxray.app/api/v2` |

Both API calls below need a bearer token, obtained from an Xray Cloud API key (client id and secret, issued in the Xray settings of the Jira instance).
The token is valid for 24 hours:

```bash
XRAY_TOKEN=$(curl -s -H "Content-Type: application/json" -X POST \
  --data "{\"client_id\":\"$XRAY_CLIENT_ID\",\"client_secret\":\"$XRAY_CLIENT_SECRET\"}" \
  https://xray.cloud.getxray.app/api/v2/authenticate | tr -d '"')
```

## How Tests Map to Jira

The mapping is carried entirely by tags in the feature files, which Xray reads on both the import and the upload direction.

```gherkin
@CXTPM-1043
Feature: Output Reflects Own Shared Master Data

  #h3. Test Objective:
  #
  #* Verify a newly shared record's output reflects the legal entity master data produced for it.

  @TEST_CXTPM-1012 @BPDM @Smoke
  Scenario: Legal Entity Master Data In Output
```

| Element                 | Meaning                                                                                                                  |
|-------------------------|--------------------------------------------------------------------------------------------------------------------------|
| `@CXTPM-1043` on Feature| The **Test Execution** issue the results are reported into. A bare issue key on the Feature is how Xray routes a Cucumber import into an existing Test Execution instead of creating a new one. Every feature file carries the same key, and it is [replaced once per release](#the-releases-test-execution). |
| `@TEST_CXTPM-<n>`       | The **Test** issue this scenario is. `TEST_` is the Xray Cucumber tag prefix.                                              |
| `@BPDM`                 | Product marker, on every scenario. Tags also become labels on the Test issue.                                              |
| `@Smoke`                | Part of the fast round-trip subset [CI runs on a pull request](#relation-to-the-ci-runs).                                   |
| `@TwoSharingMembers`, `@ThreeSharingMembers` | Needs that many sharing members; [skipped](#scenarios-that-need-further-sharing-members) when the run has fewer.  |
| `#h3. …` comments       | The Test issue's description in Jira wiki markup, kept next to the scenario it documents. The comment block belongs directly above the scenario's tag line — see [the description block](#the-description-block) for the shapes that survive the import. |

To find the key in use, read the feature-level tag of any feature file.
The [feature import](#1-upsert-the-tests-then-tag-the-new-ones) ignores it: requirement linking needs an explicit `@REQ_` prefix.

### The description block

The block is Jira wiki markup, one line per source line, with the Gherkin `#` stripped and **no space after it** — `#h3.` and not `# h3.`, since a space would land in the markup and stop `h3.` from being read as a heading.

Only part of the markup survives the round trip, and a line Xray cannot use is dropped silently:

| In the feature file | In Jira |
|---|---|
| `#h3. Test Objective:` | heading |
| `#* Verify …` | bullet |
| `#` | blank line |
| `#Plain sentence.` | paragraph text |
| `## Numbered item` | **nothing — the line is lost** |

`##` is the natural way to write a Jira numbered list through a Gherkin comment, and it is the one shape that does not arrive; every ordered list in this suite is therefore a `#*` bullet list.
The suite carried `##` list items for two releases before anyone noticed that every Test's *Preconditions* and *Description* section was empty in Jira, so treat an unexplained gap as a markup problem first.

To check what a Test actually holds, export it to a `.feature` file from the Xray UI with the Test issue's *Export to Cucumber* action.
The export is written in the same convention the import reads, so it is also the reference for what a description block should look like — and comparing it against the feature file shows line by line what arrived.

## 1. Upsert the Tests, Then Tag the New Ones

**Any release that added or changed an end-to-end scenario starts here, before the suite is run.**
Every scenario has to resolve to a real Jira Test issue. Push the feature files to Xray to achieve that — never write Tests by hand in the Jira UI.

The push is a **two-pass loop**: pass 1 creates the missing Tests, and their keys must be written into the feature files before pass 2.
Skip the write-back and the next upsert creates *another* Test for the same scenario, leaving the upload nothing to match against — the run goes unreported.

### Pass 1 — upsert

Clear any placeholder tags first.
A tag like `@TEST_CXTPM-XXXX` is worse than no tag: Xray tries to resolve `CXTPM-XXXX`, fails to find it, and errors instead of creating the Test.

```bash
cd bpdm-system-tester/src/main/resources
grep -rl "@TEST_CXTPM-XXXX" cucumber            # find them
# remove those tags, then:
zip -r /tmp/features.zip cucumber

curl -H "Authorization: Bearer $XRAY_TOKEN" \
  -F "file=@/tmp/features.zip" \
  -X POST "https://xray.cloud.getxray.app/api/v2/import/feature?projectKey=CXTPM"
```

Per scenario, Xray then either:

- **Tagged with `@TEST_CXTPM-<n>`** — updates that Test issue with the current specification. This is how a changed scenario propagates: edit the feature file, upsert, done.
- **Untagged** — creates a new Test issue, taking the scenario name as its summary.

### Pass 2 — assign the new keys back to their scenarios

The response carries no mapping: it lists the Tests as bare issue keys, with created and updated mixed together and no scenario names. Read it only to confirm `errors` is empty and that it created as many Tests as there were untagged scenarios.

Recover it yourself, for the scenarios that went in untagged:

1. List the CXTPM Tests newest first — in the Jira UI, or without leaving the terminal:

   ```bash
   curl -s -H "Authorization: Bearer $XRAY_TOKEN" -H "Content-Type: application/json" \
     -X POST https://xray.cloud.getxray.app/api/v2/graphql \
     -d '{"query":"{getTests(jql:\"project=CXTPM ORDER BY created DESC\",limit:20){results{jira(fields:[\"key\",\"summary\"])}}}"}' \
     | jq '.data.getTests.results[].jira'
   ```

   The Tests the upsert created are at the top, each summary being the scenario name it was taken from.
   This is Xray's GraphQL API, so the bearer token from above works — no Jira credential needed. `limit` accepts at most 100.

2. Tag each scenario with its key, on the same line as the `@BPDM` marker:

   ```gherkin
     @TEST_CXTPM-1234 @BPDM
     Scenario: Address Succession Reflected In Output
   ```

3. Commit the feature files; the tags are the mapping.

Two traps, both from Xray's fallback matching for untagged scenarios:

- An untagged scenario whose name matches an existing Test in the project binds to **that** Test instead of creating a new one. Convenient when intended, silently wrong when two scenarios share a name.
- Renaming a scenario that has no tag makes it a new scenario as far as the upsert is concerned, and it gets a second Test issue. Tag first, rename later.

### Verify the loop closed

Run the upsert a second time.
It should create nothing — every scenario now resolves to a Test and only updates it.
Anything still being created means a scenario is untagged, and that scenario would not be reported.

Removing a scenario is the one direction the upsert does not cover: close or deprecate the orphaned Test issue in Jira by hand.

## 2. Run the Suite Against INT

### Build the JAR

```bash
mvn -B -U clean package -pl bpdm-system-tester -am -DskipTests
```

### Run

The INT topology is checked in as the `int` Spring profile (`bpdm-system-tester/src/main/resources/application-int.yml`): the three base URLs and the Central-IDP issuer, with the credentials left to the environment.

```bash
SPRING_PROFILES_ACTIVE=int \
BPDM_INT_CLIENT_ID=<client id> \
BPDM_INT_CLIENT_SECRET=<client secret> \
  java -jar bpdm-system-tester/target/bpdm-system-tester.jar \
  --plugin json:target/cucumber-report.json
```

The credentials are a Central-IDP technical user of the `Cx-Operator` company holding the Pool, Gate and Orchestrator permissions — one user serves all four clients. See [operator API access](environments.md#operator-api-access) for how to create one. A sharing member's own Gate needs two Gate users instead; see [testing through a sharing member's Gate](#testing-through-a-sharing-members-gate).

Two things about this invocation are easy to get wrong:

- **Activate the profile through the environment.** `--spring.profiles.active=int` does *not* work: `main` forwards its arguments to the Cucumber CLI, which rejects the unknown option, prints its usage text and runs nothing — a run that looks like it did something and reports no scenarios.
- **Pass the JSON report plugin explicitly.** It is off by default, because the CI helm-test pod cannot write to its read-only filesystem. It is the artifact Xray consumes, so a release run without it produces nothing to upload.

Missing credentials do not fail fast: Spring passes the unresolved placeholder through as a literal and the run dies on the first token request with `401 invalid_client`.

The suite runs on 32 threads unless `--threads <n>` says otherwise, which puts a full run in the twenty-minute range.

### Scenarios that need further sharing members

A handful of scenarios assert what only shows when several sharing members share the same golden record.
Tagged `@TwoSharingMembers`: each member reflecting the other's master data changes, and the sharing member count of the golden record rising.
Tagged `@ThreeSharingMembers`: the confidence level itself rising, since the sharing member count only counts towards that level from three members on.
Each needs a Gate with technical users of a company of its own — a run configured with fewer members reports those scenarios as **skipped**, with the reason on the aborting hook in the Cucumber JSON, and its exit code is unaffected.

A skipped scenario is reported as neither passed nor failed, so the Test issues behind these scenarios stay without a result for the release unless a second sharing member is supplied:

```bash
SPRING_PROFILES_ACTIVE=int \
BPDM_INT_CLIENT_ID=<operator id> BPDM_INT_CLIENT_SECRET=<operator secret> \
BPDM_INT_GATE_2_BASE_URL=https://business-partners.int.catena-x.net/companies/<other member> \
BPDM_INT_GATE_2_INPUT_CLIENT_ID=<input manager id> BPDM_INT_GATE_2_INPUT_CLIENT_SECRET=<input manager secret> \
BPDM_INT_GATE_2_OUTPUT_CLIENT_ID=<output consumer id> BPDM_INT_GATE_2_OUTPUT_CLIENT_SECRET=<output consumer secret> \
  java -jar bpdm-system-tester/target/bpdm-system-tester.jar \
  --plugin json:target/cucumber-report.json
```

A third member follows the same shape, under `BPDM_INT_GATE_3_BASE_URL` and the four `BPDM_INT_GATE_3_*_CLIENT_*` variables.

Each member's two users are the input manager and output consumer pair described under [testing through a sharing member's Gate](#testing-through-a-sharing-members-gate), issued for that member's company.
The company is the point: the tester compares the BPNL in the tokens of all members before the first scenario and refuses to run when two of them match, because members from one company would neither be told apart by their Gates nor counted separately.
Note the skips in the release check issue when no second member is available, so the gap is recorded rather than mistaken for a clean run.

### Trying the suite on the snapshot deployment first

The `snapshot` profile targets the [`bpdm-snapshot` deployment](environments.md#int--argocdintcatena-xnet), which runs the development state of `main`.
It sits on the INT environment behind the same Central-IDP realm, so it takes the same credentials as the `int` profile and the variable names are shared:

```bash
SPRING_PROFILES_ACTIVE=snapshot \
BPDM_INT_CLIENT_ID=<client id> \
BPDM_INT_CLIENT_SECRET=<client secret> \
  java -jar bpdm-system-tester/target/bpdm-system-tester.jar \
  --plugin json:target/cucumber-report.json
```

Use it to shake the suite out before a release run — but do not report its results: the deployment runs unreleased code, so a failure there is as likely to be a real finding as a broken test, and the release is judged on INT.

### Testing through a sharing member's Gate

A Gate whose users the Portal manages cannot be driven by one credential. The Portal issues its technical users one role each, and the suite needs both sides of the Gate API: sharing input data and driving the sharing process is an **input manager**, reading the resulting golden records is an **output consumer**. The tester therefore configures two Gate clients — `bpdm.client.gate-input` for input and the sharing process, `bpdm.client.gate-output` for the output — and sends each call with the credential permitted to make it.

Everything but the credentials defaults from the input client, so the `int` profile takes only the two users:

```bash
SPRING_PROFILES_ACTIVE=int \
BPDM_INT_CLIENT_ID=<operator client id> \
BPDM_INT_CLIENT_SECRET=<operator client secret> \
BPDM_INT_GATE_INPUT_CLIENT_ID=<input manager client id> \
BPDM_INT_GATE_INPUT_CLIENT_SECRET=<input manager client secret> \
BPDM_INT_GATE_OUTPUT_CLIENT_ID=<output consumer client id> \
BPDM_INT_GATE_OUTPUT_CLIENT_SECRET=<output consumer client secret> \
  java -jar bpdm-system-tester/target/bpdm-system-tester.jar \
  --plugin json:target/cucumber-report.json
```

Point the Gate at that member's context path as well — `BPDM_CLIENT_GATE_INPUT_BASE_URL=https://business-partners.int.catena-x.net/companies/<member>`, which the output client follows. Pool and Orchestrator keep the operator user: the tester writes Pool metadata and acts as the cleaning service against the Orchestrator, which no sharing member user may do.

Both Gate users have to belong to the same company. The Gate scopes what a read returns by the BPNL of the token that made it, so an output consumer from another company reads an empty output rather than a `403`. The tester fetches a token for each Gate credential before the first scenario and refuses to run when the two name different companies, naming both in the message — without that check the run would fail much later, in the wait for the golden record output, with nothing pointing at the credentials. It logs the company it verified, and where it cannot decide — one credential in both roles, or a token carrying no BPNL — it says so and continues.

Leaving the two pairs unset falls back to `BPDM_INT_CLIENT_ID`/`BPDM_INT_CLIENT_SECRET` for both clients, which is how the Gate of the golden record core deployment is tested.

Naming further members' Gates and users on top of these runs the scenarios that need more than one sharing member — see [scenarios that need further sharing members](#scenarios-that-need-further-sharing-members).

### Testing another deployment

For a deployment with no profile of its own — a feature branch deployment, STABLE — copy the `int` profile's shape into `application-developer.yml`. That filename is gitignored, so it takes credentials inline without risking them reaching the repository; build the JAR afterwards so the file is packaged, and activate it with `SPRING_PROFILES_ACTIVE=developer`.

Base URLs always follow the ingress paths of that deployment's [`values.yaml`](https://github.com/eclipse-tractusx/bpdm/tree/environments).

### Test data is not cleaned up

Each run leaves its business partner data in the INT deployment permanently — there is no cleanup step and no reset before a release run.
This is accepted: the generator produces records that do not collide, so runs interfere neither with each other nor with data already there.

## 3. Upload the Test Execution

### The release's Test Execution

**Every release gets its own Test Execution issue**, so the evidence for a version stays self-contained.

Before the run:

1. Create a Test Execution issue in [CXTPM](https://catena-x.atlassian.net/jira/software/c/projects/CXTPM/list), named after the version being validated.
2. Replace the feature-level tag in every feature file with the new key, since that tag is what routes the upload:

   ```bash
   cd bpdm-system-tester/src/main/resources/cucumber
   sed -i '' 's/^@CXTPM-[0-9]*$/@CXTPM-<new key>/' *.feature
   ```

3. Commit that change as part of the release cycle.

Each release's execution is recorded by [linking it in that release's check issue](#link-the-execution-in-the-release-check-issue), which is where the history of them lives.

Re-running the suite for the same release adds a further run to each Test rather than overwriting the previous one.

### Upload

```bash
curl -H "Content-Type: application/json" \
     -H "Authorization: Bearer $XRAY_TOKEN" \
     -X POST --data @bpdm-system-tester/target/cucumber-report.json \
  https://xray.cloud.getxray.app/api/v2/import/execution/cucumber
```

Xray matches each scenario to its Test issue by the `@TEST_…` tag and records the runs against the Test Execution named by the feature-level tag.

If a run additionally needs fields set on the execution — a test environment, a fix version, a summary — use the multipart variant, which takes the results alongside an `info.json` in Jira's issue-create format:

```bash
curl -H "Authorization: Bearer $XRAY_TOKEN" \
     -F info=@info.json -F results=@bpdm-system-tester/target/cucumber-report.json \
  -X POST https://xray.cloud.getxray.app/api/v2/import/execution/cucumber/multipart
```

After the import, check in Jira that every scenario in the run mapped to a Test issue.
Scenarios whose tag does not resolve show up unmatched, which is the symptom of a skipped step 1.

### Link the execution in the release check issue

Link the Test Execution in the [release check issue](release-process.md#the-two-tracking-issues) under its testing section, where it backs the end-to-end and regression test items.
It is the evidence the release is judged on, and the release management team signs it off with the issue as a whole — the maintainer does not.

### What the execution records in Jira

Xray does not store a bare pass or fail. The Cucumber JSON carries per-step data and Jira surfaces all of it:

| Data | Shown as |
|---|---|
| Step status | Passed / Failed / Pending / Skipped, per `Given` / `When` / `Then` |
| Error message and stack trace | Inline on the failing step |
| Step duration | Execution time per step |
| Evidence | Anything attached at runtime, as step-level attachments |

The suite exploits the last row: the step definitions attach **every API call** — `uri`, request body and response body as pretty-printed JSON, named `<METHOD> <path>` — via `scenario.attach`, so a failed run shows the exact HTTP exchanges that led to the assertion.

When reading a failure, note that evidence attached from an `@After` hook lands on the step *after* the one that threw.

### Automating the upload

The upload is manual. Moving it into CI has to work around the plain Cucumber endpoint accepting no `testExecKey` or `testEnvironment` parameter: route through the feature-level tag, through the multipart endpoint's `info.json`, or by converting the report to Xray JSON, which takes `testExecutionKey` and `testEnvironments` as fields.

For GitHub Actions, [`mikepenz/xray-action`](https://github.com/mikepenz/xray-action) wraps the import and takes `testExecKey` and `testEnvironments` as workflow inputs; [`xray-maven-plugin`](https://github.com/Xray-App/xray-maven-plugin) covers the conversion route.

## Relation to the CI Runs

The chart ships the system tester as a Helm test hook, and CI runs it against a freshly deployed chart on a throwaway cluster in two scopes:

| Run | Scope | Sharing members |
|---|---|---|
| Pull request, and push to `main` or `release/**` | the `@Smoke` subset | one |
| Nightly (*Perform Nightly Chart Tests*, 02:20 UTC) | the entire suite | two — the only CI run that deploys a second Gate, and therefore the only one that executes the `@TwoSharingMembers` scenarios. CI deploys no third Gate, so `@ThreeSharingMembers` skips there and is covered by the release run alone |

Either way this is a deployment check, not the release validation: an ephemeral deployment and no JSON report to upload.
The release validation is the manual full run described above, against the INT deployment of the release candidate — and it covers the multi-member scenarios only when [those members are supplied](#scenarios-that-need-further-sharing-members).

## Xray Reference

- [Importing Cucumber Tests — REST v2](https://docs.getxray.app/display/XRAYCLOUD/Importing+Cucumber+Tests+-+REST+v2)
- [Import Execution Results — REST v2](https://docs.getxray.app/display/XRAYCLOUD/Import+Execution+Results+-+REST+v2)
- [Authentication — REST v2](https://docs.getxray.app/display/XRAYCLOUD/Authentication+-+REST+v2)
- [GraphQL API](https://docs.getxray.app/display/XRAYCLOUD/GraphQL+API) and its [schema reference](https://us.xray.cloud.getxray.app/doc/graphql/)
- [Testing using Cucumber in Java](https://docs.getxray.app/display/XRAY/Testing+using+Cucumber+in+Java)

The `TEST_` tag prefix is configurable per Xray instance (**Xray → Settings → Cucumber**).
To confirm the prefix in use, export an existing Test issue to a `.feature` file from the Xray UI and read the tag it writes.

Read only the Xray **Cloud** documentation — it uses the `/api/v2/…` paths of this guide, while Server/DC uses `/rest/raven/1.0/…` and its examples fail against this instance.

---

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2026 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2026 SAP SE
- SPDX-FileCopyrightText: 2023,2026 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2026 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2026 Robert Bosch GmbH
- SPDX-FileCopyrightText: 2023,2026 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2026 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm
