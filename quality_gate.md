# Quality Gate Assessment — BPDM 7.4.0 / Chart 7.0.0

Checklist source: https://github.com/eclipse-tractusx/bpdm/issues/1647

> **Overall note:** This assessment was made on the release candidate `7.4.0-rc2` / chart `7.0.0-rc2`.
> Several checks can only be *finalized* once the real release is cut (drop the `-rc2` suffixes,
> publish images/chart, set CHANGELOG dates). Those are marked **Pending final release** — they are
> prepared correctly but not yet "true" until tagging. A few require external dashboards
> (Eclipse IPLab, CI scan results) that cannot be read from the repo — marked **Verify externally**.

Legend: ✅ fulfilled · ⚠️ gap / needs attention · 🕓 pending final release · 🔍 verify externally · ➖ N/A

## TRG 1 — Documentation
- **1.01 README** — ✅ `README.md` present.
- **1.02 Install instructions** — ✅ `INSTALL.md` present.
- **1.03 CHANGELOG** — ⚠️ `CHANGELOG.md` present and populated, but the entry is `## [7.4.0] - tbd`. Set the release date before final (expected to be `tbd` while on RC).
- **1.04 Editable static files** — ✅ Architecture docs are Markdown/source under `docs/architecture/` with an `assets` folder; no binary-only diagrams observed.
- **1.05 Architecture docs** — ✅ Full arc42 set under `docs/architecture/` (01–11).
- **1.06 Administrator guide** — ✅ `docs/admin/README.md` + `MIGRATION_GUIDE.md`.
- **1.07 User manual** — ⚠️ No dedicated user manual (`docs/user` absent). Coverage is implicit via admin guide + OpenAPI docs. Confirm whether the QG reviewer accepts API docs as the user manual, otherwise this is a gap.
- **1.08 OpenAPI docs** — ✅ `docs/api/` has gate/orchestrator/pool in `.yaml` and `.json`.

## TRG 2 — Git
- **2.01 Default branch `main`** — ✅ Default branch is `main`.
- **2.03 Repository structure** — ✅ Standard mono-repo layout.
- **2.04 Leading product repository** — ✅ `.tractusx` declares `leadingRepository` = this repo.
- **2.05 `.tractusx` metafile** — ✅ Valid format; `openApiSpecs` already includes the `7.4.x` and `main` spec URLs.
- **2.06 Dependabot** — ✅ `.github/dependabot.yml` covers maven, github-actions, docker (weekly).

## TRG 3 — Kubernetes
- **3.02 PV/PVC or DB dependency** — ✅ Chart bundles Postgres dependency (`postgres 0.11.0`); subcharts depend on a database.

## TRG 4 — Container
- **4.01 Semantic versioning/tagging** — ✅ Images tagged via `appVersion`; SemVer in use (RC suffix is fine pre-release).
- **4.02 Base image agreed** — ✅ `eclipse-temurin:21-jre-alpine`. ⚠️ But `docker/README.md` still says `17-jre-alpine` — fix this stale doc.
- **4.03 USER / non-root** — ✅ Dockerfiles add a `bpdm` user (UID 10001) and set `USER`; charts set `runAsNonRoot: true`.
- **4.05 Released image in DockerHub, remove GHCR** — 🕓 Pending final release. Image refs point to `docker.io/tractusx/...`; no GHCR references found in charts/docker/deploy workflows. True once images are published.
- **4.06 Separate DockerHub notice** — ✅ Each module has its own `docker/<module>/DOCKER_NOTICE.md`.
- **4.07 Read-only root filesystem** — ✅ `readOnlyRootFilesystem: true` in chart values (verified on pool; confirm across all subcharts).

## TRG 5 — Helm
- **5.01 Chart requirements** — ✅ `Chart.yaml` (apiVersion v2, name, version, maintainers).
- **5.02 Chart location `/charts`** — ✅ `charts/bpdm`.
- **5.03 Version strategy** — 🕓 Chart `7.0.0-rc2` uses `-rc` suffix — correct RC strategy; drop suffix → `7.0.0` for final.
- **5.04 CPU/MEM requests & limits** — ✅ `resources:` block present in subchart values.
- **5.06 Configurable via Helm** — ✅ Extensive values + projected config secrets.
- **5.07 Dependencies in Chart.yaml** — ✅ Subcharts + postgres/keycloak declared.
- **5.08 Single deployable chart** — ✅ Umbrella `bpdm` chart deploys all services.
- **5.09 Helm Test** — ✅ `templates/tests/test-connection.yaml` runs the system-tester as a `helm test` hook.
- **5.10 Support 3 versions** — ✅ `.tractusx` lists 6.2.x→7.4.x specs; confirm 3 chart release branches are maintained.
- **5.11 Upgradeability** — ⚠️ This release is flagged Breaking (Postgres/Keycloak upgrade affects embedded DB). Migration guide exists, but verify the upgrade path is documented/tested as the QG expects.

## TRG 6 — Released Helm Chart
- **6.01 Released Helm Chart** — 🕓 Pending final release. Not yet published (still RC). `helm-chart-release.yaml` workflow is in place.

## TRG 7 — Open Source Governance
- **7.01 Legal documentation** — ✅ `LICENSE`, `LICENSE_non-code`, `LICENSES/`, `NOTICE.md`, `AUTHORS.md`.
- **7.02 License/copyright header** — ✅ Headers present (verified Kotlin source + chart templates).
- **7.03 IP checks (project content)** — 🔍 Verify externally (Eclipse IPLab / `gitdash`).
- **7.04 IP checks (3rd party)** — ⚠️/🔍 `DEPENDENCIES` file present and current-looking; confirm all entries are resolved/approved in the Eclipse dashboard (recent netty/tomcat bumps may have introduced new deps to clear).
- **7.05 Legal info for distributions** — ✅ `DEPENDENCIES` + DockerHub notices.
- **7.06 Legal info for end-user content** — ✅ `NOTICE.md`.
- **7.07 Legal notice for docs** — ✅ Doc headers / `LICENSE_non-code`.
- **7.08 Legal notice for KIT docs** — ➖ N/A unless BPDM publishes a KIT; confirm.

## TRG 8 — Security
All four scanners are wired into CI — fulfillment depends on the latest run results, which cannot be read from the working tree. Check the Actions tab / security dashboard.
- **8.01 CodeQL** — 🔍 Verify externally. `codeql.yml` workflow exists; check latest run has no high+ findings.
- **8.02 KICS** — 🔍 `kics.yml` exists; verify no high+ open.
- **8.03 GitGuardian/TruffleHog secrets** — 🔍 `trufflehog.yml` exists; verify no secret findings.
- **8.04 Trivy** — 🔍 `trivy.yml` + `app-test-trivy.yaml` exist; verify no high+ unmitigated.

## TRG 9 — UX/UI Styleguide
- **9.01 UI consistency/styleguide** — ➖ N/A. BPDM ships backend services + APIs only; no UI in this repo.

---

## Action items before cutting final
1. Drop `-rc2` from all chart versions and `appVersion` (`7.0.0`, `7.4.0`).
2. Set CHANGELOG date: `## [7.4.0] - tbd` → release date.
3. Fix `docker/README.md` base image (`17-jre-alpine` → `21-jre-alpine`).
4. Confirm/clarify user manual (TRG 1.07).
5. Confirm Eclipse IP checks clear (7.03/7.04), especially after netty/tomcat bumps.
6. Confirm latest security scan runs are clean (TRG 8.x).
7. Publish image + chart (4.05, 6.01) and re-tick.
