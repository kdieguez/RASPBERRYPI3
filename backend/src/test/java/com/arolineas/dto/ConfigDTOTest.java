package com.aerolineas.dto;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigDTOTest {

  @Test
  void constructorVacio_ySetters_debenAsignarValores() {
    ConfigDTO dto = new ConfigDTO();

    Map<String, String> data = new HashMap<>();
    data.put("key1", "value1");
    data.put("key2", "value2");

    dto.setSection("home");
    dto.setData(data);

    assertEquals("home", dto.getSection());
    assertNotNull(dto.getData());
    assertEquals(2, dto.getData().size());
    assertEquals("value1", dto.getData().get("key1"));
  }

  @Test
  void constructorConArgs_debeGuardarSectionYData() {
    Map<String, String> data = new HashMap<>();
    data.put("title", "Bienvenido");
    data.put("color", "#FFFFFF");

    ConfigDTO dto = new ConfigDTO("landing", data);

    assertEquals("landing", dto.getSection());
    assertSame(data, dto.getData()); 
    assertEquals("Bienvenido", dto.getData().get("title"));
    assertEquals("#FFFFFF", dto.getData().get("color"));
  }
}
