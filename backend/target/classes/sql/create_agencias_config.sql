--------------------------------------------------------
-- Tabla para configuración de agencias asociadas
-- Permite a una aerolínea registrar múltiples agencias
-- que pueden conectarse a su sistema
--------------------------------------------------------

CREATE TABLE "AEROLINEA"."AGENCIAS_CONFIG" 
   (	
    "ID_AGENCIA" VARCHAR2(100 BYTE) NOT NULL ENABLE,  -- Identificador único (ej: "AGENCIA_VIAJES_1")
    "NOMBRE" VARCHAR2(200 BYTE) NOT NULL ENABLE,       -- Nombre descriptivo
    "API_URL" VARCHAR2(500 BYTE) NOT NULL ENABLE,      -- URL del backend de la agencia (ej: http://localhost:8000)
    "ID_USUARIO_WS" NUMBER(10,0),                       -- FK a USUARIO (usuario webservice que representa a esta agencia)
    "HABILITADO" NUMBER(1,0) DEFAULT 1,                 -- 1=habilitado, 0=deshabilitado
    "CREADO_EN" TIMESTAMP (6) DEFAULT CURRENT_TIMESTAMP,
    "ACTUALIZADO_EN" TIMESTAMP (6),
    CONSTRAINT "PK_AGENCIAS_CONFIG" PRIMARY KEY ("ID_AGENCIA"),
    CONSTRAINT "FK_AGENCIAS_USUARIO" FOREIGN KEY ("ID_USUARIO_WS")
         REFERENCES "AEROLINEA"."USUARIO" ("ID_USUARIO") ON DELETE SET NULL
   ) 
SEGMENT CREATION IMMEDIATE 
PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 
NOCOMPRESS LOGGING
STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
TABLESPACE "USERS" ;

-- Índice para búsqueda rápida por usuario webservice
CREATE INDEX "IDX_AGENCIAS_USUARIO_WS" ON "AEROLINEA"."AGENCIAS_CONFIG" ("ID_USUARIO_WS") 
TABLESPACE "USERS" ;

-- Índice para búsqueda de agencias habilitadas
CREATE INDEX "IDX_AGENCIAS_HABILITADO" ON "AEROLINEA"."AGENCIAS_CONFIG" ("HABILITADO") 
TABLESPACE "USERS" ;

-- Comentarios en columnas
COMMENT ON COLUMN "AEROLINEA"."AGENCIAS_CONFIG"."ID_AGENCIA" IS 'Identificador único de la agencia (ej: AGENCIA_1)';
COMMENT ON COLUMN "AEROLINEA"."AGENCIAS_CONFIG"."NOMBRE" IS 'Nombre descriptivo de la agencia';
COMMENT ON COLUMN "AEROLINEA"."AGENCIAS_CONFIG"."API_URL" IS 'URL completa del backend de la agencia (http://ip:puerto)';
COMMENT ON COLUMN "AEROLINEA"."AGENCIAS_CONFIG"."ID_USUARIO_WS" IS 'ID del usuario webservice asociado a esta agencia (NULL si no se ha asignado)';
COMMENT ON COLUMN "AEROLINEA"."AGENCIAS_CONFIG"."HABILITADO" IS '1=habilitada, 0=deshabilitada';


