package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.dto.ClaseDTOs;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClaseDAO {

  public List<ClaseDTOs.View> listAll() throws Exception {
    String claseTable = DB.table("CLASE_ASIENTO");
    String sql = "SELECT ID_CLASE, NOMBRE FROM " + claseTable + " ORDER BY ID_CLASE";
    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      List<ClaseDTOs.View> out = new ArrayList<>();
      while (rs.next()) {
        out.add(new ClaseDTOs.View(
            rs.getInt("ID_CLASE"),
            rs.getString("NOMBRE")
        ));
      }
      return out;
    }
  }
}
