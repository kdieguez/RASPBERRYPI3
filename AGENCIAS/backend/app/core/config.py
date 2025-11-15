import os
from urllib.parse import quote_plus
from dotenv import load_dotenv

load_dotenv()

def getenv_bool(name: str, default: bool = False) -> bool:
    v = os.getenv(name)
    return default if v is None else str(v).lower() in ("1", "true", "yes", "on")

def getenv_list(name: str, default=None):
    raw = os.getenv(name)
    if raw is None:
        return default if default is not None else []
    return [x.strip() for x in raw.split(",") if x.strip()]

MONGO_URI = os.getenv("MONGO_URI") or os.getenv("MONGODB_URI")

if not MONGO_URI:
    user = quote_plus(os.getenv("MONGO_USER", ""))
    pwd  = quote_plus(os.getenv("MONGO_PASS", ""))
    cluster = os.getenv("MONGO_CLUSTER", "")
    appname = quote_plus(os.getenv("MONGO_APPNAME", "AgenciaApp"))
    if user and pwd and cluster:
        MONGO_URI = (
            f"mongodb+srv://{user}:{pwd}@{cluster}/"
            f"?retryWrites=true&w=majority&appName={appname}"
        )

if not MONGO_URI:
    raise RuntimeError(
        "MONGO_URI no configurada. Define MONGO_URI o MONGODB_URI "
        "o (MONGO_USER, MONGO_PASS, MONGO_CLUSTER)."
    )

MONGO_DB = os.getenv("MONGO_DB") or os.getenv("MONGODB_DB") or "agencia_viajes"

ALLOWED_ORIGINS = getenv_list(
    "ALLOWED_ORIGINS",
    ["http://localhost:5174", "http://127.0.0.1:5174"]
)

CAPTCHA_SECRET   = os.getenv("CAPTCHA_SECRET", "")
CAPTCHA_DISABLED = getenv_bool("CAPTCHA_DISABLED", False)

APP_NAME = os.getenv("APP_NAME", "Agencia API")

AEROLINEAS_API_URL = (os.getenv("AEROLINEAS_API_URL", "http://localhost:8080") or "").rstrip("/")

ESTADO_CANCELADO_ID  = int(os.getenv("ESTADO_CANCELADO_ID", "2"))
ESTADO_CANCELADO_TXT = os.getenv("ESTADO_CANCELADO_TXT", "CANCELADO").strip().lower()

AEROLINEAS_TIMEOUT = float(os.getenv("AEROLINEAS_TIMEOUT", "20"))

__all__ = [
    "MONGO_URI",
    "MONGO_DB",
    "ALLOWED_ORIGINS",
    "CAPTCHA_SECRET",
    "CAPTCHA_DISABLED",
    "APP_NAME",
    "AEROLINEAS_API_URL",
    "ESTADO_CANCELADO_ID",
    "ESTADO_CANCELADO_TXT",
    "AEROLINEAS_TIMEOUT",
]

SMTP_HOST = os.getenv("SMTP_HOST", "")
SMTP_PORT = int(os.getenv("SMTP_PORT", "587"))
SMTP_USER = os.getenv("SMTP_USER", "")
SMTP_PASS = os.getenv("SMTP_PASS", "")
SMTP_FROM = os.getenv("SMTP_FROM", os.getenv("SMTP_USER", ""))
SMTP_USE_TLS = getenv_bool("SMTP_USE_TLS", True)

__all__ += [
    "SMTP_HOST", "SMTP_PORT", "SMTP_USER", "SMTP_PASS", "SMTP_FROM", "SMTP_USE_TLS",
]
