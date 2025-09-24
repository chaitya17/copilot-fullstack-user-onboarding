{{/*
Expand the name of the chart.
*/}}
{{- define "user-onboard.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "user-onboard.fullname" -}}
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
{{- define "user-onboard.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "user-onboard.labels" -}}
helm.sh/chart: {{ include "user-onboard.chart" . }}
{{ include "user-onboard.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "user-onboard.selectorLabels" -}}
app.kubernetes.io/name: {{ include "user-onboard.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "user-onboard.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "user-onboard.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Database JDBC URL helper - supports both MSSQL and Oracle
*/}}
{{- define "user-onboard.databaseUrl" -}}
{{- if eq .Values.database.type "mssql" -}}
{{- printf "jdbc:sqlserver://%s" .Values.database.external.jdbcUrl -}}
{{- else if eq .Values.database.type "oracle" -}}
{{- printf "jdbc:oracle:thin:@%s" .Values.database.external.jdbcUrl -}}
{{- else -}}
{{- .Values.database.external.jdbcUrl -}}
{{- end -}}
{{- end -}}

{{/*
Database specific configuration validation
*/}}
{{- define "user-onboard.validateDatabase" -}}
{{- if not (or (eq .Values.database.type "mssql") (eq .Values.database.type "oracle")) -}}
{{- fail "database.type must be either 'mssql' or 'oracle'" -}}
{{- end -}}
{{- if and (eq .Values.database.type "oracle") (not (contains "oracle" .Values.database.hibernate.dialect)) -}}
{{- fail "When using Oracle database, hibernate.dialect must be set to an Oracle dialect (e.g., org.hibernate.dialect.Oracle12cDialect)" -}}
{{- end -}}
{{- if and (eq .Values.database.type "mssql") (not (contains "SQLServer" .Values.database.hibernate.dialect)) -}}
{{- fail "When using MSSQL database, hibernate.dialect must be set to a SQL Server dialect (e.g., org.hibernate.dialect.SQLServerDialect)" -}}
{{- end -}}
{{- end -}}
