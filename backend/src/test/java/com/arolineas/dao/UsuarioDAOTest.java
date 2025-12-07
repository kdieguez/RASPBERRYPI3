package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.dto.UsuarioAdminDTOs;
import com.aerolineas.dto.VueloDTO;
import com.aerolineas.model.Usuario;
import com.aerolineas.util.PasswordUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioDAOTest {

    @Test
    @DisplayName("findByEmail devuelve usuario cuando existe")
    void findByEmail_found() throws Exception {
        String email = "test@x.com";

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        String usuarioTable = "USUARIO";
        String sql = "SELECT ID_USUARIO, EMAIL, CONTRASENA, NOMBRES, APELLIDOS, HABILITADO, ID_ROL FROM "
                + usuarioTable + " WHERE LOWER(EMAIL) = LOWER(?)";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("USUARIO")).thenReturn(usuarioTable);

            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true);
            when(rs.getLong("ID_USUARIO")).thenReturn(1L);
            when(rs.getString("EMAIL")).thenReturn(email);
            when(rs.getString("CONTRASENA")).thenReturn("hash");
            when(rs.getString("NOMBRES")).thenReturn("Nombre");
            when(rs.getString("APELLIDOS")).thenReturn("Apellido");
            when(rs.getInt("HABILITADO")).thenReturn(1);
            when(rs.getInt("ID_ROL")).thenReturn(3);

            UsuarioDAO dao = new UsuarioDAO();
            Usuario u = dao.findByEmail(email);

            assertNotNull(u);
            assertEquals(1L, u.getIdUsuario());
            assertEquals(email, u.getEmail());
            assertEquals("hash", u.getContrasenaHash());
            assertEquals("Nombre", u.getNombres());
            assertEquals("Apellido", u.getApellidos());
            assertTrue(u.isHabilitado());
            assertEquals(3, u.getIdRol());
        }
    }

    @Test
    @DisplayName("findByEmail devuelve null cuando no existe")
    void findByEmail_notFound() throws Exception {
        String email = "no@x.com";

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        String usuarioTable = "USUARIO";
        String sql = "SELECT ID_USUARIO, EMAIL, CONTRASENA, NOMBRES, APELLIDOS, HABILITADO, ID_ROL FROM "
                + usuarioTable + " WHERE LOWER(EMAIL) = LOWER(?)";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("USUARIO")).thenReturn(usuarioTable);

            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            UsuarioDAO dao = new UsuarioDAO();
            Usuario u = dao.findByEmail(email);

            assertNull(u);
        }
    }

    @Test
    @DisplayName("findByEmail envuelve SQLException en RuntimeException")
    void findByEmail_sqlException() throws Exception {
        String email = "err@x.com";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenThrow(new SQLException("boom"));
            db.when(() -> DB.table("USUARIO")).thenReturn("USUARIO");

            UsuarioDAO dao = new UsuarioDAO();
            assertThrows(RuntimeException.class, () -> dao.findByEmail(email));
        }
    }

    @Test
    @DisplayName("create inserta usuario y luego lo busca por email")
    void create_ok() throws Exception {
        String email = "new@x.com";

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        String usuarioTable = "USUARIO";
        String insert = "INSERT INTO " + usuarioTable +
                " (EMAIL, CONTRASENA, NOMBRES, APELLIDOS, HABILITADO, ID_ROL) VALUES (?, ?, ?, ?, 1, 3)";

        Usuario resultado = new Usuario();
        resultado.setIdUsuario(5L);
        resultado.setEmail(email);

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("USUARIO")).thenReturn(usuarioTable);

            when(cn.prepareStatement(insert)).thenReturn(ps);

            UsuarioDAO dao = spy(new UsuarioDAO());
            doReturn(resultado).when(dao).findByEmail(email);

            Usuario u = dao.create(email, "hash", "Nom", "Ape");

            assertSame(resultado, u);
            verify(ps).setString(1, email);
            verify(ps).setString(2, "hash");
            verify(ps).setString(3, "Nom");
            verify(ps).setString(4, "Ape");
            verify(ps).executeUpdate();
        }
    }

    @Test
    @DisplayName("create envuelve SQLException en RuntimeException")
    void create_sqlException() throws Exception {
        String email = "x@x.com";

        Connection cn = mock(Connection.class);
        String usuarioTable = "USUARIO";
        String insert = "INSERT INTO " + usuarioTable +
                " (EMAIL, CONTRASENA, NOMBRES, APELLIDOS, HABILITADO, ID_ROL) VALUES (?, ?, ?, ?, 1, 3)";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("USUARIO")).thenReturn(usuarioTable);

            when(cn.prepareStatement(insert)).thenThrow(new SQLException("boom"));

            UsuarioDAO dao = new UsuarioDAO();
            assertThrows(RuntimeException.class,
                    () -> dao.create(email, "h", "N", "A"));
        }
    }

    @Test
    @DisplayName("createWithRole inserta usuario con rol específico y lo devuelve")
    void createWithRole_ok() throws Exception {
        String email = "role@x.com";

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        String usuarioTable = "USUARIO";
        String insert = "INSERT INTO " + usuarioTable +
                " (EMAIL, CONTRASENA, NOMBRES, APELLIDOS, HABILITADO, ID_ROL) VALUES (?, ?, ?, ?, 1, ?)";

        Usuario esperado = new Usuario();
        esperado.setIdUsuario(9L);
        esperado.setEmail(email);

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("USUARIO")).thenReturn(usuarioTable);
            when(cn.prepareStatement(insert)).thenReturn(ps);

            UsuarioDAO dao = spy(new UsuarioDAO());
            doReturn(esperado).when(dao).findByEmail(email);

            Usuario u = dao.createWithRole(email, "hash", "Nom", "Ape", 2);

            assertSame(esperado, u);
            verify(ps).setString(1, email);
            verify(ps).setString(2, "hash");
            verify(ps).setString(3, "Nom");
            verify(ps).setString(4, "Ape");
            verify(ps).setInt(5, 2);
            verify(ps).executeUpdate();
        }
    }

    @Test
    @DisplayName("createWithRole envuelve SQLException en RuntimeException")
    void createWithRole_sqlException() throws Exception {
        String email = "err2@x.com";

        Connection cn = mock(Connection.class);
        String usuarioTable = "USUARIO";
        String insert = "INSERT INTO " + usuarioTable +
                " (EMAIL, CONTRASENA, NOMBRES, APELLIDOS, HABILITADO, ID_ROL) VALUES (?, ?, ?, ?, 1, ?)";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("USUARIO")).thenReturn(usuarioTable);
            when(cn.prepareStatement(insert)).thenThrow(new SQLException("boom"));

            UsuarioDAO dao = new UsuarioDAO();
            assertThrows(RuntimeException.class,
                    () -> dao.createWithRole(email, "h", "N", "A", 1));
        }
    }

    @Test
    @DisplayName("findById devuelve usuario cuando existe")
    void findById_found() throws Exception {
        long id = 3L;

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        String usuarioTable = "USUARIO";
        String sql = "SELECT ID_USUARIO, EMAIL, CONTRASENA, NOMBRES, APELLIDOS, HABILITADO, ID_ROL FROM " +
                usuarioTable + " WHERE ID_USUARIO = ?";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("USUARIO")).thenReturn(usuarioTable);
            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true);
            when(rs.getLong("ID_USUARIO")).thenReturn(id);
            when(rs.getString("EMAIL")).thenReturn("x@x.com");
            when(rs.getString("CONTRASENA")).thenReturn("h");
            when(rs.getString("NOMBRES")).thenReturn("N");
            when(rs.getString("APELLIDOS")).thenReturn("A");
            when(rs.getInt("HABILITADO")).thenReturn(1);
            when(rs.getInt("ID_ROL")).thenReturn(2);

            UsuarioDAO dao = new UsuarioDAO();
            Usuario u = dao.findById(id);

            assertNotNull(u);
            assertEquals(id, u.getIdUsuario());
        }
    }

    @Test
    @DisplayName("findById devuelve null cuando no existe")
    void findById_notFound() throws Exception {
        long id = 33L;

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        String usuarioTable = "USUARIO";
        String sql = "SELECT ID_USUARIO, EMAIL, CONTRASENA, NOMBRES, APELLIDOS, HABILITADO, ID_ROL FROM " +
                usuarioTable + " WHERE ID_USUARIO = ?";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("USUARIO")).thenReturn(usuarioTable);
            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            UsuarioDAO dao = new UsuarioDAO();
            Usuario u = dao.findById(id);
            assertNull(u);
        }
    }

    @Test
    @DisplayName("adminList sin filtro devuelve filas paginadas")
    void adminList_sinQ() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        String usuarioTable = "USUARIO";
        String rolTable = "ROL";
        String base = "SELECT u.ID_USUARIO, u.EMAIL, u.NOMBRES, u.APELLIDOS, u.ID_ROL, r.NOMBRE AS ROL_NOMBRE, u.HABILITADO " +
                "FROM " + usuarioTable + " u JOIN " + rolTable + " r ON r.ID_ROL = u.ID_ROL WHERE 1=1 ";
        String paging = " ORDER BY u.ID_USUARIO DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        String sql = base + "" + paging;

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(() -> DB.table("USUARIO")).thenReturn(usuarioTable);
            db.when(() -> DB.table("ROL")).thenReturn(rolTable);
            db.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true, false);
            when(rs.getLong("ID_USUARIO")).thenReturn(1L);
            when(rs.getString("EMAIL")).thenReturn("a@x.com");
            when(rs.getString("NOMBRES")).thenReturn("A");
            when(rs.getString("APELLIDOS")).thenReturn("B");
            when(rs.getInt("ID_ROL")).thenReturn(2);
            when(rs.getString("ROL_NOMBRE")).thenReturn("Empleado");
            when(rs.getInt("HABILITADO")).thenReturn(1);

            UsuarioDAO dao = new UsuarioDAO();
            List<UsuarioAdminDTOs.Row> out = dao.adminList(null, 0, 10);

            assertEquals(1, out.size());
            UsuarioAdminDTOs.Row r = out.get(0);
            assertEquals(1L, r.idUsuario());
            verify(ps).setInt(1, 0);
            verify(ps).setInt(2, 10);
        }
    }

    @Test
    @DisplayName("adminList con filtro q agrega condiciones LIKE")
    void adminList_conQ() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        String usuarioTable = "USUARIO";
        String rolTable = "ROL";
        String base = "SELECT u.ID_USUARIO, u.EMAIL, u.NOMBRES, u.APELLIDOS, u.ID_ROL, r.NOMBRE AS ROL_NOMBRE, u.HABILITADO " +
                "FROM " + usuarioTable + " u JOIN " + rolTable + " r ON r.ID_ROL = u.ID_ROL WHERE 1=1 ";
        String where = " AND (LOWER(u.EMAIL) LIKE ? OR LOWER(u.NOMBRES) LIKE ? OR LOWER(u.APELLIDOS) LIKE ?) ";
        String paging = " ORDER BY u.ID_USUARIO DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        String sql = base + where + paging;

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(() -> DB.table("USUARIO")).thenReturn(usuarioTable);
            db.when(() -> DB.table("ROL")).thenReturn(rolTable);
            db.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            UsuarioDAO dao = new UsuarioDAO();
            dao.adminList("juan", 5, 20);

            String like = "%juan%";
            verify(ps).setString(1, like);
            verify(ps).setString(2, like);
            verify(ps).setString(3, like);
            verify(ps).setInt(4, 5);
            verify(ps).setInt(5, 20);
        }
    }

    @Test
    @DisplayName("adminGet devuelve View mapeada cuando existe")
    void adminGet_found() throws Exception {
        long id = 7L;

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        String usuarioTable = "USUARIO";
        String rolTable = "ROL";
        String pasajeroTable = "PASAJERO";
        String sql = "SELECT u.ID_USUARIO, u.EMAIL, u.NOMBRES, u.APELLIDOS, u.ID_ROL, r.NOMBRE AS ROL_NOMBRE, u.HABILITADO, " +
                "p.FECHA_NACIMIENTO, p.ID_PAIS_DOCUMENTO, p.PASAPORTE FROM " + usuarioTable +
                " u JOIN " + rolTable + " r ON r.ID_ROL = u.ID_ROL LEFT JOIN " + pasajeroTable +
                " p ON p.ID_USUARIO = u.ID_USUARIO WHERE u.ID_USUARIO = ?";

        Date fecha = Date.valueOf("2000-01-02");

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(() -> DB.table("USUARIO")).thenReturn(usuarioTable);
            db.when(() -> DB.table("ROL")).thenReturn(rolTable);
            db.when(() -> DB.table("PASAJERO")).thenReturn(pasajeroTable);
            db.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true);
            when(rs.getLong("ID_USUARIO")).thenReturn(id);
            when(rs.getString("EMAIL")).thenReturn("u@x.com");
            when(rs.getString("NOMBRES")).thenReturn("Nom");
            when(rs.getString("APELLIDOS")).thenReturn("Ape");
            when(rs.getInt("ID_ROL")).thenReturn(2);
            when(rs.getString("ROL_NOMBRE")).thenReturn("Empleado");
            when(rs.getInt("HABILITADO")).thenReturn(1);
            when(rs.getDate("FECHA_NACIMIENTO")).thenReturn(fecha);
            when(rs.getObject("ID_PAIS_DOCUMENTO")).thenReturn(10L);
            when(rs.getLong("ID_PAIS_DOCUMENTO")).thenReturn(10L);
            when(rs.getString("PASAPORTE")).thenReturn("P123");

            UsuarioDAO dao = new UsuarioDAO();
            UsuarioAdminDTOs.View v = dao.adminGet(id);

            assertNotNull(v);
            assertEquals(id, v.idUsuario());
            assertEquals("2000-01-02", v.fechaNacimiento());
            assertEquals(10L, v.idPais());
            assertEquals("P123", v.pasaporte());
        }
    }

    @Test
    @DisplayName("adminGet devuelve null cuando no existe")
    void adminGet_notFound() throws Exception {
        long id = 8L;

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        String usuarioTable = "USUARIO";
        String rolTable = "ROL";
        String pasajeroTable = "PASAJERO";
        String sql = "SELECT u.ID_USUARIO, u.EMAIL, u.NOMBRES, u.APELLIDOS, u.ID_ROL, r.NOMBRE AS ROL_NOMBRE, u.HABILITADO, " +
                "p.FECHA_NACIMIENTO, p.ID_PAIS_DOCUMENTO, p.PASAPORTE FROM " + usuarioTable +
                " u JOIN " + rolTable + " r ON r.ID_ROL = u.ID_ROL LEFT JOIN " + pasajeroTable +
                " p ON p.ID_USUARIO = u.ID_USUARIO WHERE u.ID_USUARIO = ?";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(() -> DB.table("USUARIO")).thenReturn(usuarioTable);
            db.when(() -> DB.table("ROL")).thenReturn(rolTable);
            db.when(() -> DB.table("PASAJERO")).thenReturn(pasajeroTable);
            db.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            UsuarioDAO dao = new UsuarioDAO();
            UsuarioAdminDTOs.View v = dao.adminGet(id);
            assertNull(v);
        }
    }

    @Test
    @DisplayName("adminUpdate actualiza usuario sin cambiar contraseña e inserta pasajero cuando no existe")
    void adminUpdate_sinNewPassword_insertaPasajero() throws Exception {
        long id = 10L;

        Connection cn = mock(Connection.class);
        PreparedStatement psUser = mock(PreparedStatement.class);
        PreparedStatement psSelPas = mock(PreparedStatement.class);
        PreparedStatement psInsPas = mock(PreparedStatement.class);
        ResultSet rsSel = mock(ResultSet.class);

        String usuarioTable = "USUARIO";
        String pasajeroTable = "PASAJERO";

        String upUser = "UPDATE " + usuarioTable +
                " SET NOMBRES=?, APELLIDOS=?, ID_ROL=?, HABILITADO=? WHERE ID_USUARIO=?";
        String selPas = "SELECT 1 FROM " + pasajeroTable + " WHERE ID_USUARIO=?";
        String insPas = "INSERT INTO " + pasajeroTable +
                " (FECHA_NACIMIENTO, ID_PAIS_DOCUMENTO, PASAPORTE, ID_USUARIO) VALUES (?,?,?,?)";

        UsuarioAdminDTOs.UpdateAdmin dto = mock(UsuarioAdminDTOs.UpdateAdmin.class);
        when(dto.nombres()).thenReturn(" Nom ");
        when(dto.apellidos()).thenReturn(" Ape ");
        when(dto.idRol()).thenReturn(2);
        when(dto.habilitado()).thenReturn(1);
        when(dto.newPassword()).thenReturn(null);
        when(dto.fechaNacimiento()).thenReturn("2000-01-02");
        when(dto.idPais()).thenReturn(15L);
        when(dto.pasaporte()).thenReturn("P999");

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("USUARIO")).thenReturn(usuarioTable);
            db.when(() -> DB.table("PASAJERO")).thenReturn(pasajeroTable);

            when(cn.prepareStatement(upUser)).thenReturn(psUser);
            when(cn.prepareStatement(selPas)).thenReturn(psSelPas);
            when(cn.prepareStatement(insPas)).thenReturn(psInsPas);

            when(psSelPas.executeQuery()).thenReturn(rsSel);
            when(rsSel.next()).thenReturn(false);

            UsuarioDAO dao = new UsuarioDAO();
            dao.adminUpdate(id, dto);

            verify(cn).setAutoCommit(false);
            verify(psUser).setString(1, "Nom");
            verify(psUser).setString(2, "Ape");
            verify(psUser).setInt(3, 2);
            verify(psUser).setInt(4, 1);
            verify(psUser).setLong(5, id);

            verify(psInsPas).setDate(eq(1), any(Date.class));
            verify(psInsPas).setLong(2, 15L);
            verify(psInsPas).setString(3, "P999");
            verify(psInsPas).setLong(4, id);
            verify(cn).commit();
            verify(cn).setAutoCommit(true);
        }
    }

    @Test
    @DisplayName("adminUpdate con newPassword usa PasswordUtil.hash y actualiza pasajero existente")
    void adminUpdate_conNewPassword_actualizaPasajero() throws Exception {
        long id = 11L;

        Connection cn = mock(Connection.class);
        PreparedStatement psUser = mock(PreparedStatement.class);
        PreparedStatement psSelPas = mock(PreparedStatement.class);
        PreparedStatement psUpdPas = mock(PreparedStatement.class);
        ResultSet rsSel = mock(ResultSet.class);

        String usuarioTable = "USUARIO";
        String pasajeroTable = "PASAJERO";

        String upUser = "UPDATE " + usuarioTable +
                " SET NOMBRES=?, APELLIDOS=?, ID_ROL=?, HABILITADO=?, CONTRASENA=? WHERE ID_USUARIO=?";
        String selPas = "SELECT 1 FROM " + pasajeroTable + " WHERE ID_USUARIO=?";
        String updPas = "UPDATE " + pasajeroTable +
                " SET FECHA_NACIMIENTO=?, ID_PAIS_DOCUMENTO=?, PASAPORTE=? WHERE ID_USUARIO=?";

        UsuarioAdminDTOs.UpdateAdmin dto = mock(UsuarioAdminDTOs.UpdateAdmin.class);
        when(dto.nombres()).thenReturn("Nom");
        when(dto.apellidos()).thenReturn("Ape");
        when(dto.idRol()).thenReturn(3);
        when(dto.habilitado()).thenReturn(0);
        when(dto.newPassword()).thenReturn("newpw");
        when(dto.fechaNacimiento()).thenReturn(null);
        when(dto.idPais()).thenReturn(null);
        when(dto.pasaporte()).thenReturn("P0");

        try (MockedStatic<DB> db = mockStatic(DB.class);
             MockedStatic<PasswordUtil> pw = mockStatic(PasswordUtil.class)) {

            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("USUARIO")).thenReturn(usuarioTable);
            db.when(() -> DB.table("PASAJERO")).thenReturn(pasajeroTable);

            when(cn.prepareStatement(upUser)).thenReturn(psUser);
            when(cn.prepareStatement(selPas)).thenReturn(psSelPas);
            when(cn.prepareStatement(updPas)).thenReturn(psUpdPas);

            when(psSelPas.executeQuery()).thenReturn(rsSel);
            when(rsSel.next()).thenReturn(true);

            pw.when(() -> PasswordUtil.hash("newpw")).thenReturn("HASHED");

            UsuarioDAO dao = new UsuarioDAO();
            dao.adminUpdate(id, dto);

            verify(psUser).setString(5, "HASHED");

            verify(psUpdPas).setNull(1, Types.DATE);
            verify(psUpdPas).setNull(2, Types.NUMERIC);
            verify(psUpdPas).setString(3, "P0");
            verify(psUpdPas).setLong(4, id);
            verify(cn).commit();
        }
    }

    @Test
    @DisplayName("selfUpdate actualiza nombres sin cambiar contraseña e inserta pasajero si no existe")
    void selfUpdate_sinNewPassword_insertaPasajero() throws Exception {
        long id = 20L;

        Connection cn = mock(Connection.class);
        PreparedStatement psUser = mock(PreparedStatement.class);
        PreparedStatement psSelPas = mock(PreparedStatement.class);
        PreparedStatement psInsPas = mock(PreparedStatement.class);
        ResultSet rsSel = mock(ResultSet.class);

        String usuarioTable = "USUARIO";
        String pasajeroTable = "PASAJERO";

        String upUser = "UPDATE " + usuarioTable + " SET NOMBRES=?, APELLIDOS=? WHERE ID_USUARIO=?";
        String selPas = "SELECT 1 FROM " + pasajeroTable + " WHERE ID_USUARIO=?";
        String insPas = "INSERT INTO " + pasajeroTable +
                " (FECHA_NACIMIENTO, ID_PAIS_DOCUMENTO, PASAPORTE, ID_USUARIO) VALUES (?,?,?,?)";

        UsuarioAdminDTOs.UpdateSelf dto = mock(UsuarioAdminDTOs.UpdateSelf.class);
        when(dto.nombres()).thenReturn("Zoe");
        when(dto.apellidos()).thenReturn("X");
        when(dto.newPassword()).thenReturn(null);
        when(dto.fechaNacimiento()).thenReturn("1999-05-01");
        when(dto.idPais()).thenReturn(55L);
        when(dto.pasaporte()).thenReturn("PP");

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("USUARIO")).thenReturn(usuarioTable);
            db.when(() -> DB.table("PASAJERO")).thenReturn(pasajeroTable);

            when(cn.prepareStatement(upUser)).thenReturn(psUser);
            when(cn.prepareStatement(selPas)).thenReturn(psSelPas);
            when(cn.prepareStatement(insPas)).thenReturn(psInsPas);

            when(psSelPas.executeQuery()).thenReturn(rsSel);
            when(rsSel.next()).thenReturn(false);

            UsuarioDAO dao = new UsuarioDAO();
            dao.selfUpdate(id, dto);

            verify(psUser).setString(1, "Zoe");
            verify(psUser).setString(2, "X");
            verify(psUser).setLong(3, id);

            verify(psInsPas).setDate(eq(1), any(Date.class));
            verify(psInsPas).setLong(2, 55L);
            verify(psInsPas).setString(3, "PP");
            verify(psInsPas).setLong(4, id);
            verify(cn).commit();
        }
    }

    @Test
    @DisplayName("selfUpdate con newPassword usa PasswordUtil.hash y actualiza pasajero existente")
    void selfUpdate_conNewPassword_actualizaPasajero() throws Exception {
        long id = 21L;

        Connection cn = mock(Connection.class);
        PreparedStatement psUser = mock(PreparedStatement.class);
        PreparedStatement psSelPas = mock(PreparedStatement.class);
        PreparedStatement psUpdPas = mock(PreparedStatement.class);
        ResultSet rsSel = mock(ResultSet.class);

        String usuarioTable = "USUARIO";
        String pasajeroTable = "PASAJERO";

        String upUser = "UPDATE " + usuarioTable +
                " SET NOMBRES=?, APELLIDOS=?, CONTRASENA=? WHERE ID_USUARIO=?";
        String selPas = "SELECT 1 FROM " + pasajeroTable + " WHERE ID_USUARIO=?";
        String updPas = "UPDATE " + pasajeroTable +
                " SET FECHA_NACIMIENTO=?, ID_PAIS_DOCUMENTO=?, PASAPORTE=? WHERE ID_USUARIO=?";

        UsuarioAdminDTOs.UpdateSelf dto = mock(UsuarioAdminDTOs.UpdateSelf.class);
        when(dto.nombres()).thenReturn("Ana");
        when(dto.apellidos()).thenReturn("Lopez");
        when(dto.newPassword()).thenReturn("secret");
        when(dto.fechaNacimiento()).thenReturn(null);
        when(dto.idPais()).thenReturn(null);
        when(dto.pasaporte()).thenReturn("PX1");

        try (MockedStatic<DB> db = mockStatic(DB.class);
             MockedStatic<PasswordUtil> pw = mockStatic(PasswordUtil.class)) {

            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("USUARIO")).thenReturn(usuarioTable);
            db.when(() -> DB.table("PASAJERO")).thenReturn(pasajeroTable);

            when(cn.prepareStatement(upUser)).thenReturn(psUser);
            when(cn.prepareStatement(selPas)).thenReturn(psSelPas);
            when(cn.prepareStatement(updPas)).thenReturn(psUpdPas);

            when(psSelPas.executeQuery()).thenReturn(rsSel);
            when(rsSel.next()).thenReturn(true);

            pw.when(() -> PasswordUtil.hash("secret")).thenReturn("HASHED");

            UsuarioDAO dao = new UsuarioDAO();
            dao.selfUpdate(id, dto);

            verify(psUser).setString(3, "HASHED");

            verify(psUpdPas).setNull(1, Types.DATE);
            verify(psUpdPas).setNull(2, Types.NUMERIC);
            verify(psUpdPas).setString(3, "PX1");
            verify(psUpdPas).setLong(4, id);

            verify(cn).commit();
        }
    }

    @Test
    @DisplayName("obtenerVuelo devuelve view con clases y escalas")
    void obtenerVuelo_ok() throws Exception {
        long idVuelo = 100L;

        Connection cn1 = mock(Connection.class);
        Connection cn2 = mock(Connection.class);
        PreparedStatement ps1 = mock(PreparedStatement.class);
        PreparedStatement ps2 = mock(PreparedStatement.class);
        ResultSet rs1 = mock(ResultSet.class);
        ResultSet rs2 = mock(ResultSet.class);

        String vueloTable = "VUELO";
        String salidaClaseTable = "SALIDA_CLASE";
        String vueloEscalaTable = "VUELO_ESCALA";
        String ciudadTable = "CIUDAD";
        String paisTable = "PAIS";

        String sql1 = "SELECT v.ID_VUELO, v.CODIGO, v.ID_RUTA, v.FECHA_SALIDA, v.FECHA_LLEGADA, v.ACTIVO, " +
                "sc.ID_CLASE, sc.CUPO_TOTAL, sc.PRECIO FROM " + vueloTable +
                " v LEFT JOIN " + salidaClaseTable + " sc ON v.ID_VUELO = sc.ID_VUELO WHERE v.ID_VUELO = ?";

        String sqlEsc = "SELECT ve.ID_CIUDAD, c.NOMBRE AS CIUDAD, p.NOMBRE AS PAIS, ve.LLEGADA, ve.SALIDA FROM " +
                vueloEscalaTable + " ve JOIN " + ciudadTable + " c ON c.ID_CIUDAD = ve.ID_CIUDAD JOIN " +
                paisTable + " p ON p.ID_PAIS = c.ID_PAIS WHERE ve.ID_VUELO = ?";

        LocalDateTime salida = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime llegada = LocalDateTime.of(2025, 1, 1, 12, 0);
        LocalDateTime escLlegada = LocalDateTime.of(2025, 1, 1, 11, 0);
        LocalDateTime escSalida = LocalDateTime.of(2025, 1, 1, 11, 30);

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(() -> DB.table("VUELO")).thenReturn(vueloTable);
            db.when(() -> DB.table("SALIDA_CLASE")).thenReturn(salidaClaseTable);
            db.when(() -> DB.table("VUELO_ESCALA")).thenReturn(vueloEscalaTable);
            db.when(() -> DB.table("CIUDAD")).thenReturn(ciudadTable);
            db.when(() -> DB.table("PAIS")).thenReturn(paisTable);

            db.when(DB::getConnection).thenReturn(cn1, cn2);

            when(cn1.prepareStatement(sql1)).thenReturn(ps1);
            when(ps1.executeQuery()).thenReturn(rs1);

            when(rs1.next()).thenReturn(true, false);
            when(rs1.getLong("ID_VUELO")).thenReturn(idVuelo);
            when(rs1.getString("CODIGO")).thenReturn("V01");
            when(rs1.getLong("ID_RUTA")).thenReturn(5L);
            when(rs1.getTimestamp("FECHA_SALIDA")).thenReturn(Timestamp.valueOf(salida));
            when(rs1.getTimestamp("FECHA_LLEGADA")).thenReturn(Timestamp.valueOf(llegada));
            when(rs1.getInt("ACTIVO")).thenReturn(1);
            when(rs1.getInt("ID_CLASE")).thenReturn(1);
            when(rs1.getInt("CUPO_TOTAL")).thenReturn(100);
            when(rs1.getDouble("PRECIO")).thenReturn(500.0);

            when(cn2.prepareStatement(sqlEsc)).thenReturn(ps2);
            when(ps2.executeQuery()).thenReturn(rs2);

            when(rs2.next()).thenReturn(true, false);
            when(rs2.getLong("ID_CIUDAD")).thenReturn(10L);
            when(rs2.getString("CIUDAD")).thenReturn("Ciudad");
            when(rs2.getString("PAIS")).thenReturn("Pais");
            when(rs2.getTimestamp("LLEGADA")).thenReturn(Timestamp.valueOf(escLlegada));
            when(rs2.getTimestamp("SALIDA")).thenReturn(Timestamp.valueOf(escSalida));

            UsuarioDAO dao = new UsuarioDAO();
            VueloDTO.View v = dao.obtenerVuelo(idVuelo);

            assertNotNull(v);
            assertEquals(idVuelo, v.idVuelo());
            assertEquals("V01", v.codigo());
            assertEquals(5L, v.idRuta());
            assertEquals(salida, v.fechaSalida());
            assertEquals(llegada, v.fechaLlegada());
            assertTrue(v.activo());
            assertEquals(1, v.clases().size());
            assertEquals(1, v.escalas().size());
        }
    }

    @Test
    @DisplayName("obtenerVuelo devuelve null cuando no existe")
    void obtenerVuelo_notFound() throws Exception {
        long idVuelo = 200L;

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        String vueloTable = "VUELO";
        String salidaClaseTable = "SALIDA_CLASE";

        String sql1 = "SELECT v.ID_VUELO, v.CODIGO, v.ID_RUTA, v.FECHA_SALIDA, v.FECHA_LLEGADA, v.ACTIVO, " +
                "sc.ID_CLASE, sc.CUPO_TOTAL, sc.PRECIO FROM " + vueloTable +
                " v LEFT JOIN " + salidaClaseTable + " sc ON v.ID_VUELO = sc.ID_VUELO WHERE v.ID_VUELO = ?";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(() -> DB.table("VUELO")).thenReturn(vueloTable);
            db.when(() -> DB.table("SALIDA_CLASE")).thenReturn(salidaClaseTable);
            db.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(sql1)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            UsuarioDAO dao = new UsuarioDAO();
            VueloDTO.View v = dao.obtenerVuelo(idVuelo);

            assertNull(v);
        }
    }

    @Test
    @DisplayName("actualizarVueloAdmin actualiza vuelo, clases y escala correctamente")
    void actualizarVueloAdmin_ok() throws Exception {
        long idVuelo = 300L;

        Connection cn = mock(Connection.class);
        PreparedStatement psUpdate = mock(PreparedStatement.class);
        PreparedStatement psDelClase = mock(PreparedStatement.class);
        PreparedStatement psInsClase = mock(PreparedStatement.class);
        PreparedStatement psDelEsc = mock(PreparedStatement.class);
        PreparedStatement psInsEsc = mock(PreparedStatement.class);

        String vueloTable = "VUELO";
        String salidaClaseTable = "SALIDA_CLASE";
        String vueloEscalaTable = "VUELO_ESCALA";

        String up = "UPDATE " + vueloTable + " SET CODIGO=?, ID_RUTA=?, FECHA_SALIDA=?, FECHA_LLEGADA=?, ACTIVO=? WHERE ID_VUELO=?";
        String delClases = "DELETE FROM " + salidaClaseTable + " WHERE ID_VUELO=?";
        String insClase = "INSERT INTO " + salidaClaseTable + " (ID_VUELO, ID_CLASE, CUPO_TOTAL, PRECIO) VALUES (?,?,?,?)";
        String delEsc = "DELETE FROM " + vueloEscalaTable + " WHERE ID_VUELO=?";
        String insEsc = "INSERT INTO " + vueloEscalaTable + " (ID_VUELO, ID_CIUDAD, LLEGADA, SALIDA) VALUES (?,?,?,?)";

        LocalDateTime salida = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime llegada = LocalDateTime.of(2025, 1, 1, 12, 0);

        VueloDTO.UpdateAdmin dto = mock(VueloDTO.UpdateAdmin.class);
        when(dto.codigo()).thenReturn(" V01 ");
        when(dto.idRuta()).thenReturn(5L);
        when(dto.fechaSalida()).thenReturn(salida);
        when(dto.fechaLlegada()).thenReturn(llegada);
        when(dto.activo()).thenReturn(true);

        List<VueloDTO.ClaseConfig> clases = List.of(new VueloDTO.ClaseConfig(1, 100, 500.0));
        when(dto.clases()).thenReturn(clases);

        LocalDateTime escLlegada = LocalDateTime.of(2025, 1, 1, 11, 0);
        LocalDateTime escSalida = LocalDateTime.of(2025, 1, 1, 11, 30);
        List<VueloDTO.EscalaCreate> escalas = List.of(
                new VueloDTO.EscalaCreate(10L, escLlegada, escSalida)
        );
        when(dto.escalas()).thenReturn(escalas);

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(() -> DB.table("VUELO")).thenReturn(vueloTable);
            db.when(() -> DB.table("SALIDA_CLASE")).thenReturn(salidaClaseTable);
            db.when(() -> DB.table("VUELO_ESCALA")).thenReturn(vueloEscalaTable);
            db.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(up)).thenReturn(psUpdate);
            when(cn.prepareStatement(delClases)).thenReturn(psDelClase);
            when(cn.prepareStatement(insClase)).thenReturn(psInsClase);
            when(cn.prepareStatement(delEsc)).thenReturn(psDelEsc);
            when(cn.prepareStatement(insEsc)).thenReturn(psInsEsc);

            when(psUpdate.executeUpdate()).thenReturn(1);
            when(psDelClase.executeUpdate()).thenReturn(1);
            when(psInsClase.executeUpdate()).thenReturn(1);
            when(psDelEsc.executeUpdate()).thenReturn(1);
            when(psInsEsc.executeUpdate()).thenReturn(1);

            UsuarioDAO dao = new UsuarioDAO();
            dao.actualizarVueloAdmin(idVuelo, dto);

            verify(cn).setAutoCommit(false);
            verify(psUpdate).setString(1, "V01");
            verify(cn).commit();
            verify(cn).setAutoCommit(true);
        }
    }

    @Test
    @DisplayName("actualizarVueloAdmin lanza SQLException si el vuelo no existe")
    void actualizarVueloAdmin_vueloNoExiste() throws Exception {
        long idVuelo = 999L;

        Connection cn = mock(Connection.class);
        PreparedStatement psUpdate = mock(PreparedStatement.class);

        String vueloTable = "VUELO";
        String salidaClaseTable = "SALIDA_CLASE";
        String vueloEscalaTable = "VUELO_ESCALA";

        String up = "UPDATE " + vueloTable +
                " SET CODIGO=?, ID_RUTA=?, FECHA_SALIDA=?, FECHA_LLEGADA=?, ACTIVO=? WHERE ID_VUELO=?";

        VueloDTO.UpdateAdmin dto = mock(VueloDTO.UpdateAdmin.class);
        when(dto.codigo()).thenReturn("X01");
        when(dto.idRuta()).thenReturn(1L);
        when(dto.fechaSalida()).thenReturn(LocalDateTime.now());
        when(dto.fechaLlegada()).thenReturn(LocalDateTime.now().plusHours(1));
        when(dto.activo()).thenReturn(true);
        when(dto.clases()).thenReturn(null);
        when(dto.escalas()).thenReturn(null);

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("VUELO")).thenReturn(vueloTable);
            db.when(() -> DB.table("SALIDA_CLASE")).thenReturn(salidaClaseTable);
            db.when(() -> DB.table("VUELO_ESCALA")).thenReturn(vueloEscalaTable);

            when(cn.prepareStatement(up)).thenReturn(psUpdate);
            when(psUpdate.executeUpdate()).thenReturn(0);

            UsuarioDAO dao = new UsuarioDAO();

            assertThrows(SQLException.class, () -> dao.actualizarVueloAdmin(idVuelo, dto));
            verify(cn).setAutoCommit(false);
            verify(cn).rollback();
            verify(cn).setAutoCommit(true);
        }
    }

    @Test
    @DisplayName("actualizarVueloAdmin lanza error si se envían más de 1 escala")
    void actualizarVueloAdmin_masDeUnaEscala() throws Exception {
        long idVuelo = 301L;

        Connection cn = mock(Connection.class);
        PreparedStatement psUpdate = mock(PreparedStatement.class);
        PreparedStatement psDelClase = mock(PreparedStatement.class);
        PreparedStatement psDelEsc = mock(PreparedStatement.class);

        String vueloTable = "VUELO";
        String salidaClaseTable = "SALIDA_CLASE";
        String vueloEscalaTable = "VUELO_ESCALA";

        String up = "UPDATE " + vueloTable +
                " SET CODIGO=?, ID_RUTA=?, FECHA_SALIDA=?, FECHA_LLEGADA=?, ACTIVO=? WHERE ID_VUELO=?";
        String delClases = "DELETE FROM " + salidaClaseTable + " WHERE ID_VUELO=?";
        String delEsc = "DELETE FROM " + vueloEscalaTable + " WHERE ID_VUELO=?";

        LocalDateTime salida = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime llegada = LocalDateTime.of(2025, 1, 1, 12, 0);

        VueloDTO.UpdateAdmin dto = mock(VueloDTO.UpdateAdmin.class);
        when(dto.codigo()).thenReturn("V02");
        when(dto.idRuta()).thenReturn(5L);
        when(dto.fechaSalida()).thenReturn(salida);
        when(dto.fechaLlegada()).thenReturn(llegada);
        when(dto.activo()).thenReturn(true);
        when(dto.clases()).thenReturn(null);

        LocalDateTime escLlegada = LocalDateTime.of(2025, 1, 1, 11, 0);
        LocalDateTime escSalida = LocalDateTime.of(2025, 1, 1, 11, 30);

        List<VueloDTO.EscalaCreate> escalas = List.of(
                new VueloDTO.EscalaCreate(10L, escLlegada, escSalida),
                new VueloDTO.EscalaCreate(11L, escLlegada, escSalida)
        );
        when(dto.escalas()).thenReturn(escalas);

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("VUELO")).thenReturn(vueloTable);
            db.when(() -> DB.table("SALIDA_CLASE")).thenReturn(salidaClaseTable);
            db.when(() -> DB.table("VUELO_ESCALA")).thenReturn(vueloEscalaTable);

            when(cn.prepareStatement(up)).thenReturn(psUpdate);
            when(cn.prepareStatement(delClases)).thenReturn(psDelClase);
            when(cn.prepareStatement(delEsc)).thenReturn(psDelEsc);

            when(psUpdate.executeUpdate()).thenReturn(1);
            when(psDelClase.executeUpdate()).thenReturn(1);
            when(psDelEsc.executeUpdate()).thenReturn(1);

            UsuarioDAO dao = new UsuarioDAO();

            assertThrows(SQLException.class, () -> dao.actualizarVueloAdmin(idVuelo, dto));
            verify(cn).rollback();
        }
    }

    @Test
    @DisplayName("actualizarVueloAdmin lanza error si la llegada de la escala es después de la salida")
    void actualizarVueloAdmin_llegadaDespuesDeSalida() throws Exception {
        long idVuelo = 302L;

        Connection cn = mock(Connection.class);
        PreparedStatement psUpdate = mock(PreparedStatement.class);
        PreparedStatement psDelClase = mock(PreparedStatement.class);
        PreparedStatement psDelEsc = mock(PreparedStatement.class);

        String vueloTable = "VUELO";
        String salidaClaseTable = "SALIDA_CLASE";
        String vueloEscalaTable = "VUELO_ESCALA";

        String up = "UPDATE " + vueloTable +
                " SET CODIGO=?, ID_RUTA=?, FECHA_SALIDA=?, FECHA_LLEGADA=?, ACTIVO=? WHERE ID_VUELO=?";
        String delClases = "DELETE FROM " + salidaClaseTable + " WHERE ID_VUELO=?";
        String delEsc = "DELETE FROM " + vueloEscalaTable + " WHERE ID_VUELO=?";

        LocalDateTime salida = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime llegada = LocalDateTime.of(2025, 1, 1, 12, 0);

        VueloDTO.UpdateAdmin dto = mock(VueloDTO.UpdateAdmin.class);
        when(dto.codigo()).thenReturn("V03");
        when(dto.idRuta()).thenReturn(5L);
        when(dto.fechaSalida()).thenReturn(salida);
        when(dto.fechaLlegada()).thenReturn(llegada);
        when(dto.activo()).thenReturn(true);
        when(dto.clases()).thenReturn(null);

        LocalDateTime escLlegada = LocalDateTime.of(2025, 1, 1, 11, 30);
        LocalDateTime escSalida = LocalDateTime.of(2025, 1, 1, 11, 0);
        List<VueloDTO.EscalaCreate> escalas = List.of(
                new VueloDTO.EscalaCreate(10L, escLlegada, escSalida)
        );
        when(dto.escalas()).thenReturn(escalas);

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("VUELO")).thenReturn(vueloTable);
            db.when(() -> DB.table("SALIDA_CLASE")).thenReturn(salidaClaseTable);
            db.when(() -> DB.table("VUELO_ESCALA")).thenReturn(vueloEscalaTable);

            when(cn.prepareStatement(up)).thenReturn(psUpdate);
            when(cn.prepareStatement(delClases)).thenReturn(psDelClase);
            when(cn.prepareStatement(delEsc)).thenReturn(psDelEsc);

            when(psUpdate.executeUpdate()).thenReturn(1);
            when(psDelClase.executeUpdate()).thenReturn(1);
            when(psDelEsc.executeUpdate()).thenReturn(1);

            UsuarioDAO dao = new UsuarioDAO();

            assertThrows(SQLException.class, () -> dao.actualizarVueloAdmin(idVuelo, dto));
            verify(cn).rollback();
        }
    }
}
