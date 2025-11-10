# System Overview (Non-Technical)

What this system does
- Provides two secure dashboards:
  - Admin dashboard for administrators
  - Records Officer dashboard for uploading member data files
- Protects information by encrypting uploaded files
- Checks files to ensure the correct format before accepting them
- Tracks important actions (audits) and unusual events (incidents)
- Uses a simple, consistent interface

Who can access what
- Admin:
  - Can view the Admin dashboard
  - Can see encrypted files, audit logs, and incidents
  - Can download a decrypted copy of an uploaded file
- Records Officer:
  - Can view the Upload dashboard
  - Can upload member files (CSV/Excel)
  - Can see a history of uploaded files
- Each role is blocked from the other’s dashboard. If they try, they see a “403 Access Denied” page.

Login and security
- Users sign in on a login page
- If a user enters the wrong password 5 times, their account is locked for 2 minutes
- All actions are recorded for accountability
- Passwords are safely stored in memory as unreadable hashes (not plain text). The complete hashed values are shown in the application log at startup for demo proof.
- A logout button signs the user out

File uploads (Records Officer)
- The system only accepts files with the correct column headers in the first row:
  MemberID, FullName, Address, AccountNumber, Balance, LastTransactionDate
- If headers are missing or out of order, the upload is rejected and the event is recorded
- The system also checks each row for basic mistakes (like missing values or wrong formats)
- Every accepted file is encrypted before saving

Admin dashboard
- Shows lists of:
  - Encrypted files
  - Audit logs (who did what and when)
  - Incidents (suspicious or blocked actions)
- The lists have page numbers to make them easy to browse
- Admin can acknowledge incidents and download decrypted files

H2 Database Console (for demo)
- A simple built-in database is used for the demo
- A web console is available at /h2-console
- Data is temporary and reset every time the app restarts

How to run
- Start the app (developer runs it with Java and Maven)
- Go to the login page, then:
  - Admin user: username admin, password Admin@123
  - Records Officer: username officer, password Officer@123

Look and feel
- Clean, minimal screens
- Consistent dark headers and buttons across pages