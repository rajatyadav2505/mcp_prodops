{{- define "prodops-control-tower-mcp.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "prodops-control-tower-mcp.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- include "prodops-control-tower-mcp.name" . -}}
{{- end -}}
{{- end -}}

{{- define "prodops-control-tower-mcp.serviceAccountName" -}}
{{- if .Values.serviceAccount.name -}}
{{- .Values.serviceAccount.name -}}
{{- else -}}
{{- include "prodops-control-tower-mcp.fullname" . -}}
{{- end -}}
{{- end -}}
