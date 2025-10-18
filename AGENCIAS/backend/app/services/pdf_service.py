from typing import Dict, Any

async def build_ticket_pdf(detalle: Dict[str, Any]) -> bytes:
    content = f"Compra #{detalle.get('codigo') or detalle.get('id')}\nTotal: {detalle.get('total')}\n"
    return content.encode("utf-8")