# Xray for Jira — Cucumber Integration Research

Research findings on integrating the BPDM system tester (Cucumber/Gherkin) with Xray for Jira
for test result reporting across multiple environments.

---

## 1. Binding a Cucumber Scenario to a Jira Test Issue

Xray represents each Cucumber `Scenario` or `Scenario Outline` as a **Test issue** (type: *Cucumber*) in Jira.

### Option A — Let Xray create/match automatically

Upload the `.feature` file via the Xray REST API:

```
POST /rest/raven/1.0/import/feature?projectKey=BPDM
```

Xray creates a new Test issue per scenario. On subsequent imports it matches by:
- A `@id:N` internal tag it adds to each scenario (unique within the file).
- The feature file's relative path (stored as a label, e.g., `cucumber/share_generic_business_partner.feature`).

If both match an existing Test issue it is **updated**, not duplicated.

### Option B — Tag the scenario with the Jira Test issue key (recommended)

Add the Jira Test issue key as a tag on the scenario using the prefix configured in
**Xray → Settings → Cucumber** (default prefix: `TEST_`):

```gherkin
Feature: Share generic business partner

  @TEST_BPDM-123
  Scenario: Share a valid generic business partner
    Given a company uploads a business partner
    When the sharing process completes
    Then the partner appears in the Pool with a BPN
```

When Cucumber JSON results are imported, Xray reads `@TEST_BPDM-123` and maps the result to
issue `BPDM-123`. The prefix is configurable — **export an existing Test issue to a `.feature`
file from the Xray UI to see the exact prefix in use in your instance.**

---

## 2. What Data Is Captured in a Test Execution

Xray does **not** just store an overall pass/fail. The Cucumber JSON format carries per-step
data and Xray surfaces all of it.

### Per-step information shown in Jira

| Data | Details |
|---|---|
| **Step status** | Passed / Failed / Pending / Skipped per `Given` / `When` / `Then` |
| **Error message & stack trace** | Displayed inline on the failing step |
| **Step duration** | Execution time per step |
| **Embeddings** | Screenshots, text logs, or any binary attached at runtime |

### Embedding HTTP request/response logs at runtime

Use the Cucumber `Scenario` API to attach data to any step. Xray stores these as
**step-level evidence** inside the Test Run:

```kotlin
import io.cucumber.java.Scenario

// Inject Scenario into a step definition or a hook
@When("I call the sharing API")
fun callSharingApi(scenario: Scenario) {
    val response = client.post("/api/sharing", requestBody)

    val log = """
        POST /api/sharing
        Request:  ${requestBody.toJson()}
        Response: ${response.status} ${response.body}
    """.trimIndent()

    // Attach as step evidence — visible in Jira Test Run view
    scenario.attach(log.toByteArray(), "text/plain", "http-exchange.txt")
}
```

Supported MIME types for embeddings:

| Type | Use |
|---|---|
| `text/plain` | Plain log text, request/response dumps |
| `application/json` | JSON payloads |
| `image/png` / `image/jpeg` | Screenshots |
| `text/html` | HTML reports |

### Known quirk — screenshot placement

Screenshots taken in an `@After` hook are attached to the **step after the one that threw**,
not the failing step itself. This is a known Xray behaviour.

### Post-hoc evidence via REST (Server/DC)

Evidence can also be attached after the fact via:

```
POST /rest/raven/1.0/api/testrun/{testRunId}/attachment
Content-Type: multipart/form-data
```

For Xray Cloud this requires the GraphQL API (`addEvidenceToTestRun` mutation).

---

## 3. Multiple Test Executions per Environment

The goal: one persistent Test Execution issue in Jira for each of:
- **Snapshot** — triggered when merging to `main`
- **Release Candidate** — triggered when cutting an RC
- **Release** — triggered on final release

### Core limitation

The standard Cucumber JSON import endpoint does **not** support `testExecKey` or
`testEnvironment` as query parameters. This is a documented Xray restriction. A workaround
is required for all three options below.

---

### Option A — Dynamic feature file tag injection (Cloud + Server/DC)

Pre-create three Test Execution issues in Jira once (e.g., `BPDM-100`, `BPDM-101`, `BPDM-102`).
In CI, inject the correct `@TestExecution_<key>` tag at the top of each feature file before
running the tests:

```bash
# Determine the target Test Execution based on pipeline context
if [ "$PIPELINE" = "snapshot" ]; then
  EXEC_KEY="BPDM-100"
elif [ "$PIPELINE" = "release-candidate" ]; then
  EXEC_KEY="BPDM-101"
elif [ "$PIPELINE" = "release" ]; then
  EXEC_KEY="BPDM-102"
fi

# Inject the tag into every feature file (work on a copy in CI)
for f in src/main/resources/cucumber/*.feature; do
  sed -i "1s/^/@TestExecution_${EXEC_KEY}\n/" "$f"
done
```

The Cucumber JSON produced by the test run will carry the tag. On import, Xray routes results
to the matching Test Execution. The three issues accumulate a run history over time.

**Pros:** Simple, works on both Cloud and Server/DC.  
**Cons:** Requires mutating feature files in CI (restore them afterwards, or work on a copy).

---

### Option B — Multipart endpoint with per-environment `info` JSON

The multipart Cucumber import endpoint accepts a separate JSON to set fields on the Test
Execution that gets created:

