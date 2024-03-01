{{/*
Copyright (c) 2021,2024 Contributors to the Eclipse Foundation

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.

SPDX-License-Identifier: Apache-2.0
*/}}

{{/*
Expand the name of the chart.
*/}}
{{- define "bpdm.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "bpdm.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- $name := .Values.fullnameOverride }}
{{- else if .Values.nameOverride }}
{{- $name := .Values.nameOverride }}
{{- else }}
{{- $name := .Chart.Name }}
{{- include "bpdm.toReleaseName" (list . $name)  -}}
{{- end }}
{{- end }}

{{- define "bpdm.toReleaseName" -}}
{{- $top := first . }}
{{- $name := index . 1 }}
{{- if contains $name $top.Release.Name }}
{{- $top.Release.Name | trunc 63 | trimSuffix "-" }}
{{- else if hasPrefix $top.Release.Name $name  }}
{{- $name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" $top.Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "bpdm.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "bpdm.labels" -}}
helm.sh/chart: {{ include "bpdm.chart" . }}
{{ include "bpdm.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "bpdm.selectorLabels" -}}
app.kubernetes.io/name: {{ include "bpdm.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create name of application secret
*/}}
{{- define "bpdm.applicationSecret.name" -}}
{{- printf "%s-application" (include "bpdm.fullname" .) }}
{{- end }}

{/*
Determine postgres service/host name to connect to
*/}}
{{- define "bpdm.postgresDependency" -}}
        {{- include "includeWithPostgresContext" (list $ "postgresql.primary.fullname") }}
{{- end }}}

{{/*
Invoke include on given definition with postgresql dependency context
Usage: include "includeWithPostgresContext" (list $ "your_include_function_here")
*/}}
{{- define "includeWithPostgresContext" -}}
{{- $ := index . 0 }}
{{- $function := index . 1 }}
{{- include $function (dict "Values" $.Values.postgres "Chart" (dict "Name" "postgres") "Release" $.Release "global" $.global) }}
{{- end }}

{{- /*
Merges three templates one after another in the following order:
valuesOverride -overrides-> (defaultOverride -overrides-> baseTemplate)

Usage: include "bpdm-common.threeWayMerge" ("context" $ "baseTemplate" "templateName" "defaultOverride" "templateName" "valuesOverride" "templateName")
*/}}
{{- define "bpdm-common.threeWayMerge" -}}
    {{- if .defaultOverride -}}
        {{- $ := .context -}}
        {{- $baseTemplateDict := fromYaml (include .baseTemplate $) | default (dict ) -}}
        {{- $defaultOverrideDict := fromYaml (include .defaultOverride $) | default (dict )  -}}
        {{- $valuesOverrideDict:= fromYaml (include .valuesOverride $) | default (dict )  -}}
        {{- $intermediateDict := merge $defaultOverrideDict $baseTemplateDict  -}}
        {{- $finalDict :=  merge $valuesOverrideDict $intermediateDict -}}
        {{- toYaml $finalDict -}}
    {{- else -}}
        {{- include "bpdm-common.merge" (dict "context" .context "baseTemplate" .baseTemplate "override" .valuesOverride) }}
    {{- end -}}
{{- end -}}

{{- define "bpdm-common.merge" -}}
    {{- $ := .context -}}
    {{- $baseTemplateDict := fromYaml (include .baseTemplate $) | default (dict ) -}}
    {{- $overrideDict := fromYaml (include .override $) | default (dict )  -}}
    {{- $finalDict :=  merge $overrideDict $baseTemplateDict -}}
    {{- toYaml $finalDict -}}
{{- end -}}

{{- define "bpdm-common.empty" -}}
{{- end -}}