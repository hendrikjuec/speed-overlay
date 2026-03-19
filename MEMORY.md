# =============================================================================
# MEMORY.md - Project Security Context Template
# =============================================================================
# Dieses Template in jedes Projekt-Root kopieren und anpassen.
# Copilot liest diese Datei automatisch für Projekt-Kontext.
# =============================================================================

## 🔒 Security Context

### Klassifizierung
- **Datenklassifizierung:** [ÖFFENTLICH | INTERN | VERTRAULICH | STRENG VERTRAULICH]
- **Personenbezogene Daten:** [JA/NEIN] → Falls JA: DSGVO-Konformität erforderlich
- **Regulatorische Anforderungen:** [z.B. ISO 27001, BSI IT-Grundschutz, PCI-DSS]

### Secrets Management
- **Secrets-Speicherort:** [Azure KeyVault / Environment Variables / Windows Credential Manager]
- **KeyVault URL:** `https://VAULT-NAME.vault.azure.net/`
- **Benötigte Secrets:**
  - `DB_PASSWORD` → KeyVault Secret: `proj-db-password`
  - `API_KEY` → KeyVault Secret: `proj-api-key`
- **Rotation:** [Zeitraum, z.B. 90 Tage]

### Dependency Policy
- **Erlaubte Lizenzen:** [MIT, Apache-2.0, BSD-2/3-Clause]
- **Gesperrte Lizenzen:** [GPL-3.0, AGPL-3.0 (bei proprietärem Code)]
- **CVSS-Schwellwert:** ≥ 7.0 blockiert Build
- **Update-Zyklus:** [Wöchentlich / Bei jedem Sprint / Monatlich]
- **Vulnerability-Check:** `mvn dependency-check:check` / `pip-audit`

### SAST Configuration
- **Aktive Scanner:** [SpotBugs+FindSecBugs, PMD, Bandit, SonarQube]
- **SonarQube URL:** `https://sonarqube.example.com`
- **Quality Gate:** [Keine neuen Bugs, 0 Security Hotspots unreviewed]
- **Ausnahmen:** (dokumentierte Suppressions mit Begründung)

### Authentifizierung & Autorisierung
- **Auth-Methode:** [OAuth2 / JWT / SAML / API-Key]
- **Identity Provider:** [Azure AD / Keycloak / Custom]
- **Rollen-Modell:** [RBAC / ABAC]
- **Session-Timeout:** [Minuten]

### Netzwerk & Deployment
- **Deployment-Ziel:** [Kubernetes / Azure App Service / On-Premise]
- **Namespace:** [z.B. prod, staging, dev]
- **Ingress/TLS:** [Ja, Let's Encrypt / Custom CA]
- **Network Policies:** [Ja/Nein]

### Sichere Coding-Regeln (Projekt-spezifisch)
1. Kein `System.out.println()` für Logging → SLF4J/Logback verwenden
2. Keine SQL-String-Konkatenation → PreparedStatements oder JPA
3. Input-Validierung an allen API-Endpunkten
4. Keine hartcodierten Credentials
5. Sensitive Daten in Logs maskieren

### Notfall-Kontakte
- **Security-Team:** security@msg.group
- **Incident-Response:** [Prozess/Link]
