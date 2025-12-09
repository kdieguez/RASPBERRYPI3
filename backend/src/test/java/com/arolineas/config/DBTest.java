package com.aerolineas.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DBTest {

  @BeforeEach
  void reset() {
    System.clearProperty("schema");
    DB.resetForTests();
  }

  @Test
  void getSchema_usaSystemPropertySiExiste() {
    System.setProperty("schema", "mi_schema");

    String schema = DB.getSchema();

    assertEquals("MI_SCHEMA", schema);
  }

  @Test
  void getSchema_fallbackPorDefectoAEROLINEA_siNoHayNada() {
    System.clearProperty("schema");
    DB.resetForTests();

    String schema = DB.getSchema();

    assertEquals("AEROLINEA", schema);
  }

  @Test
  void table_construyeNombreConSchemaYTabla() {
    System.setProperty("schema", "test_schema");
    DB.resetForTests();

    String fullTableName = DB.table("VUELO");

    assertEquals("TEST_SCHEMA.VUELO", fullTableName);
  }
}
