# BloodBridge — Full Stack Java

A blood-donor and recipient connection platform for the FSJP mini project. The complete application runs on Java 17 with Spring Boot, Spring Security, Hibernate/JPA and a SQL database. React provides the HTML/CSS/JavaScript interface, served by Java. MySQL is the main college database; H2 is included for local use and PostgreSQL for hosted deployment.

## Open and run in VS Code

1. Extract the ZIP and open the `BloodBridge-Java` folder with **File → Open Folder**.
2. Install JDK 17 or newer and Maven. `java -version` and `mvn -version` should work in the terminal.
3. On Windows, run `run-local.bat`; on macOS/Linux, run `bash run-local.sh`.
4. First run asks for an admin ID and password and saves a salted hash in your local `.env`. If PowerShell script execution is restricted, run `python setup-admin.py` first.
5. Open **http://localhost:8080**. Email entry works immediately, without OTP. Local records persist under `data/`.

The compiled interface is included, so Node.js is required only when editing frontend source. First startup downloads Java dependencies. You can also use **Terminal → Run Task → BloodBridge: run locally**.

## Use MySQL

With Docker Desktop installed, run the admin setup script, then:

```bash
docker compose up --build
```

This starts Java and MySQL 8.4 at **http://localhost:8080**. A Docker volume retains data. `docker compose down` stops the application while preserving it.

For your own MySQL server, create the `bloodbridge` database and configure `DB_URL`, `DB_USER`, `DB_PASSWORD`, then run `mvn spring-boot:run`. Migrations V1–V4 preserve existing records and add the request flow and internal inbox.

## Complete donation flow

1. Enter an email and create a profile with name, mobile, blood group, state, city and availability.
2. A recipient searches the directory by blood group and location, then sends a request with patient and hospital details.
3. The request appears in both people's **Requests** inboxes. Both must approve before contact details become available.
4. After meeting through the treating hospital, only the receiver can select **Mark blood received**.
5. The receiver's profile updates to **Blood received**, and the donor receives one completed donation and **500 recognition points**.
6. After **10 completed donations**, the donor gets the **Saviour** badge and **two lifetime family-support uses**.

Duplicate confirmation does not add points again. Pending or declined requests cannot be marked received. Administrators cannot bypass receiver confirmation to award points.

## Administrator portal

Open **Admin portal** in the footer or the **Admin portal** tab inside Sign in. Sign in with your configured ID/password. The portal shows private donor records with filters for **name, state, city, email, phone and blood group**.

Family requests include patient, donor and hospital details. Admins can mark requests in progress, completed or cancelled. A cancelled request restores one family use; completed and open requests count towards the limit.

## Internal inbox and message automation

The administrator portal opens to **Internal inbox**. Donor profile changes, blood requests, each person's approval, receiver confirmation and family-support updates automatically create notifications and a suggested reply. Filter by notification type or unread status, open a conversation, review or edit the generated reply and press **Send to member**.

Each member has a private **Inbox** in the site navigation. The admin's draft stays private until Send is pressed. Members can reply in an existing conversation or select **New message** to contact the team. Read/unread indicators and pagination are included. Inbox badges refresh every 30 seconds while the page is visible. Opening **Refresh conversation** loads newer updates without replacing a reply the administrator is editing in the background.

Messages are stored in SQL and are addressed by the account's internal ID, never by a self-entered email address. Only the owner and authenticated administrators can access a conversation. Automatic event messages are saved in the same transaction as the original request or update. Repeated Send requests do not duplicate a message, and stale drafts cannot overwrite a newer update. Inbox replies do not approve requests, award points or change a family-support status: use the corresponding Requests or Family requests controls for those actions.

This entire workflow works without an email domain or external mail provider. Suggested replies use built-in templates; there is no external text-generation service.

## Family notifications

The internal inbox is the primary notification channel. An optional email outbox can also notify **bloodbridgeadmin@gmail.com** after a Resend API key and verified sender are configured. External email is not required to deploy or use the site. See `AUTH-SETUP.md` if enabling it later. Both family uses are enforced inside a locked database transaction, including simultaneous submissions.

The family program has a 24-hour target to begin coordination. Blood availability and eligibility require confirmation by a licensed hospital or blood centre; the application cannot guarantee supply.

## Rewards

Live points recognise voluntary donations. The existing **1 point = ₹0.25** claim area remains a clearly labelled classroom simulation; it does not transfer money or alter live recognition points.

## Edit the application

- `src/main/java/com/bloodbridge/`: Java controllers, services, database entities, authentication and email automation.
- `src/main/resources/db/migration/`: MySQL and PostgreSQL migrations.
- `src/test/java/com/bloodbridge/`: integration tests, including simultaneous quota requests and mocked email delivery.
- `frontend/`: complete editable React interface.
- `src/main/resources/static/`: compiled interface served by Spring Boot.
- `setup-admin.py` and `setup-admin.ps1`: secure local administrator setup.

To rebuild the interface after editing:

```bash
cd frontend
npm ci
npm run build
```

To test and package the backend, run `mvn verify` from the main folder. Start the resulting JAR with `java -jar target/bloodbridge-1.0.0.jar` and appropriate database settings.

## Hosting

`render.yaml` and `Dockerfile` define a free Java web service. Supply a persistent PostgreSQL database through `DB_URL`, `DB_USER` and `DB_PASSWORD`; H2 should not be used on an ephemeral hosting filesystem. Configure `COOKIE_SECURE=true`, the public `SITE_ORIGIN` and admin hash. Email setup is optional: without a sender domain, family requests still save in the administrator portal and email notices remain pending. Add the mail provider settings later to activate delivery. Free hosting may sleep when idle.

Deploy the extracted project from your own GitHub/GitLab/Bitbucket repository using the included Render Blueprint. Secrets are filled in through the host and are not present in the ZIP. The live release must be verified before its deployment can be considered complete.

Email entry does not verify ownership: another device cannot claim an existing profile merely by entering the same email. Use the original browser to retain access. Admin sessions and rate limits are process-local, suitable for one instance; a shared store is needed before scaling to multiple Java instances.

## Project team

K. C. College of Engineering & Management Studies & Research, Thane. FSJP Mini Project, Second Year Computer Engineering, 2026–27.

Shaikh Yaseen · Shreyansh Singh · Sparsh Patel · Soham Kanase. Guide: Dr. Nita Patil.
