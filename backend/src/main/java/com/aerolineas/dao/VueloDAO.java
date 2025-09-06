package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.dto.VueloDTO;
import static com.aerolineas.util.EstadosVuelo.*; 

import java.sql.*;
import java.util.*;

public class VueloDAO {

    public void crearVuelo(VueloDTO.Create dto) throws SQLException {
        String sqlVuelo = "INSERT INTO VUELO (CODIGO, ID_RUTA, FECHA_SALIDA, FECHA_LLEGADA) VALUES (?,?,?,?)";
        try (Connection conn = DB.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sqlVuelo, new String[]{"ID_VUELO"})) {
                ps.setString(1, dto.codigo());
                ps.setLong(2, dto.idRuta());
                ps.setTimestamp(3, Timestamp.valueOf(dto.fechaSalida()));
                ps.setTimestamp(4, Timestamp.valueOf(dto.fechaLlegada()));
                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    long idVuelo = rs.getLong(1);

                    for (VueloDTO.ClaseConfig clase : dto.clases()) {
                        try (PreparedStatement ps2 = conn.prepareStatement(
                                "INSERT INTO SALIDA_CLASE (ID_VUELO, ID_CLASE, CUPO_TOTAL, PRECIO) VALUES (?,?,?,?)")) {
                            ps2.setLong(1, idVuelo);
                            ps2.setInt(2, clase.idClase());
                            ps2.setInt(3, clase.cupoTotal());
                            ps2.setDouble(4, clase.precio());
                            ps2.executeUpdate();
                        }   
                    }

                        if (dto.escalas() != null && !dto.escalas().isEmpty()) {
                        if (dto.escalas().size() > 1) {
                            throw new SQLException("Solo se permite 1 escala por vuelo");
                        }
                        var e = dto.escalas().get(0);

                        if (e.llegada().isAfter(e.salida())) {
                            throw new SQLException("La hora de SALIDA de la escala debe ser >= LLEGADA");
                        }

                        try (PreparedStatement psE = conn.prepareStatement(
                                "INSERT INTO VUELO_ESCALA (ID_VUELO, ID_CIUDAD, LLEGADA, SALIDA) VALUES (?,?,?,?)")) {
                            psE.setLong(1, idVuelo);
                            psE.setLong(2, e.idCiudad());
                            psE.setTimestamp(3, Timestamp.valueOf(e.llegada()));
                            psE.setTimestamp(4, Timestamp.valueOf(e.salida()));
                            psE.executeUpdate();
                        }
                    }
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public List<VueloDTO.View> listarVuelos() throws SQLException {
        String sql = """
            SELECT v.ID_VUELO, v.CODIGO, v.ID_RUTA, v.FECHA_SALIDA, v.FECHA_LLEGADA, v.ACTIVO,
               sc.ID_CLASE, sc.CUPO_TOTAL, sc.PRECIO
            FROM VUELO v
            JOIN SALIDA_CLASE sc ON v.ID_VUELO = sc.ID_VUELO
            ORDER BY v.ID_VUELO
        """;

        Map<Long, VueloDTO.View> vuelos = new LinkedHashMap<>();

        try (Connection conn = DB.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long idVuelo = rs.getLong("ID_VUELO");
                    VueloDTO.View view = vuelos.get(idVuelo);

                    VueloDTO.ClaseConfig clase = new VueloDTO.ClaseConfig(
                        rs.getInt("ID_CLASE"),
                        rs.getInt("CUPO_TOTAL"),
                        rs.getDouble("PRECIO")
                    );

                    if (view == null) {
                        view = new VueloDTO.View(
                            idVuelo,
                            rs.getString("CODIGO"),
                            rs.getLong("ID_RUTA"),
                            rs.getTimestamp("FECHA_SALIDA").toLocalDateTime(),
                            rs.getTimestamp("FECHA_LLEGADA").toLocalDateTime(),
                            rs.getInt("ACTIVO") == 1,
                            new ArrayList<>(List.of(clase)),  
                            new ArrayList<>()                 
                            );
                        vuelos.put(idVuelo, view);
                    } else {
                        view.clases().add(clase);
                    }
                }
            }  
            if (vuelos.isEmpty()) return List.of();
            StringBuilder placeholders = new StringBuilder();
            int size = vuelos.size();
            for (int i = 0; i < size; i++) {
                if (i > 0) placeholders.append(',');
                placeholders.append('?');
            }

            String sqlEsc = """
                SELECT ve.ID_VUELO, ve.ID_CIUDAD, c.NOMBRE AS CIUDAD, p.NOMBRE AS PAIS,
                    ve.LLEGADA, ve.SALIDA
                FROM VUELO_ESCALA ve
                JOIN CIUDAD c ON c.ID_CIUDAD = ve.ID_CIUDAD
                JOIN PAIS   p ON p.ID_PAIS   = c.ID_PAIS
                WHERE ve.ID_VUELO IN (%s)
            """.formatted(placeholders.toString());

            try (PreparedStatement psE = conn.prepareStatement(sqlEsc)) {
                int idx = 1;
                for (Long id : vuelos.keySet()) {
                    psE.setLong(idx++, id);
                }
                
                try (ResultSet rsE = psE.executeQuery()) {
                    while (rsE.next()) {
                        VueloDTO.View view = vuelos.get(rsE.getLong("ID_VUELO"));
                        view.escalas().add(new VueloDTO.EscalaView(
                            rsE.getLong("ID_CIUDAD"),
                            rsE.getString("CIUDAD"),
                            rsE.getString("PAIS"),
                            rsE.getTimestamp("LLEGADA").toLocalDateTime(),
                            rsE.getTimestamp("SALIDA").toLocalDateTime()
                        ));
                    }
                }
            }
        }
        return new ArrayList<>(vuelos.values());
    }

    public void actualizarEstado(long idVuelo, int idEstado) throws SQLException {
        if (!VALID.contains(idEstado)) {
            throw new SQLException("Estado inválido: " + idEstado);
        }

        String sqlSel = "SELECT ID_ESTADO FROM VUELO WHERE ID_VUELO = ?";
        String sqlUpd = "UPDATE VUELO SET ID_ESTADO = ? WHERE ID_VUELO = ?";

        try (Connection conn = DB.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int estadoActual;

                try (PreparedStatement ps = conn.prepareStatement(sqlSel)) {
                    ps.setLong(1, idVuelo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Vuelo no existe: " + idVuelo);
                        estadoActual = rs.getInt(1);
                    }
                }
                if (estadoActual == CANCELADO && idEstado != CANCELADO) {
                    throw new SQLException("El vuelo ya está cancelado; no se permite cambiar a otro estado.");
                }
                if (estadoActual == idEstado) {
                    conn.commit();
                    return;
                }
                try (PreparedStatement ps = conn.prepareStatement(sqlUpd)) {
                    ps.setInt(1, idEstado);
                    ps.setLong(2, idVuelo);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof SQLException) throw (SQLException) e;
                throw new SQLException("Error al actualizar estado", e);
            }
        }
    }
}
