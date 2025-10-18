import asyncio
import smtplib
import ssl
from email.message import EmailMessage
from typing import Iterable, Optional, Tuple

from app.core.config import (
    SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS, SMTP_FROM, SMTP_USE_TLS,
)

_Attachment = Tuple[str, bytes, str]

def _build_message(to: str, subject: str, html: str,
                   attachments: Optional[Iterable[_Attachment]] = None) -> EmailMessage:
    msg = EmailMessage()
    msg["From"] = SMTP_FROM or SMTP_USER
    msg["To"] = to
    msg["Subject"] = subject

    plain = "Gracias por su compra. Si no ve el contenido, habilite HTML."
    msg.set_content(plain)
    msg.add_alternative(html, subtype="html")

    for att in attachments or []:
        filename, content, mime = att
        maintype, subtype = (mime.split("/", 1) if "/" in mime else ("application", "octet-stream"))
        msg.add_attachment(content, maintype=maintype, subtype=subtype, filename=filename)

    return msg

def _send_sync(msg: EmailMessage) -> None:
    if not SMTP_HOST:
        raise RuntimeError("SMTP no configurado (faltan variables en .env).")

    if SMTP_USE_TLS:
        with smtplib.SMTP(SMTP_HOST, SMTP_PORT) as server:
            server.ehlo()
            server.starttls(context=ssl.create_default_context())
            server.login(SMTP_USER, SMTP_PASS)
            server.send_message(msg)
    else:
        context = ssl.create_default_context()
        with smtplib.SMTP_SSL(SMTP_HOST, SMTP_PORT, context=context) as server:
            server.login(SMTP_USER, SMTP_PASS)
            server.send_message(msg)

async def send_email(to: str, subject: str, html: str,
                     attachments: Optional[Iterable[_Attachment]] = None) -> None:

    msg = _build_message(to, subject, html, attachments)
    await asyncio.to_thread(_send_sync, msg)