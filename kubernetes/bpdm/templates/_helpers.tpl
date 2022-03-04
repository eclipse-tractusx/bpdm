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
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
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
Create the name of the service account to use
*/}}
{{- define "bpdm.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "bpdm.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Create the name of the application secrets
*/}}
{{- define "bpdm.applicationSecretsName" -}}
{{- printf "%s-%s" (include "bpdm.fullname" .) "application" }}
{{- end }}

{{/*
Create the name of the pull image secrets
*/}}
{{- define "bpdm.imagePullSecretsName" -}}
{{- printf "%s-%s" (include "bpdm.fullname" .) "image-pull" }}
{{- end }}

{{/*
Create docker auth string
*/}}
{{- define "bpdm.dockerAuth" -}}
{{- printf "%s:%s" .Values.imagePullSecrets.user .Values.imagePullSecrets.password | b64enc }}
{{- end }}

{{/*
Create host name for this release
*/}}
{{- define "bpdm.host" -}}
{{- printf "%s.%s" (index .Values.ingnginx.controller.service.annotations  "service.beta.kubernetes.io/azure-dns-label-name") .Values.ingress.hostSuffix }}
{{- end }}