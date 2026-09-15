"""Configure local credentials without putting a password in the source code."""
from pathlib import Path
import argparse, base64, getpass, hashlib, os, secrets
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--hash-only", action="store_true", help="Print only the two Render admin variables, without writing local settings.")
args = parser.parse_args()
root = Path(__file__).resolve().parent
path = root / ".env"
settings = {}
if not args.hash_only and path.exists():
    for line in path.read_text().splitlines():
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            settings[key] = value
username = input("Admin ID [adminysss]: ").strip() or "adminysss"
if len(username) > 80:
    raise SystemExit("Admin ID must be at most 80 characters. No changes saved.")
password = getpass.getpass("Choose the admin password: ")
confirm = getpass.getpass("Repeat the password: ")
if not password or password != confirm:
    raise SystemExit("Passwords do not match or are empty. No changes saved.")
if len(password) > 72:
    raise SystemExit("Password must be at most 72 characters. No changes saved.")
salt = secrets.token_bytes(16)
key = hashlib.pbkdf2_hmac("sha256", password.encode(), salt, 600000)
settings.update(ADMIN_USERNAME=username, ADMIN_PASSWORD_HASH="pbkdf2:600000:" + base64.b64encode(salt).decode() + ":" + base64.b64encode(key).decode())
if args.hash_only:
    print("\nPaste these two values into Render's Environment tab:")
    print("ADMIN_USERNAME=" + settings["ADMIN_USERNAME"])
    print("ADMIN_PASSWORD_HASH=" + settings["ADMIN_PASSWORD_HASH"])
    raise SystemExit(0)
for name, value in {"DB_PASSWORD": secrets.token_urlsafe(24), "DB_ROOT_PASSWORD": secrets.token_urlsafe(24), "COOKIE_SECURE": "false", "SITE_ORIGIN": "http://localhost:8080", "FAMILY_ASSISTANCE_ENABLED": "true", "RESEND_API_KEY": "", "MAIL_FROM": ""}.items():
    settings.setdefault(name, value)
path.write_text("\n".join(k + "=" + v for k, v in settings.items()) + "\n")
try: path.chmod(0o600)
except OSError: pass
print("Admin configuration saved. The password is stored only as a salted hash.")
