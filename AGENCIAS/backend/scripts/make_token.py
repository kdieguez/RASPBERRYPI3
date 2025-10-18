import os, jwt, datetime as dt
from dotenv import load_dotenv, find_dotenv

env_path = find_dotenv(usecwd=True)
if env_path:
    load_dotenv(env_path)

SECRET = os.getenv("JWT_SECRET", "dev-secret")
ALGO   = os.getenv("JWT_ALG", "HS256")
ISS    = os.getenv("JWT_ISSUER")    
AUD    = os.getenv("JWT_AUDIENCE")    

USER_ID = "68f346f029b86ecdb8cebea4"
EMAIL   = "tester@agencia.com"
ROL     = "cliente" 

now = dt.datetime.utcnow()
exp = now + dt.timedelta(hours=12)

payload = {
    "sub": USER_ID,
    "email": EMAIL,
    "rol": ROL,
    "iat": int(now.timestamp()),
    "exp": int(exp.timestamp()),
}
if ISS: payload["iss"] = ISS
if AUD: payload["aud"] = AUD

token = jwt.encode(payload, SECRET, algorithm=ALGO)
print(token)