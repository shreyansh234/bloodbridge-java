# Sign-in and administrator setup

Member entry needs only an email address and works without an OTP. An email string is not proof of ownership. A random, HttpOnly device cookie restores only the account created on that browser. Another browser entering the same email receives a separate account. Keep using the original browser; clearing its cookies loses this access. A manual ownership check is required before any account transfer. This is deliberately not a verified identity system.

Administrator access requires the configured ID and password. The public download contains no live password or password hash.

## Configure your local administrator

On Windows, run `setup-admin.ps1` in PowerShell. If local policy prevents scripts, use `python setup-admin.py`. On macOS/Linux, run `python3 setup-admin.py`.

Enter the admin ID and the password you want to use. Setup creates `.env` with a random salt and PBKDF2-HMAC-SHA256 hash using 600,000 iterations. The original password is not saved. The ID can be `adminysss`.

For hosted deployment, set `ADMIN_USERNAME` and `ADMIN_PASSWORD_HASH` in the hosting service's environment settings. Use the generated hash from your own local `.env`. Do not upload `.env` with the project.

### Configure the existing Render service

1. From this project folder on your laptop, run `python setup-admin.py --hash-only` (or `python3` on macOS/Linux).
2. Enter `adminysss` as the Admin ID, then enter and repeat your chosen password at the hidden prompts. This mode does not write a file or change database settings.
3. Open **Render Dashboard → bloodbridge-java → Environment → Edit**. Set `ADMIN_USERNAME` to the printed ID and `ADMIN_PASSWORD_HASH` to the entire printed `pbkdf2:600000:...:...` value. Paste the hash without quotes, line breaks, or the `ADMIN_PASSWORD_HASH=` prefix. Do not put your plaintext password in this variable.
4. Keep existing database and other environment variables. Choose **Save, rebuild, and deploy** (or the equivalent save-and-deploy option), and wait for the deployment to become **Live**.
5. Open `/api/session` on that Render service: `adminReady` should be `true`. In the site's existing Admin portal, enter the ID and original password, not the hash.

The property mapping is `ADMIN_USERNAME` → `bloodbridge.admin-username` and `ADMIN_PASSWORD_HASH` → `bloodbridge.admin-password-hash`. `adminReady` stays false if the ID is missing or the hash is malformed. The accepted hash format is `pbkdf2:<iterations>:<Base64 salt>:<Base64 key>`, using PBKDF2-HMAC-SHA256, 210,000–1,000,000 iterations, a 16-byte salt and a 32-byte key. The setup script generates 600,000 iterations. Do not use a bcrypt hash or plaintext here.

Changing a value in GitHub's `render.yaml` with `sync: false` does not set it on an existing service. Enter the actual hash in Render's Environment tab. A failed build leaves the preceding successful version running; fix the build and deploy before checking the new release. No diagnostic endpoint is needed, and neither passwords nor hashes should appear in source code or logs.

## Internal notifications — no domain required

Admin notifications and generated reply drafts work immediately through **Admin portal → Internal inbox**. Press **Send to member** to deliver a draft to the user's private site inbox. The user can reply there. SQL stores the conversation and read status; the receiver is always the account that owns the request. No SMTP, email API key, domain or external messaging account is needed for this workflow.

## Optional external family-request email

The Java service stores each family request and email notice in the same database transaction. After commit it attempts delivery immediately; failed sends stay in the database and are retried with backoff. Five automatic attempts are allowed. Once the provider setup is repaired, an administrator can retry an exhausted notice, starting a new attempt budget with the same request ID. Sent notices are never resent by this action, and exhausted notices do not block newer messages. The idempotency key is tied to the request ID, preventing duplicate sends within the provider's retry window. Administrators can see delivery state in Family requests. “Submitted” means accepted by the provider, not confirmed inbox delivery.

Set these server environment values:

- `RESEND_API_KEY`: a Resend key allowed to send transactional emails.
- `MAIL_FROM`: an email address on a verified sender domain, optionally `BloodBridge <address@your-domain>`.
- `FAMILY_ASSISTANCE_ENABLED=true`.

The recipient is fixed to `bloodbridgeadmin@gmail.com`. Each notice contains the request ID, Saviour name, self-entered email, patient name, relationship, hospital, state, city, mobile number and required blood group. The form requires consent for this sharing.

Without provider credentials, family requests are still saved for the admin, and notifications remain pending. No live email is claimed to have been sent. Do not use the provider's test sender for arbitrary recipients; it may only send to the account owner's verified email.

## Security boundaries

- Admin passwords remain in server configuration as salted hashes.
- Spring Security sessions are server-side; login rotates the session and CSRF token.
- Admin sessions expire after 15 minutes idle and at most one hour.
- Mutations require the session's CSRF token; browsers never receive provider secrets.
- Public search returns anonymous donor availability and location.
- Full profiles are visible to their owner and administrators. Both approvals unlock the request partner's name and phone only.
- Only the receiver can confirm receipt, and one donor/date can earn points only once.
- Email-only entry cannot establish that two accounts belong to different physical people. Live points are recognition, not money.
