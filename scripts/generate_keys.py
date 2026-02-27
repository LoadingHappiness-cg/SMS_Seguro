from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from cryptography.hazmat.primitives import serialization
import base64
import os

BASE_DIR = os.path.dirname(os.path.dirname(__file__))

RULES_DIR = os.path.join(BASE_DIR, "rules")

PRIVATE_KEY_FILE = os.path.join(RULES_DIR, "private_key.pem")
PUBLIC_KEY_FILE = os.path.join(RULES_DIR, "public_key.txt")

os.makedirs(RULES_DIR, exist_ok=True)

private_key = Ed25519PrivateKey.generate()
public_key = private_key.public_key()

private_bytes = private_key.private_bytes(
    encoding=serialization.Encoding.PEM,
    format=serialization.PrivateFormat.PKCS8,
    encryption_algorithm=serialization.NoEncryption(),
)

# IMPORTANT:
# Android SignatureVerifier uses X509EncodedKeySpec, which expects a DER-encoded
# SubjectPublicKeyInfo (SPKI) structure, not raw 32-byte Ed25519 public key bytes.
public_der_spki = public_key.public_bytes(
    encoding=serialization.Encoding.DER,
    format=serialization.PublicFormat.SubjectPublicKeyInfo,
)

with open(PRIVATE_KEY_FILE, "wb") as f:
    f.write(private_bytes)

with open(PUBLIC_KEY_FILE, "w", encoding="utf-8") as f:
    f.write(base64.b64encode(public_der_spki).decode())

print("")
print("KEYS GENERATED")
print("")
print("Private key:")
print(PRIVATE_KEY_FILE)
print("")
print("Public key (DER/SPKI base64 for app):")
print(PUBLIC_KEY_FILE)
print("")
print("IMPORTANT:")
print("Copy private_key.pem to iCloud for backup.")
print("Example:")
print("~/Library/Mobile Documents/com~apple~CloudDocs/SMS_Seguro_Backup/")
print("")

