# app/services/provision_service.py
import os, string, secrets, re
import oracledb
from fastapi import HTTPException

# ENV del DBA
ORA_DBA_DSN  = os.getenv("ORACLE_DBA_DSN")            # ej: "localhost:1521/XEPDB1"
ORA_DBA_USER = os.getenv("ORACLE_DBA_USER", "SYS")
ORA_DBA_PASS = os.getenv("ORACLE_DBA_PASS", "")
ORA_DBA_MODE = os.getenv("ORACLE_DBA_MODE", "SYSDBA")  # "SYSDBA" | "NORMAL"

# Esquema plantilla (AJÚSTALO al nombre real)
TEMPLATE_SCHEMA = os.getenv("ORACLE_TEMPLATE_SCHEMA", "AEROLINEA")

ORACLE_NAME_MAXLEN = 30

def strong_password(n=16):
    alphabet = string.ascii_letters + string.digits + "!@#$%^&*()_+-="
    return "".join(secrets.choice(alphabet) for _ in range(n))

def _sanitize_username(raw: str) -> str:
    """
    Convierte un string arbitrario en un identificador Oracle válido:
    - Solo [A-Z0-9_]
    - Si inicia con dígito, antepone 'U_'
    - Máx 30 chars
    - Uppercase
    """
    if not raw:
        raise HTTPException(400, "Identificador vacío")
    s = re.sub(r"[^A-Za-z0-9_]", "_", raw)          # reemplaza no válidos por _
    if s[0].isdigit():
        s = "U_" + s
    s = s.upper()
    if len(s) > ORACLE_NAME_MAXLEN:
        s = s[:ORACLE_NAME_MAXLEN]
    return s

def _connect_as_dba():
    if not (ORA_DBA_DSN and ORA_DBA_USER and ORA_DBA_PASS):
        raise HTTPException(500, "Config de DBA incompleta (ORACLE_DBA_*)")
    mode = oracledb.AUTH_MODE_SYSDBA if ORA_DBA_MODE.upper() == "SYSDBA" else oracledb.AUTH_MODE_DEFAULT
    return oracledb.connect(user=ORA_DBA_USER, password=ORA_DBA_PASS, dsn=ORA_DBA_DSN, mode=mode)

def _run_plsql(conn, block: str, binds: dict | None = None):
    with conn.cursor() as cur:
        cur.execute(block, binds or {})

def provision_oracle_schema(
    tenant_id: str,
    db_user: str | None = None,
    db_pass: str | None = None,
    db_dsn:  str | None = None,
) -> dict:
    """
    Crea/actualiza el usuario {db_user} y clona tablas desde TEMPLATE_SCHEMA.
    Si falla, lanza excepción (no se guarda nada en Central).
    """
    if not tenant_id or not tenant_id.strip():
        raise HTTPException(400, "tenant_id requerido")

    # ←— nombre Oracle seguro
    base = _sanitize_username(f"AERO_{tenant_id}")
    db_user = _sanitize_username(db_user) if db_user else base
    db_pass = db_pass or strong_password()
    db_dsn  = db_dsn  or ORA_DBA_DSN

    # Listas de tablas (ajustadas a tu requerimiento)
    only_structure = [
        'ACCION','CARRITO','CARRITO_ITEM','CIUDAD','ENTIDAD','PAIS','PASAJERO',
        'RESENA_VUELO','RESENA','RESERVA_ITEM','RUTA','SALIDA_CLASE','USUARIO',
        'VUELO','VUELO_COMENTARIO','VUELO_MOTIVO'
    ]
    with_data = [
        'CLASE_ASIENTO','ESTADO_RESERVA','DATOS_ESTRUCTURA',
        'ESTADOS','ESTRUCTURA','ROL'
    ]

    # PL/SQL
    create_user_block = """
    DECLARE
      v_count NUMBER;
    BEGIN
      SELECT COUNT(*) INTO v_count FROM dba_users WHERE username = :dst;
      IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE USER '||:dst||' IDENTIFIED BY "'||:pwd||'" DEFAULT TABLESPACE USERS TEMPORARY TABLESPACE TEMP QUOTA UNLIMITED ON USERS';
        EXECUTE IMMEDIATE 'GRANT CREATE SESSION, CREATE TABLE, CREATE VIEW, CREATE SEQUENCE, CREATE PROCEDURE TO '||:dst;
      ELSE
        EXECUTE IMMEDIATE 'ALTER USER '||:dst||' IDENTIFIED BY "'||:pwd||'"';
      END IF;
    END;
    """

    clone_header = """
    BEGIN
      DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'SEGMENT_ATTRIBUTES',FALSE);
      DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'STORAGE',FALSE);
      DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'TABLESPACE',FALSE);
      DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'OID',FALSE);
      DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'CONSTRAINTS',TRUE);
      DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'REF_CONSTRAINTS',TRUE);
      DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'SQLTERMINATOR',TRUE);
      DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'EMIT_SCHEMA',TRUE);
    END;
    """

    drop_block = """
    DECLARE
      v_sql VARCHAR2(500);
    BEGIN
      BEGIN
        v_sql := 'DROP TABLE '||:dst||'.'||:tab||' CASCADE CONSTRAINTS';
        EXECUTE IMMEDIATE v_sql;
      EXCEPTION WHEN OTHERS THEN
        IF SQLCODE != -942 THEN RAISE; END IF;
      END;
    END;
    """

    create_from_src_block = """
    DECLARE
      ddl CLOB;
      rep_src  VARCHAR2(400);
      rep_dst  VARCHAR2(400);
    BEGIN
      ddl := DBMS_METADATA.GET_DDL('TABLE', UPPER(:tab), :src);
      rep_src := ' "'||:src||'".';
      rep_dst := ' "'||:dst||'".';
      ddl := REPLACE(ddl, rep_src, rep_dst);
      EXECUTE IMMEDIATE ddl;
    END;
    """

    copy_data_block = """
    DECLARE
      stmt VARCHAR2(1000);
    BEGIN
      stmt := 'INSERT /*+ APPEND */ INTO '||:dst||'.'||:tab||' SELECT * FROM '||:src||'.'||:tab;
      EXECUTE IMMEDIATE stmt;
      COMMIT;
    END;
    """

    # Ejecuta
    conn = _connect_as_dba()
    try:
        _run_plsql(conn, create_user_block, {"dst": db_user, "pwd": db_pass})
        _run_plsql(conn, clone_header, {})

        for t in only_structure:
            _run_plsql(conn, drop_block, {"dst": db_user, "tab": t})
            _run_plsql(conn, create_from_src_block, {"tab": t, "src": TEMPLATE_SCHEMA, "dst": db_user})

        for t in with_data:
            _run_plsql(conn, drop_block, {"dst": db_user, "tab": t})
            _run_plsql(conn, create_from_src_block, {"tab": t, "src": TEMPLATE_SCHEMA, "dst": db_user})
            _run_plsql(conn, copy_data_block, {"tab": t, "src": TEMPLATE_SCHEMA, "dst": db_user})
    finally:
        conn.close()

    return {
        "db_user": db_user,
        "db_pass": db_pass,
        "db_dsn":  db_dsn,
        "jdbc_url": f"jdbc:oracle:thin:@{db_dsn}"
    }