```
POST /rest/raven/1.0/import/execution/cucumber/multipart
```

Maintain one `testExecInfo-*.json` per environment:

```json
// testExecInfo-snapshot.json
{
  "fields": {
    "project":     { "key": "BPDM" },
    "summary":     "Snapshot — main branch test execution",
    "issuetype":   { "name": "Test Execution" },
    "fixVersions": [{ "name": "snapshot" }]
  }
}
```

```bash
curl -X POST \
  "https://<your-jira>/rest/raven/1.0/import/execution/cucumber/multipart" \
  -H "Authorization: Bearer $TOKEN" \
  -F "result=@cucumber.json;type=application/json" \
  -F "info=@testExecInfo-snapshot.json;type=application/json"
```

**Pros:** No feature file mutation; clean metadata per run.  
**Cons:** Creates a **new** Test Execution issue on every run (accumulates over time); does not
update a single persistent issue.

---

### Option C — Convert to Xray JSON format (maximum control)

Instead of importing native Cucumber JSON, convert it to **Xray JSON** first. This format
supports `testExecutionKey` and `testEnvironments` directly:

```json
{
  "testExecutionKey": "BPDM-100",
  "testEnvironments": ["snapshot"],
  "tests": [
    {
      "testKey": "BPDM-123",
      "status": "PASSED",
      "steps": [
        { "status": "PASSED", "actualResult": "BPN assigned successfully" }
      ]
    }
  ]
}
```

```bash
curl -X POST \
  "https://<your-jira>/rest/raven/1.0/import/execution" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  --data @xray-results-snapshot.json
```

The Cucumber JSON → Xray JSON conversion can be done with a small script or the
[xray-maven-plugin](https://github.com/Xray-App/xray-maven-plugin).

**Pros:** Full control — targets a pre-existing Test Execution, supports `testEnvironments`,
step-level data.  
**Cons:** Requires a conversion step in the pipeline.

---

### Option D — GitHub Actions: `mikepenz/xray-action`

If the pipeline runs in GitHub Actions, [`mikepenz/xray-action`](https://github.com/mikepenz/xray-action)
wraps the import and accepts `testExecKey` and `testEnvironments` directly as workflow inputs:

```yaml
# .github/workflows/test-snapshot.yml
- name: Import Cucumber results to Xray
  uses: mikepenz/xray-action@v3
  with:
    username: ${{ secrets.XRAY_CLIENT_ID }}
    password: ${{ secrets.XRAY_CLIENT_SECRET }}
    testFormat: "cucumber"
    testPaths: "**/cucumber.json"
    testExecKey: "BPDM-100"
    testEnvironments: "snapshot"
```

Use three workflow files (or one parameterised workflow with inputs) each referencing a
different `testExecKey`.

**Pros:** Clean, declarative, no scripting; integrates naturally with GitHub Actions CI.  
**Cons:** GitHub Actions only; depends on a third-party action.

---

## 4. Recommended Approach for BPDM

Given three distinct pipeline contexts (main merge / RC / release):

1. **Pre-create three persistent Test Execution issues** in Jira (one per environment). These
   are reused across all runs — each import appends a new run to the same issue, building a
   history.

2. **In CI, select the right key** based on the trigger (branch push vs. release tag vs. RC tag).

3. **Use Option D** (`xray-action`) if the pipelines are in GitHub Actions — cleanest
   integration. Fall back to **Option A** (tag injection) if a different CI system is used or
   if the Cucumber endpoint limitation is blocking Option D.

4. **Embed HTTP request/response logs** via `scenario.attach(...)` in the step definitions so
   every test run in Jira shows exactly which API calls were made.

---

## 5. Key Sources

- [Testing using Cucumber in Java — Xray Docs](https://docs.getxray.app/display/XRAY/Testing+using+Cucumber+in+Java)
- [Importing Cucumber Tests — REST (Xray Cloud)](https://docs.getxray.app/display/XRAYCLOUD/Importing+Cucumber+Tests+-+REST)
- [Import Execution Results — REST (Xray Cloud)](https://docs.getxray.app/display/XRAYCLOUD/Import+Execution+Results+-+REST)
- [Import Execution Results — REST (Xray Server/DC)](https://docs.getxray.app/display/XRAY/Import+Execution+Results+-+REST)
- [Test Runs — REST (Server/DC)](https://docs.getxray.app/display/XRAY/Test+Runs+-+REST)
- [xray-maven-plugin — Xray-App/xray-maven-plugin](https://github.com/Xray-App/xray-maven-plugin)
- [mikepenz/xray-action — GitHub](https://github.com/mikepenz/xray-action)
- [XRay: import cucumber results to existing test execution — Atlassian Community](https://community.atlassian.com/forums/App-Central-questions/XRay-import-cucumber-results-to-an-existing-test-execution-issue/qaq-p/2817542)
- [How to manage your Testing Environments in Jira — Xray Blog](https://www.getxray.app/blog/how-to-manage-your-testing-environments-in-jira)
- [Why embedded screenshots from Cucumber JSON don't appear as evidence — Atlassian Community](https://community.atlassian.com/t5/Marketplace-Apps-Integrations/Why-do-embedded-screenshots-from-Cucumber-JSON-report-don-t/qaq-p/778054)
