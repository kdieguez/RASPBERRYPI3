package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.dto.TipDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TipDAOTest {

    @Test
    @DisplayName("listar devuelve lista mapeada correctamente")
    void listar_ok() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        String tipsTable = "TIPS";
        String sql = "SELECT ID_TIP, TITULO, DBMS_LOB.SUBSTR(DESCRIPCION, 4000, 1) AS DESCRIPCION, ORDEN FROM " +
                tipsTable + " ORDER BY ORDEN ASC, ID_TIP ASC";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("TIPS")).thenReturn(tipsTable);

            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true, true, false);
            when(rs.getLong("ID_TIP")).thenReturn(1L, 2L);
            when(rs.getString("TITULO")).thenReturn("t1", "t2");
            when(rs.getString("DESCRIPCION")).thenReturn("d1", "d2");
            when(rs.getInt("ORDEN")).thenReturn(10, 20);

            TipDAO dao = new TipDAO();
            List<TipDTO> lista = dao.listar();

            assertEquals(2, lista.size());
            assertEquals(1L, lista.get(0).idTip);
            assertEquals("t1", lista.get(0).titulo);
            assertEquals(10, lista.get(0).orden);
            assertEquals(2L, lista.get(1).idTip);
            assertEquals("t2", lista.get(1).titulo);
            assertEquals(20, lista.get(1).orden);
        }
    }

    @Test
    @DisplayName("obtenerPorId devuelve tip mapeado cuando existe")
    void obtenerPorId_encontrado() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        String tipsTable = "TIPS";
        String sql = "SELECT ID_TIP, TITULO, DBMS_LOB.SUBSTR(DESCRIPCION, 4000, 1) AS DESCRIPCION, ORDEN FROM " +
                tipsTable + " WHERE ID_TIP = ?";

        long idTip = 5L;

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("TIPS")).thenReturn(tipsTable);

            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getLong("ID_TIP")).thenReturn(idTip);
            when(rs.getString("TITULO")).thenReturn("titulo");
            when(rs.getString("DESCRIPCION")).thenReturn("desc");
            when(rs.getInt("ORDEN")).thenReturn(7);

            TipDAO dao = new TipDAO();
            TipDTO t = dao.obtenerPorId(idTip);

            assertNotNull(t);
            assertEquals(idTip, t.idTip);
            assertEquals("titulo", t.titulo);
            assertEquals("desc", t.descripcion);
            assertEquals(7, t.orden);

            verify(ps).setLong(1, idTip);
        }
    }

    @Test
    @DisplayName("obtenerPorId devuelve null cuando no existe")
    void obtenerPorId_noEncontrado() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        String tipsTable = "TIPS";
        String sql = "SELECT ID_TIP, TITULO, DBMS_LOB.SUBSTR(DESCRIPCION, 4000, 1) AS DESCRIPCION, ORDEN FROM " +
                tipsTable + " WHERE ID_TIP = ?";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("TIPS")).thenReturn(tipsTable);

            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            TipDAO dao = new TipDAO();
            TipDTO t = dao.obtenerPorId(99L);

            assertNull(t);
        }
    }

    @Test
    @DisplayName("crear con orden null inserta al final sin shift y devuelve ID")
    void crear_ordenNull() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psMax = mock(PreparedStatement.class);
        PreparedStatement psIns = mock(PreparedStatement.class);
        ResultSet rsMax = mock(ResultSet.class);
        ResultSet rsKeys = mock(ResultSet.class);

        String tipsTable = "TIPS";
        String sqlMax = "SELECT NVL(MAX(ORDEN), 0) AS MAXO FROM " + tipsTable;
        String sqlIns = "INSERT INTO " + tipsTable + " (TITULO, DESCRIPCION, ORDEN) VALUES (?,?,?)";

        TipDTO.Upsert dto = new TipDTO.Upsert();
        dto.titulo = "t";
        dto.descripcion = "d";
        dto.orden = null;

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("TIPS")).thenReturn(tipsTable);

            when(cn.prepareStatement(sqlMax)).thenReturn(psMax);
            when(psMax.executeQuery()).thenReturn(rsMax);
            when(rsMax.next()).thenReturn(true);
            when(rsMax.getInt("MAXO")).thenReturn(3); 

            when(cn.prepareStatement(sqlIns, new String[]{"ID_TIP"})).thenReturn(psIns);
            when(psIns.getGeneratedKeys()).thenReturn(rsKeys);
            when(rsKeys.next()).thenReturn(true);
            when(rsKeys.getLong(1)).thenReturn(10L);

            TipDAO dao = new TipDAO();
            long id = dao.crear(dto);

            assertEquals(10L, id);

            verify(psIns).setString(1, "t");
            verify(psIns).setString(2, "d");
            verify(psIns).setInt(3, 4);          
            verify(cn).commit();
        }
    }

    @Test
    @DisplayName("crear con orden muy alto normaliza a maxOrden+1 (branch dto.orden>maxOrden+1)")
    void crear_ordenMuyAlto_normaliza() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psMax = mock(PreparedStatement.class);
        PreparedStatement psIns = mock(PreparedStatement.class);
        ResultSet rsMax = mock(ResultSet.class);
        ResultSet rsKeys = mock(ResultSet.class);

        String tipsTable = "TIPS";
        String sqlMax = "SELECT NVL(MAX(ORDEN), 0) AS MAXO FROM " + tipsTable;
        String sqlIns = "INSERT INTO " + tipsTable + " (TITULO, DESCRIPCION, ORDEN) VALUES (?,?,?)";

        TipDTO.Upsert dto = new TipDTO.Upsert();
        dto.titulo = "t";
        dto.descripcion = "d";
        dto.orden = 100; 

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("TIPS")).thenReturn(tipsTable);

            when(cn.prepareStatement(sqlMax)).thenReturn(psMax);
            when(psMax.executeQuery()).thenReturn(rsMax);
            when(rsMax.next()).thenReturn(true);
            when(rsMax.getInt("MAXO")).thenReturn(5); 

            when(cn.prepareStatement(sqlIns, new String[]{"ID_TIP"})).thenReturn(psIns);
            when(psIns.getGeneratedKeys()).thenReturn(rsKeys);
            when(rsKeys.next()).thenReturn(true);
            when(rsKeys.getLong(1)).thenReturn(20L);

            TipDAO dao = new TipDAO();
            long id = dao.crear(dto);

            assertEquals(20L, id);
            verify(psIns).setInt(3, 6); 
        }
    }

    @Test
    @DisplayName("crear inserta en medio, hace shift de órdenes y devuelve ID")
    void crear_insertaEnMedio_haceShift() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psMax = mock(PreparedStatement.class);
        PreparedStatement psShift = mock(PreparedStatement.class);
        PreparedStatement psIns = mock(PreparedStatement.class);
        ResultSet rsMax = mock(ResultSet.class);
        ResultSet rsKeys = mock(ResultSet.class);

        String tipsTable = "TIPS";
        String sqlMax = "SELECT NVL(MAX(ORDEN), 0) AS MAXO FROM " + tipsTable;
        String sqlShift = "UPDATE " + tipsTable + " SET ORDEN = ORDEN + 1 WHERE ORDEN >= ?";
        String sqlIns = "INSERT INTO " + tipsTable + " (TITULO, DESCRIPCION, ORDEN) VALUES (?,?,?)";

        TipDTO.Upsert dto = new TipDTO.Upsert();
        dto.titulo = "t";
        dto.descripcion = "d";
        dto.orden = 2;  

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("TIPS")).thenReturn(tipsTable);

            when(cn.prepareStatement(sqlMax)).thenReturn(psMax);
            when(psMax.executeQuery()).thenReturn(rsMax);
            when(rsMax.next()).thenReturn(true);
            when(rsMax.getInt("MAXO")).thenReturn(5);  

            when(cn.prepareStatement(sqlShift)).thenReturn(psShift);
            when(cn.prepareStatement(sqlIns, new String[]{"ID_TIP"})).thenReturn(psIns);
            when(psIns.getGeneratedKeys()).thenReturn(rsKeys);
            when(rsKeys.next()).thenReturn(true);
            when(rsKeys.getLong(1)).thenReturn(30L);

            TipDAO dao = new TipDAO();
            long id = dao.crear(dto);

            assertEquals(30L, id);

            verify(psShift).setInt(1, 2);      
            verify(psShift).executeUpdate();
            verify(psIns).setInt(3, 2);
        }
    }

    @Test
    @DisplayName("crear lanza SQLException cuando no se genera ID_TIP")
    void crear_sinGeneratedKey_lanzaSQLException() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psMax = mock(PreparedStatement.class);
        PreparedStatement psIns = mock(PreparedStatement.class);
        ResultSet rsMax = mock(ResultSet.class);
        ResultSet rsKeys = mock(ResultSet.class);

        String tipsTable = "TIPS";
        String sqlMax = "SELECT NVL(MAX(ORDEN), 0) AS MAXO FROM " + tipsTable;
        String sqlIns = "INSERT INTO " + tipsTable + " (TITULO, DESCRIPCION, ORDEN) VALUES (?,?,?)";

        TipDTO.Upsert dto = new TipDTO.Upsert();
        dto.titulo = "t";
        dto.descripcion = "d";
        dto.orden = 1;

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("TIPS")).thenReturn(tipsTable);

            when(cn.prepareStatement(sqlMax)).thenReturn(psMax);
            when(psMax.executeQuery()).thenReturn(rsMax);
            when(rsMax.next()).thenReturn(true);
            when(rsMax.getInt("MAXO")).thenReturn(0);

            when(cn.prepareStatement(sqlIns, new String[]{"ID_TIP"})).thenReturn(psIns);
            when(psIns.getGeneratedKeys()).thenReturn(rsKeys);
            when(rsKeys.next()).thenReturn(false);

            TipDAO dao = new TipDAO();

            SQLException ex = assertThrows(SQLException.class, () -> dao.crear(dto));
            assertEquals("No se generó ID_TIP", ex.getMessage());
        }
    }

    @Test
    @DisplayName("crear envuelve excepciones no SQL en SQLException con mensaje de error")
    void crear_envuelveRuntimeException() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psMax = mock(PreparedStatement.class);

        String tipsTable = "TIPS";
        String sqlMax = "SELECT NVL(MAX(ORDEN), 0) AS MAXO FROM " + tipsTable;

        TipDTO.Upsert dto = new TipDTO.Upsert();
        dto.titulo = "t";
        dto.descripcion = "d";
        dto.orden = 1;

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("TIPS")).thenReturn(tipsTable);

            when(cn.prepareStatement(sqlMax)).thenReturn(psMax);
            when(psMax.executeQuery()).thenThrow(new RuntimeException("boom"));

            TipDAO dao = new TipDAO();
            SQLException ex = assertThrows(SQLException.class, () -> dao.crear(dto));
            assertEquals("Error al crear tip", ex.getMessage());
        }
    }

    

    @Test
    @DisplayName("actualizar con orden null mantiene ordenActual y no reordena")
    void actualizar_ordenNull_sinReordenar() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psSelOrden = mock(PreparedStatement.class);
        PreparedStatement psMax = mock(PreparedStatement.class);
        PreparedStatement psUp = mock(PreparedStatement.class);
        ResultSet rsSel = mock(ResultSet.class);
        ResultSet rsMax = mock(ResultSet.class);

        String tipsTable = "TIPS";
        String sqlSel = "SELECT ORDEN FROM " + tipsTable + " WHERE ID_TIP = ? FOR UPDATE";
        String sqlMax = "SELECT NVL(MAX(ORDEN), 0) AS MAXO FROM " + tipsTable;
        String sqlUp = "UPDATE " + tipsTable + " SET TITULO = ?, DESCRIPCION = ?, ORDEN = ? WHERE ID_TIP = ?";

        TipDTO.Upsert dto = new TipDTO.Upsert();
        dto.titulo = "nuevo";
        dto.descripcion = "desc";
        dto.orden = null;

        long idTip = 7L;

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("TIPS")).thenReturn(tipsTable);

            when(cn.prepareStatement(sqlSel)).thenReturn(psSelOrden);
            when(cn.prepareStatement(sqlMax)).thenReturn(psMax);
            when(cn.prepareStatement(sqlUp)).thenReturn(psUp);

            when(psSelOrden.executeQuery()).thenReturn(rsSel);
            when(rsSel.next()).thenReturn(true);
            when(rsSel.getInt("ORDEN")).thenReturn(5); 

            when(psMax.executeQuery()).thenReturn(rsMax);
            when(rsMax.next()).thenReturn(true);
            when(rsMax.getInt("MAXO")).thenReturn(5);

            when(psUp.executeUpdate()).thenReturn(1);

            TipDAO dao = new TipDAO();
            dao.actualizar(idTip, dto);

            
            verify(psUp).setInt(3, 5); 
            verify(cn).commit();
        }
    }

    @Test
    @DisplayName("actualizar mueve tip hacia arriba y ejecuta UPDATE de shift + update final")
    void actualizar_mueveArriba() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psSelOrden = mock(PreparedStatement.class);
        PreparedStatement psMax = mock(PreparedStatement.class);
        PreparedStatement psShift = mock(PreparedStatement.class);
        PreparedStatement psUp = mock(PreparedStatement.class);
        ResultSet rsSel = mock(ResultSet.class);
        ResultSet rsMax = mock(ResultSet.class);

        String tipsTable = "TIPS";
        String sqlSel = "SELECT ORDEN FROM " + tipsTable + " WHERE ID_TIP = ? FOR UPDATE";
        String sqlMax = "SELECT NVL(MAX(ORDEN), 0) AS MAXO FROM " + tipsTable;
        String sqlShiftUp = "UPDATE " + tipsTable +
                " SET ORDEN = ORDEN + 1 WHERE ORDEN >= ? AND ORDEN < ? AND ID_TIP <> ?";
        String sqlUp = "UPDATE " + tipsTable + " SET TITULO = ?, DESCRIPCION = ?, ORDEN = ? WHERE ID_TIP = ?";

        TipDTO.Upsert dto = new TipDTO.Upsert();
        dto.titulo = "t";
        dto.descripcion = "d";
        dto.orden = 2; 

        long idTip = 10L;

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("TIPS")).thenReturn(tipsTable);

            when(cn.prepareStatement(sqlSel)).thenReturn(psSelOrden);
            when(cn.prepareStatement(sqlMax)).thenReturn(psMax);
            when(cn.prepareStatement(sqlShiftUp)).thenReturn(psShift);
            when(cn.prepareStatement(sqlUp)).thenReturn(psUp);

            when(psSelOrden.executeQuery()).thenReturn(rsSel);
            when(rsSel.next()).thenReturn(true);
            when(rsSel.getInt("ORDEN")).thenReturn(5); 

            when(psMax.executeQuery()).thenReturn(rsMax);
            when(rsMax.next()).thenReturn(true);
            when(rsMax.getInt("MAXO")).thenReturn(8);

            when(psUp.executeUpdate()).thenReturn(1);

            TipDAO dao = new TipDAO();
            dao.actualizar(idTip, dto);

            verify(psShift).setInt(1, 2);
            verify(psShift).setInt(2, 5);
            verify(psShift).setLong(3, idTip);
            verify(psShift).executeUpdate();
            verify(psUp).setInt(3, 2);
        }
    }

    @Test
    @DisplayName("actualizar mueve tip hacia abajo cuando nuevoOrden>ordenActual")
    void actualizar_mueveAbajo() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psSelOrden = mock(PreparedStatement.class);
        PreparedStatement psMax = mock(PreparedStatement.class);
        PreparedStatement psShiftDown = mock(PreparedStatement.class);
        PreparedStatement psUp = mock(PreparedStatement.class);
        ResultSet rsSel = mock(ResultSet.class);
        ResultSet rsMax = mock(ResultSet.class);

        String tipsTable = "TIPS";
        String sqlSel = "SELECT ORDEN FROM " + tipsTable + " WHERE ID_TIP = ? FOR UPDATE";
        String sqlMax = "SELECT NVL(MAX(ORDEN), 0) AS MAXO FROM " + tipsTable;
        String sqlShiftDown = "UPDATE " + tipsTable +
                " SET ORDEN = ORDEN - 1 WHERE ORDEN <= ? AND ORDEN > ? AND ID_TIP <> ?";
        String sqlUp = "UPDATE " + tipsTable + " SET TITULO = ?, DESCRIPCION = ?, ORDEN = ? WHERE ID_TIP = ?";

        TipDTO.Upsert dto = new TipDTO.Upsert();
        dto.titulo = "t";
        dto.descripcion = "d";
        dto.orden = 7; 

        long idTip = 11L;

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("TIPS")).thenReturn(tipsTable);

            when(cn.prepareStatement(sqlSel)).thenReturn(psSelOrden);
            when(cn.prepareStatement(sqlMax)).thenReturn(psMax);
            when(cn.prepareStatement(sqlShiftDown)).thenReturn(psShiftDown);
            when(cn.prepareStatement(sqlUp)).thenReturn(psUp);

            when(psSelOrden.executeQuery()).thenReturn(rsSel);
            when(rsSel.next()).thenReturn(true);
            when(rsSel.getInt("ORDEN")).thenReturn(3); 

            when(psMax.executeQuery()).thenReturn(rsMax);
            when(rsMax.next()).thenReturn(true);
            when(rsMax.getInt("MAXO")).thenReturn(7); 

            when(psUp.executeUpdate()).thenReturn(1);

            TipDAO dao = new TipDAO();
            dao.actualizar(idTip, dto);

            verify(psShiftDown).setInt(1, 7);
            verify(psShiftDown).setInt(2, 3);
            verify(psShiftDown).setLong(3, idTip);
            verify(psShiftDown).executeUpdate();
            verify(psUp).setInt(3, 7);
        }
    }

    @Test
    @DisplayName("actualizar lanza SQLException Tip no encontrado cuando SELECT inicial no devuelve filas")
    void actualizar_sinFilaEnSelect_lanza() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psSel = mock(PreparedStatement.class);
        ResultSet rsSel = mock(ResultSet.class);

        String tipsTable = "TIPS";
        String sqlSel = "SELECT ORDEN FROM " + tipsTable + " WHERE ID_TIP = ? FOR UPDATE";

        TipDTO.Upsert dto = new TipDTO.Upsert();
        dto.titulo = "t";
        dto.descripcion = "d";
        dto.orden = 1;

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("TIPS")).thenReturn(tipsTable);

            when(cn.prepareStatement(sqlSel)).thenReturn(psSel);
            when(psSel.executeQuery()).thenReturn(rsSel);
            when(rsSel.next()).thenReturn(false);

            TipDAO dao = new TipDAO();
            SQLException ex = assertThrows(SQLException.class, () -> dao.actualizar(1L, dto));
            assertEquals("Tip no encontrado", ex.getMessage());
        }
    }

    @Test
    @DisplayName("actualizar lanza SQLException Tip no encontrado cuando UPDATE final afecta 0 filas")
    void actualizar_update0_lanza() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psSelOrden = mock(PreparedStatement.class);
        PreparedStatement psMax = mock(PreparedStatement.class);
        PreparedStatement psUp = mock(PreparedStatement.class);
        ResultSet rsSel = mock(ResultSet.class);
        ResultSet rsMax = mock(ResultSet.class);

        String tipsTable = "TIPS";
        String sqlSel = "SELECT ORDEN FROM " + tipsTable + " WHERE ID_TIP = ? FOR UPDATE";
        String sqlMax = "SELECT NVL(MAX(ORDEN), 0) AS MAXO FROM " + tipsTable;
        String sqlUp = "UPDATE " + tipsTable + " SET TITULO = ?, DESCRIPCION = ?, ORDEN = ? WHERE ID_TIP = ?";

        TipDTO.Upsert dto = new TipDTO.Upsert();
        dto.titulo = "t";
        dto.descripcion = "d";
        dto.orden = 1;

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("TIPS")).thenReturn(tipsTable);

            when(cn.prepareStatement(sqlSel)).thenReturn(psSelOrden);
            when(cn.prepareStatement(sqlMax)).thenReturn(psMax);
            when(cn.prepareStatement(sqlUp)).thenReturn(psUp);

            when(psSelOrden.executeQuery()).thenReturn(rsSel);
            when(rsSel.next()).thenReturn(true);
            when(rsSel.getInt("ORDEN")).thenReturn(1);

            when(psMax.executeQuery()).thenReturn(rsMax);
            when(rsMax.next()).thenReturn(true);
            when(rsMax.getInt("MAXO")).thenReturn(5);

            when(psUp.executeUpdate()).thenReturn(0);

            TipDAO dao = new TipDAO();
            SQLException ex = assertThrows(SQLException.class, () -> dao.actualizar(1L, dto));
            assertEquals("Tip no encontrado", ex.getMessage());
        }
    }

    @Test
    @DisplayName("actualizar envuelve excepciones no SQL en SQLException con mensaje de error")
    void actualizar_envuelveRuntime() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psSel = mock(PreparedStatement.class);

        String tipsTable = "TIPS";
        String sqlSel = "SELECT ORDEN FROM " + tipsTable + " WHERE ID_TIP = ? FOR UPDATE";

        TipDTO.Upsert dto = new TipDTO.Upsert();
        dto.titulo = "t";
        dto.descripcion = "d";
        dto.orden = 1;

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("TIPS")).thenReturn(tipsTable);

            when(cn.prepareStatement(sqlSel)).thenReturn(psSel);
            when(psSel.executeQuery()).thenThrow(new RuntimeException("boom"));

            TipDAO dao = new TipDAO();
            SQLException ex = assertThrows(SQLException.class, () -> dao.actualizar(1L, dto));
            assertEquals("Error al actualizar tip", ex.getMessage());
        }
    }

    

    @Test
    @DisplayName("eliminar obtiene orden, borra y reacomoda órdenes cuando orden no es null")
    void eliminar_conOrden_reacomoda() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psSel = mock(PreparedStatement.class);
        PreparedStatement psDel = mock(PreparedStatement.class);
        PreparedStatement psShift = mock(PreparedStatement.class);
        ResultSet rsSel = mock(ResultSet.class);

        String tipsTable = "TIPS";
        String sqlSel = "SELECT ORDEN FROM " + tipsTable + " WHERE ID_TIP = ?";
        String sqlDel = "DELETE FROM " + tipsTable + " WHERE ID_TIP = ?";
        String sqlShift = "UPDATE " + tipsTable + " SET ORDEN = ORDEN - 1 WHERE ORDEN > ?";

        long idTip = 9L;

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("TIPS")).thenReturn(tipsTable);

            when(cn.prepareStatement(sqlSel)).thenReturn(psSel);
            when(cn.prepareStatement(sqlDel)).thenReturn(psDel);
            when(cn.prepareStatement(sqlShift)).thenReturn(psShift);

            when(psSel.executeQuery()).thenReturn(rsSel);
            when(rsSel.next()).thenReturn(true);
            when(rsSel.getInt("ORDEN")).thenReturn(3);

            TipDAO dao = new TipDAO();
            dao.eliminar(idTip);

            verify(psDel).setLong(1, idTip);
            verify(psDel).executeUpdate();
            verify(psShift).setInt(1, 3);
            verify(psShift).executeUpdate();
            verify(cn).commit();
        }
    }

    @Test
    @DisplayName("eliminar sigue sin shift cuando no hay fila previa (orden permanece null)")
    void eliminar_sinFila_previa() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psSel = mock(PreparedStatement.class);
        PreparedStatement psDel = mock(PreparedStatement.class);
        PreparedStatement psShift = mock(PreparedStatement.class);
        ResultSet rsSel = mock(ResultSet.class);

        String tipsTable = "TIPS";
        String sqlSel = "SELECT ORDEN FROM " + tipsTable + " WHERE ID_TIP = ?";
        String sqlDel = "DELETE FROM " + tipsTable + " WHERE ID_TIP = ?";
        String sqlShift = "UPDATE " + tipsTable + " SET ORDEN = ORDEN - 1 WHERE ORDEN > ?";

        long idTip = 9L;

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("TIPS")).thenReturn(tipsTable);

            when(cn.prepareStatement(sqlSel)).thenReturn(psSel);
            when(cn.prepareStatement(sqlDel)).thenReturn(psDel);
            when(cn.prepareStatement(sqlShift)).thenReturn(psShift);

            when(psSel.executeQuery()).thenReturn(rsSel);
            when(rsSel.next()).thenReturn(false); 

            TipDAO dao = new TipDAO();
            dao.eliminar(idTip);

            verify(psDel).setLong(1, idTip);
            verify(psDel).executeUpdate();
            verify(psShift, never()).executeUpdate(); 
            verify(cn).commit();
        }
    }

    @Test
    @DisplayName("eliminar envuelve excepciones no SQL en SQLException con mensaje de error")
    void eliminar_envuelveRuntime() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psSel = mock(PreparedStatement.class);

        String tipsTable = "TIPS";
        String sqlSel = "SELECT ORDEN FROM " + tipsTable + " WHERE ID_TIP = ?";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("TIPS")).thenReturn(tipsTable);

            when(cn.prepareStatement(sqlSel)).thenReturn(psSel);
            when(psSel.executeQuery()).thenThrow(new RuntimeException("boom"));

            TipDAO dao = new TipDAO();
            SQLException ex = assertThrows(SQLException.class, () -> dao.eliminar(1L));
            assertEquals("Error al eliminar tip", ex.getMessage());
        }
    }
}
