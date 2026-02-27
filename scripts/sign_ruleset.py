from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from cryptography.hazmat.primitives import serialization
import base64

PRIVATE_KEY_FILE = "rules/private_key.pem"

RULESET_FILE = "rules/ruleset-latest.json"

OUTPUT_SIG = "rules/ruleset-latest.sig"

with open(PRIVATE_KEY_FILE, "rb") as f:
    private_key = serialization.load_pem_private_key(
        f.read(),
        password=None
    )

with open(RULESET_FILE, "rb") as f:
    data = f.read()

signature = private_key.sign(data)

with open(OUTPUT_SIG, "w") as f:
    f.write(base64.b64encode(signature).decode())

print("Signature generated.")

