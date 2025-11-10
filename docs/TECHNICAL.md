# Technical Documentation

Overview
This application demonstrates a secured, role-based file handling workflow with encryption, strict input validation, auditing, and incident tracking, using Spring Boot and Thymeleaf.

Architecture
- Web: Spring MVC controllers and Thymeleaf templates under src/main/resources/templates
- Security: Spring Security configuration in SecurityConfig, CustomUserDetailsService, and LockoutService
- Data: Spring Data JPA with H2 in-memory DB (DDL auto update)
- Encryption: AES/GCM via EncryptionService
- Validation: CSV and Excel (XLS/XLSX) header and row validation via FileService
- UI: Unified stylesheet at /static/css/base.css for consistent minimal UI

Authentication and Authorization
- Users are in-memory (demo) with BCrypt hashes generated at startup:
  - admin → role ROLE_ADMIN
  - officer → role ROLE_RECORDS_OFFICER
- Login page: GET /login (form posts to /login, CSRF protected)
- Role landing:
  - ROLE_ADMIN → /admin
  - ROLE_RECORDS_OFFICER → /upload
- Authorization rules:
  - /admin/** requires ROLE_ADMIN
  - /upload/** requires ROLE_RECORDS_OFFICER
  - /h2-console/** is permitAll (frames allowed via sameOrigin)
  - All other endpoints require authentication
- Access denied:
  - Attempts to access unauthorized routes trigger an incident and redirect to /403
  - 403 template is templates/error/403.html

CSRF and Logout
- CSRF enabled for the application
- CSRF is ignored only for /h2-console/**
- All forms (login, logout, upload, incident acknowledge) include CSRF tokens
- Logout is POST /logout and redirects to /login?logout

Account Lockout
- Implemented in LockoutService:
  - After 5 failed login attempts, the account is locked for 2 minutes
  - On successful login, attempts reset
- Failures and lock events are recorded as incidents and audit logs
- Login page displays a “locked” notice when applicable

Saved Requests
- On successful login, if a saved protected URL exists (e.g., user tried /h2-console first), the user is redirected there after authentication
- Otherwise, users are redirected based on role

Encryption
- EncryptionService uses AES/GCM/NoPadding
  - 256-bit key loaded from application.yaml at security.encryption.key (Base64)
  - 12-byte IV, 128-bit authentication tag
- Stored per file:
  - fileName, contentType, sizeBytes, uploader, uploadedAt, encryptionAlgo
  - checksumSha256 (for dedup)
  - iv (binary), cipherData (binary)
- Decryption endpoint (admin only):
  - GET /admin/files/{id}/decrypt returns decrypted content

Strict File Validation
- Accepted file types: CSV, XLS, XLSX
- Header (first row) must exactly match (order and names):
  MemberID, FullName, Address, AccountNumber, Balance, LastTransactionDate
- Row validation:
  - MemberID: required, unique per file
  - FullName: required
  - AccountNumber: 8–16 digits
  - Balance: numeric, non-negative
  - LastTransactionDate: yyyy-MM-dd
- Failures reject the upload and record an incident; message shown in UI
- Duplicate uploads (same SHA-256 checksum) are rejected

Auditing and Incidents
- AuditLogService:
  - Logs events like LOGIN_SUCCESS, LOGIN_FAILED, ACCOUNT_LOCKED, FILE_ENCRYPTED, FILE_DECRYPTED
- IncidentService:
  - Records FAILED_LOGIN, ACCOUNT_LOCKED, UNAUTHORIZED_ACCESS, UPLOAD_FAILED, etc.
- Admin dashboard shows paginated Audit Logs and Incidents, with ability to acknowledge incidents

UI and Templates
- Unified stylesheet: /static/css/base.css
  - Consistent dark headers, buttons, tables, messages, and pagination
- Templates:
  - login.html: minimal secure login
  - admin.html: Admin dashboard (Encrypted files, Audit logs, Incidents) with pagination
  - upload.html: Records Officer page (Upload + History) with pagination
  - error/403.html: Access denied
  - error/404.html: Not found page
- Pagination parameters:
  - /upload?page=N
  - /admin?fpage=N&apage=N&ipage=N

H2 Database
- In-memory: jdbc:h2:mem:mpfcdb
- Console at /h2-console (permitAll, frames sameOrigin)
- JPA ddl-auto: update
- Users are not stored in H2 (in-memory demo users)

Configuration (application.yaml)
- spring.datasource.url: jdbc:h2:mem:mpfcdb
- spring.h2.console.enabled: true
- spring.jpa.hibernate.ddl-auto: update
- spring.thymeleaf.cache: false
- security.encryption.key: Base64 256-bit AES key

Startup Log: BCrypt Hashes
- At startup, complete BCrypt hashes for demo users are logged with spacing for readability, plus boolean match checks:
  Example (hashes vary per run due to random salt):
  ==================== DEMO USER PASSWORD HASHES ====================
  Admin (username=admin)   : $2a$10$TgM0Z...full-hash...2h9r3W
  Officer (username=officer): $2a$10$C6QxP...full-hash...nWQkTe
  BCrypt match check       : admin=true, officer=true
  ===========================================================

Endpoints (selected)
- GET /login → Login page
- POST /login → Authenticate
- POST /logout → Logout
- GET / → Redirect to role-specific home or /login
- GET /admin → Admin dashboard (ROLE_ADMIN)
- POST /admin/incidents/{id}/ack → Acknowledge incident (ROLE_ADMIN)
- GET /admin/files/{id}/decrypt → Download decrypted content (ROLE_ADMIN)
- GET /upload → Upload dashboard (ROLE_RECORDS_OFFICER)
- POST /upload → Upload & encrypt (ROLE_RECORDS_OFFICER)
- GET /h2-console → H2 Console (permitAll)
- GET /403 → Access denied (renders templates/error/403.html)
- GET /error/** → Framework error pages (including 404)

Build and Run
- Build: mvn clean package
- Run: mvn spring-boot:run
- Default users:
  - admin / Admin@123
  - officer / Officer@123