# Mindanao Prosperity Financial Cooperative Security System

## Overview
A Spring Boot application demonstrating secured role-based dashboards (Admin, Records Officer) with encrypted file intake, strict data validation, auditing, incident tracking, and minimal consistent UI.

## Why It Is Secure (Implemented Mechanisms)
- Strong Password Storage: BCrypt hashing with per-hash salt (passwords never stored or logged in plaintext).
- Authentication Lockout: 5 failed attempts trigger a 2‑minute account lock reducing brute force viability.
- Role-Based Access Control: Segregated privileges (Admin vs Records Officer) enforced at URL level (/admin/**, /upload/**).
- CSRF Protection: All state‑changing forms (login, logout, upload, incident acknowledge) include tokens; CSRF disabled only for the embedded H2 console path.
- Encrypted Data at Rest: Uploaded files encrypted using AES/GCM/NoPadding with a 256‑bit key; provides confidentiality plus integrity/authenticity via GCM tag.
- Fresh Initialization Vector: Each file uses a new 12‑byte IV ensuring semantic security (identical plaintexts encrypt differently).
- Integrity & Dedup Check: SHA‑256 checksum prevents silent duplicate re‑uploads and supports integrity verification logic.
- Strict File Validation: Exact ordered header match and per-row field validation block malformed, incomplete, or manipulated files (CSV / XLS / XLSX).
- Principle of Least Privilege: Admin cannot upload; Officer cannot access admin audit/incident management.
- Audit Logging: Successful logins, failures, lockouts, uploads, decrypt actions recorded for traceability.
- Incident Tracking: Failed logins, lockouts, unauthorized access attempts, upload validation failures recorded separately for monitoring.
- Secure Logout: POST-based logout with CSRF token, invalidates session and clears JSESSIONID.
- Consistent Defensive UI: Limits exposed surface (only necessary forms and links); no inline dangerous script features.
- H2 Console Isolation: Console allowed but framed sameOrigin and excluded from CSRF for operability only.

## Features (Implemented)
- Form login with lockout and redirect to role-specific dashboard.
- Admin dashboard: Encrypted files, audit logs, incidents (all paginated).
- Records Officer dashboard: Secure upload + encrypted history (paginated).
- AES-GCM encryption with stored IV + ciphertext.
- Strict CSV/XLS/XLSX header & row validation.
- Pagination for large data sets (files, audits, incidents).
- Unified minimal styling via `static/css/base.css`.
- Decryption endpoint (Admin only).
- In-memory demo users (hashed) for simplicity.

## Tech Stack
- Java 21, Spring Boot 3.x
- Spring Security, Spring Data JPA (H2 in-memory)
- Thymeleaf
- Apache POI (Excel parsing)
- AES/GCM (JCE), BCrypt hashing

## Prerequisites
- Java 21
- Maven 3.9+

## Quick Start
```bash
mvn clean spring-boot:run
```
Open: http://localhost:8080/

## Demo Users
| Role            | Username | Password    |
|-----------------|----------|-------------|
| Administrator    | admin    | Admin@123   |
| Records Officer  | officer  | Officer@123 |

BCrypt hashes are logged at startup (for demo verification only).

## Navigation Flow
1. Visit `/` → redirected to `/login` (if not authenticated).
2. Authenticate:
   - Admin → `/admin`
   - Officer → `/upload`
   - If accessing a protected URL first (e.g. `/h2-console`), redirected there after login.
3. Use POST logout → `/login?logout`.

## File Validation Rules
Expected header (exact order):
```
MemberID, FullName, Address, AccountNumber, Balance, LastTransactionDate
```
Row checks:
- MemberID: required & unique within file
- FullName: required
- AccountNumber: 8–16 digits
- Balance: numeric, non-negative
- LastTransactionDate: `yyyy-MM-dd`
Incorrect header/order or invalid row → upload rejected + incident logged.

## Encryption Summary
- Algorithm: AES/GCM/NoPadding
- Key: 256-bit Base64 in `application.yaml` (`security.encryption.key`)
- Per-file IV (12 bytes) + authentication tag (128-bit) stored with ciphertext
- Decryption restricted to Admin context

## Pagination Parameters
- Records Officer uploads: `/upload?page={n}`
- Admin encrypted files: `/admin?fpage={n}`
- Admin audit logs: `/admin?apage={n}`
- Admin incidents: `/admin?ipage={n}`

## Configuration (Selected)
`application.yaml`:
- `spring.h2.console.enabled=true`
- `spring.jpa.hibernate.ddl-auto=update`
- `security.encryption.key=<Base64 256-bit key>`
- Thymeleaf cache disabled for rapid iteration.

## Directory Highlights
```
src/main/java/com/mpfc/securebankingsystem/
  config/            (SecurityConfig)
  controller/        (Admin, Upload, Auth)
  security/          (CustomUserDetailsService, LockoutService)
  service/           (EncryptionService, FileService, AuditLogService, IncidentService)
  entity/            (FileEncrypted, AuditLog, Incident)
  repo/              (Repositories)
src/main/resources/
  templates/         (Thymeleaf pages + error pages)
  static/css/base.css
  application.yaml
docs/
  TECHNICAL.md
  NON-TECHNICAL.md
```

## H2 Console
- URL: `/h2-console`
- JDBC: `jdbc:h2:mem:mpfcdb`
- User: `sa` Password: (blank)

## Security Recap
- Password confidentiality (BCrypt)
- Transport assumed local/demo; app enforces server-side validation
- Data-at-rest confidentiality + integrity (AES-GCM)
- Least privilege isolation
- Tamper/abuse visibility (audits & incidents)
- Brute force mitigation (lockout)
- Session integrity (CSRF tokens, proper logout)
- Input sanitation via strict schema validation for uploads

---
End of README (operational + security overview only; deeper detail in docs).