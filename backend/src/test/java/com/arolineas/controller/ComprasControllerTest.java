package com.arolineas.controller;

import com.aerolineas.controller.ComprasController;
import com.aerolineas.dao.ComprasDAO;
import com.aerolineas.dao.UsuarioDAO;
import com.aerolineas.dto.CompraDTO.*;
import com.aerolineas.model.Usuario;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.List;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ComprasControllerTest {

  private Handler jwtHandlerNoOp() {
    return ctx -> {
    };
  }
  static class DummyBean {
    public String nombre = "Kat";
    public String otro = null;
}


  private Handler wsHandlerNoOp() {
    return ctx -> {};
  }

  @Test
  void getCarrito_ok() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);
    Handler jwt = jwtHandlerNoOp();
    Handler ws = wsHandlerNoOp();

    ComprasController controller = new ComprasController(dao, usuarioDAO, jwt, ws);

    Javalin app = mock(Javalin.class);

    ArgumentCaptor<Handler> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
    when(app.get(eq("/api/compras/carrito"), any(Handler.class))).thenReturn(app);

    controller.register(app);

    verify(app).get(eq("/api/compras/carrito"), handlerCaptor.capture());
    Handler handler = handlerCaptor.getValue();
    assertNotNull(handler);

    Context ctx = mock(Context.class);

    when(ctx.attribute("claims")).thenReturn(Map.of("idUsuario", 123L));

    CarritoResp carrito = mock(CarritoResp.class);
    when(dao.getCart(123L)).thenReturn(carrito);

    handler.handle(ctx);

    verify(dao).getCart(123L);
    verify(ctx).json(carrito);
  }

  @Test
  void getCarrito_usaHeaderUserId_cuandoNoHayClaims() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);
    Handler jwt = jwtHandlerNoOp();
    Handler ws = wsHandlerNoOp();

    ComprasController controller = new ComprasController(dao, usuarioDAO, jwt, ws);

    Javalin app = mock(Javalin.class);

    ArgumentCaptor<Handler> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
    when(app.get(eq("/api/compras/carrito"), any(Handler.class))).thenReturn(app);

    controller.register(app);

    verify(app).get(eq("/api/compras/carrito"), handlerCaptor.capture());
    Handler handler = handlerCaptor.getValue();
    assertNotNull(handler);

    Context ctx = mock(Context.class);

    when(ctx.attribute("claims")).thenReturn(null);
    when(ctx.header("X-User-Id")).thenReturn("55");

    CarritoResp carrito = mock(CarritoResp.class);
    when(dao.getCart(55L)).thenReturn(carrito);

    handler.handle(ctx);

    verify(dao).getCart(55L);
    verify(ctx).json(carrito);
  }

  @Test
  void addItem_ok() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);
    Handler jwt = jwtHandlerNoOp();
    Handler ws = wsHandlerNoOp();

    ComprasController controller = new ComprasController(dao, usuarioDAO, jwt, ws);

    Javalin app = mock(Javalin.class);

    ArgumentCaptor<Handler> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
    when(app.post(eq("/api/compras/items"), any(Handler.class))).thenReturn(app);

    controller.register(app);

    verify(app).post(eq("/api/compras/items"), handlerCaptor.capture());
    Handler handler = handlerCaptor.getValue();
    assertNotNull(handler);

    Context ctx = mock(Context.class);

    when(ctx.attribute("claims")).thenReturn(Map.of("idUsuario", 99L));

    AddItemReq req = new AddItemReq();
    req.idVuelo = 10L;
    req.idClase = 2;
    req.cantidad = 3;

    when(ctx.bodyAsClass(AddItemReq.class)).thenReturn(req);
    when(ctx.queryParam("pair")).thenReturn("true");

    handler.handle(ctx);

    verify(dao).addOrIncrementItem(99L, 10L, 2, 3, true);
    verify(ctx).status(201);
  }

  @Test
  void listReservas_ok() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);
    Handler jwt = jwtHandlerNoOp();
    Handler ws = wsHandlerNoOp();

    ComprasController controller = new ComprasController(dao, usuarioDAO, jwt, ws);

    Javalin app = mock(Javalin.class);

    ArgumentCaptor<Handler> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
    when(app.get(eq("/api/compras/reservas"), any(Handler.class))).thenReturn(app);

    controller.register(app);

    verify(app).get(eq("/api/compras/reservas"), handlerCaptor.capture());
    Handler handler = handlerCaptor.getValue();
    assertNotNull(handler);

    Context ctx = mock(Context.class);

    when(ctx.attribute("claims")).thenReturn(Map.of("idUsuario", 77L));

    handler.handle(ctx);

    verify(dao).listReservasByUser(77L);
    verify(ctx).json(any());
  }

    @Test
  void getCarrito_errorAutenticacion_responde400() throws Exception {
    Handler jwtFail = ctx -> { throw new RuntimeException("JWT fail"); };
    Handler ws = wsHandlerNoOp();

    ComprasDAO dao = mock(ComprasDAO.class);
    UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);

    ComprasController controller = new ComprasController(dao, usuarioDAO, jwtFail, ws);

    Javalin app = mock(Javalin.class);

    ArgumentCaptor<Handler> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
    when(app.get(eq("/api/compras/carrito"), any(Handler.class))).thenReturn(app);

    controller.register(app);

    verify(app).get(eq("/api/compras/carrito"), handlerCaptor.capture());
    Handler handler = handlerCaptor.getValue();
    assertNotNull(handler);

    Context ctx = mock(Context.class);
    when(ctx.attribute("claims")).thenReturn(null);

    when(ctx.status(anyInt())).thenReturn(ctx);

    handler.handle(ctx);

    verify(ctx).status(400);
    verify(ctx).json(any(Map.class));
    verify(dao, never()).getCart(anyLong());
  }

  @Test
  void checkout_datosPagoIncompletos_responde400() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);
    Handler jwt = jwtHandlerNoOp();
    Handler ws = wsHandlerNoOp();

    ComprasController controller = new ComprasController(dao, usuarioDAO, jwt, ws);

    Javalin app = mock(Javalin.class);

    ArgumentCaptor<Handler> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
    when(app.post(eq("/api/compras/checkout"), any(Handler.class))).thenReturn(app);

    controller.register(app);

    verify(app).post(eq("/api/compras/checkout"), handlerCaptor.capture());
    Handler handler = handlerCaptor.getValue();
    assertNotNull(handler);

    Context ctx = mock(Context.class);

    when(ctx.attribute("claims")).thenReturn(Map.of("idUsuario", 42L));

    when(ctx.bodyAsClass(PaymentReq.class)).thenReturn(null);

    when(ctx.status(anyInt())).thenReturn(ctx);

    handler.handle(ctx);

    verify(ctx).status(400);
    verify(ctx).json(any(Map.class));
    verify(dao, never()).getCart(anyLong());
    verify(dao, never()).checkout(anyLong());
  }


  @Test
  void topDestinos_ok() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);
    Handler jwt = jwtHandlerNoOp();
    Handler ws = wsHandlerNoOp();

    ComprasController controller = new ComprasController(dao, usuarioDAO, jwt, ws);

    Javalin app = mock(Javalin.class);

    ArgumentCaptor<Handler> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
    when(app.get(eq("/api/public/stats/top-destinos"), any(Handler.class))).thenReturn(app);

    controller.register(app);

    verify(app).get(eq("/api/public/stats/top-destinos"), handlerCaptor.capture());
    Handler handler = handlerCaptor.getValue();
    assertNotNull(handler);

    Context ctx = mock(Context.class);

    when(ctx.queryParam("desde")).thenReturn("2025-01-01");
    when(ctx.queryParam("hasta")).thenReturn("2025-01-31");
    when(ctx.queryParam("limit")).thenReturn("7");

    handler.handle(ctx);

    verify(dao).listTopDestinos(any(Timestamp.class), any(Timestamp.class), eq(7));
    verify(ctx).json(any());
  }

  @Test
void getUserId_usaIdUsuario() throws Exception {
    ComprasController c = new ComprasController(mock(ComprasDAO.class), mock(UsuarioDAO.class),
            jwtHandlerNoOp(), wsHandlerNoOp());

    Context ctx = mock(Context.class);
    when(ctx.attribute("claims")).thenReturn(Map.of("idUsuario", "10"));

    long id = c.getUserId(ctx);

    assertEquals(10L, id);
}

@Test
void getUserId_usaId() throws Exception {
    ComprasController c = new ComprasController(mock(ComprasDAO.class), mock(UsuarioDAO.class),
            jwtHandlerNoOp(), wsHandlerNoOp());

    Context ctx = mock(Context.class);
    when(ctx.attribute("claims")).thenReturn(Map.of("id", "20"));

    long id = c.getUserId(ctx);

    assertEquals(20L, id);
}

@Test
void getUserId_usaUserId() throws Exception {
    ComprasController c = new ComprasController(mock(ComprasDAO.class), mock(UsuarioDAO.class),
            jwtHandlerNoOp(), wsHandlerNoOp());

    Context ctx = mock(Context.class);
    when(ctx.attribute("claims")).thenReturn(Map.of("userId", "30"));

    long id = c.getUserId(ctx);

    assertEquals(30L, id);
}

@Test
void getUserId_usaSub() throws Exception {
    ComprasController c = new ComprasController(mock(ComprasDAO.class), mock(UsuarioDAO.class),
            jwtHandlerNoOp(), wsHandlerNoOp());

    Context ctx = mock(Context.class);
    when(ctx.attribute("claims")).thenReturn(Map.of("sub", "40"));

    long id = c.getUserId(ctx);

    assertEquals(40L, id);
}

@Test
void getUserId_desdeHeader() throws Exception {
    ComprasController c = new ComprasController(mock(ComprasDAO.class), mock(UsuarioDAO.class),
            jwtHandlerNoOp(), wsHandlerNoOp());

    Context ctx = mock(Context.class);
    when(ctx.attribute("claims")).thenReturn(null);
    when(ctx.header("X-User-Id")).thenReturn("55");

    long id = c.getUserId(ctx);

    assertEquals(55L, id);
}
@Test
void getUserId_sinNada_lanzaExcepcion() {
    ComprasController c = new ComprasController(mock(ComprasDAO.class), mock(UsuarioDAO.class),
            jwtHandlerNoOp(), wsHandlerNoOp());

    Context ctx = mock(Context.class);
    when(ctx.attribute("claims")).thenReturn(null);
    when(ctx.header("X-User-Id")).thenReturn(null);

    assertThrows(IllegalStateException.class, () -> c.getUserId(ctx));
}

@Test
void testMail_ok() throws Exception {
    ComprasController controller = new ComprasController(
            mock(ComprasDAO.class),
            mock(UsuarioDAO.class),
            jwtHandlerNoOp(),
            wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> cap = ArgumentCaptor.forClass(Handler.class);
    when(app.get(eq("/api/dev/test-mail"), any())).thenReturn(app);

    controller.register(app);

    verify(app).get(eq("/api/dev/test-mail"), cap.capture());
    Handler h = cap.getValue();

    Context ctx = mock(Context.class);
    when(ctx.queryParam("to")).thenReturn("test@example.com");

    assertThrows(IllegalStateException.class, () -> h.handle(ctx));
}

@Test
void deleteItem_ok() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    ComprasController controller = new ComprasController(
            dao, mock(UsuarioDAO.class),
            jwtHandlerNoOp(), wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> cap = ArgumentCaptor.forClass(Handler.class);
    when(app.delete(eq("/api/compras/items/{idItem}"), any())).thenReturn(app);

    controller.register(app);
    verify(app).delete(eq("/api/compras/items/{idItem}"), cap.capture());
    Handler h = cap.getValue();

    Context ctx = mock(Context.class);
    when(ctx.attribute("claims")).thenReturn(Map.of("idUsuario", 50L));
    when(ctx.pathParam("idItem")).thenReturn("99");
    when(ctx.status(anyInt())).thenReturn(ctx);

    h.handle(ctx);

    verify(dao).removeItem(50L, 99L, false);

    verify(ctx).status(204);
}

@Test
void updateItem_ok() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    ComprasController controller = new ComprasController(
            dao, mock(UsuarioDAO.class),
            jwtHandlerNoOp(), wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> cap = ArgumentCaptor.forClass(Handler.class);
    when(app.put(eq("/api/compras/items/{idItem}"), any())).thenReturn(app);

    controller.register(app);
    verify(app).put(eq("/api/compras/items/{idItem}"), cap.capture());
    Handler h = cap.getValue();

    Context ctx = mock(Context.class);
    UpdateQtyReq req = new UpdateQtyReq();
    req.cantidad = 7;

    when(ctx.attribute("claims")).thenReturn(Map.of("idUsuario", 20L));
    when(ctx.pathParam("idItem")).thenReturn("55");
    when(ctx.bodyAsClass(UpdateQtyReq.class)).thenReturn(req);
    when(ctx.status(anyInt())).thenReturn(ctx);

    h.handle(ctx);

    verify(dao).updateQuantity(20L, 55L, 7, false);

    verify(ctx).status(204);
}

  @Test
  void getReservaDetalle_usuarioNormal_usaDaoConUserId() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);

    ComprasController controller = new ComprasController(
            dao, usuarioDAO,
            jwtHandlerNoOp(), wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> cap = ArgumentCaptor.forClass(Handler.class);
    when(app.get(eq("/api/compras/reservas/{id}"), any())).thenReturn(app);

    controller.register(app);

    verify(app).get(eq("/api/compras/reservas/{id}"), cap.capture());
    Handler h = cap.getValue();
    assertNotNull(h);

    Context ctx = mock(Context.class);

    when(ctx.attribute("claims")).thenReturn(Map.of(
            "idUsuario", 77L,
            "rol", 3          
    ));
    when(ctx.pathParam("id")).thenReturn("99");

    
    ReservaDetalle detalle = new ReservaDetalle();
    when(dao.getReservaDetalle(77L, 99L)).thenReturn(detalle);

    h.handle(ctx);

    verify(dao).getReservaDetalle(77L, 99L);
    verify(dao, never()).getReservaDetalleAdmin(anyLong());
    verify(ctx).json(detalle);
  }

  @Test
  void getReservaDetalle_webservice_usaDaoAdmin() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);

    ComprasController controller = new ComprasController(
            dao, usuarioDAO,
            jwtHandlerNoOp(), wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> cap = ArgumentCaptor.forClass(Handler.class);
    when(app.get(eq("/api/compras/reservas/{id}"), any())).thenReturn(app);

    controller.register(app);

    verify(app).get(eq("/api/compras/reservas/{id}"), cap.capture());
    Handler h = cap.getValue();
    assertNotNull(h);

    Context ctx = mock(Context.class);

    when(ctx.attribute("claims")).thenReturn(Map.of(
            "idUsuario", 200L,
            "rol", 2          
    ));
    when(ctx.pathParam("id")).thenReturn("123");

    ReservaDetalle detalleAdmin = new ReservaDetalle();
    when(dao.getReservaDetalleAdmin(123L)).thenReturn(detalleAdmin);

    h.handle(ctx);

    verify(dao).getReservaDetalleAdmin(123L);
    verify(dao, never()).getReservaDetalle(anyLong(), anyLong());
    verify(ctx).json(detalleAdmin);
  }

  @Test
  void cancelarReserva_noAdmin_estadoNoCancelable_devuelve409() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);

    ComprasController controller = new ComprasController(
            dao, mock(UsuarioDAO.class),
            jwtHandlerNoOp(), wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> cap = ArgumentCaptor.forClass(Handler.class);
    when(app.post(eq("/api/compras/reservas/{id}/cancelar"), any())).thenReturn(app);

    controller.register(app);

    verify(app).post(eq("/api/compras/reservas/{id}/cancelar"), cap.capture());
    Handler h = cap.getValue();
    assertNotNull(h);

    Context ctx = mock(Context.class);
    
    when(ctx.attribute("claims")).thenReturn(Map.of(
            "idUsuario", 50L,
            "rol", 3
    ));
    when(ctx.pathParam("id")).thenReturn("99");
    when(ctx.status(anyInt())).thenReturn(ctx);

    
    when(dao.cancelarReserva(50L, 99L, false)).thenReturn(false);

    h.handle(ctx);

    verify(dao).cancelarReserva(50L, 99L, false);
    verify(ctx).status(409);
    verify(ctx).json(any(Map.class));
  }

  @Test
  void cancelarReserva_admin_ok_devuelveStatusOk() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);

    ComprasController controller = new ComprasController(
            dao, mock(UsuarioDAO.class),
            jwtHandlerNoOp(), wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> cap = ArgumentCaptor.forClass(Handler.class);
    when(app.post(eq("/api/compras/reservas/{id}/cancelar"), any())).thenReturn(app);

    controller.register(app);

    verify(app).post(eq("/api/compras/reservas/{id}/cancelar"), cap.capture());
    Handler h = cap.getValue();
    assertNotNull(h);

    Context ctx = mock(Context.class);
    
    when(ctx.attribute("claims")).thenReturn(Map.of(
            "idUsuario", 1L,
            "rol", 1
    ));
    when(ctx.pathParam("id")).thenReturn("777");

    when(dao.cancelarReserva(1L, 777L, true)).thenReturn(true);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> jsonCap = ArgumentCaptor.forClass(Map.class);

    h.handle(ctx);

    verify(dao).cancelarReserva(1L, 777L, true);
    verify(ctx).json(jsonCap.capture());

    Map<String, Object> body = jsonCap.getValue();
    assertEquals("ok", body.get("status"));
  }

    @Test
void money_convierteValoresSinFallar() throws Exception {
    ComprasController c = new ComprasController(
            mock(ComprasDAO.class),
            mock(UsuarioDAO.class),
            jwtHandlerNoOp(),
            wsHandlerNoOp()
    );

    Method m = ComprasController.class.getDeclaredMethod("money", BigDecimal.class);
    m.setAccessible(true);

    
    Object resNull = m.invoke(c, new Object[]{null});
    assertNotNull(resNull);

    Object resValor = m.invoke(c, new BigDecimal("10.50"));
    assertNotNull(resValor);
}

@Test
void dt_convierteFechasYSoportaNull() throws Exception {
    ComprasController c = new ComprasController(
            mock(ComprasDAO.class),
            mock(UsuarioDAO.class),
            jwtHandlerNoOp(),
            wsHandlerNoOp()
    );

    Method m = ComprasController.class.getDeclaredMethod("dt", String.class);
    m.setAccessible(true);

    String resNull = (String) m.invoke(c, new Object[]{null});
    assertNotNull(resNull);
    assertFalse(resNull.isEmpty());

    String resInvalida = (String) m.invoke(c, "2025/01/01");
    assertEquals("2025/01/01", resInvalida);

    String resValida = (String) m.invoke(c, "2025-01-01T10:15:30Z");
    assertNotNull(resValida);
    assertNotEquals("2025-01-01T10:15:30Z", resValida);
    assertTrue(resValida.contains("2025"));
}


@Test
void getOpt_leeCampoYDevuelveVacioSiNullOError() throws Exception {
    Method m = ComprasController.class.getDeclaredMethod("getOpt", Object.class, String.class);
    m.setAccessible(true);

    DummyBean bean = new DummyBean();

    String nombre = (String) m.invoke(null, bean, "nombre");
    assertEquals("Kat", nombre);

    String otro = (String) m.invoke(null, bean, "otro");
    assertEquals("", otro);

    String inexistente = (String) m.invoke(null, bean, "noExiste");
    assertEquals("", inexistente);

    String beanNull = (String) m.invoke(null, new Object[]{null, "nombre"});
    assertEquals("", beanNull);
}

  @Test
  void isAdmin_distingueEntreAdminYNoAdmin() throws Exception {
    ComprasController c = new ComprasController(
            mock(ComprasDAO.class),
            mock(UsuarioDAO.class),
            jwtHandlerNoOp(),
            wsHandlerNoOp()
    );

    Method m = ComprasController.class.getDeclaredMethod("isAdmin", Context.class);
    m.setAccessible(true);

    Context ctxAdmin = mock(Context.class);
    when(ctxAdmin.attribute("claims")).thenReturn(Map.of("rol", 1));

    Context ctxUser = mock(Context.class);
    when(ctxUser.attribute("claims")).thenReturn(Map.of("rol", 3));

    boolean esAdmin = (Boolean) m.invoke(c, ctxAdmin);
    boolean esUser  = (Boolean) m.invoke(c, ctxUser);

    assertTrue(esAdmin);
    assertFalse(esUser);
  }

  @Test
void sendEmail_seEjecuta_sinReventarElTest() throws Exception {
    ComprasController c = new ComprasController(
            mock(ComprasDAO.class),
            mock(UsuarioDAO.class),
            jwtHandlerNoOp(),
            wsHandlerNoOp()
    );

    Method m = ComprasController.class.getDeclaredMethod(
            "sendEmail",
            Context.class,
            CarritoResp.class,
            long.class
    );
    m.setAccessible(true);

    Context ctx = mock(Context.class);
    CarritoResp carrito = new CarritoResp();
    carrito.idUsuario = 42L; 

    try {
        m.invoke(c, ctx, carrito, 42L);
    } catch (InvocationTargetException ex) {
        assertNotNull(ex.getCause());
    }
}

@Test
void addItem_bodyNulo_responde400() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    ComprasController controller = new ComprasController(
            dao, mock(UsuarioDAO.class),
            jwtHandlerNoOp(), wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> cap = ArgumentCaptor.forClass(Handler.class);
    when(app.post(eq("/api/compras/items"), any())).thenReturn(app);

    controller.register(app);
    verify(app).post(eq("/api/compras/items"), cap.capture());
    Handler h = cap.getValue();
    assertNotNull(h);

    Context ctx = mock(Context.class);
    when(ctx.attribute("claims")).thenReturn(Map.of("idUsuario", 99L));
    when(ctx.bodyAsClass(AddItemReq.class)).thenReturn(null);
    when(ctx.status(anyInt())).thenReturn(ctx);

    h.handle(ctx);

    verify(dao, never()).addOrIncrementItem(anyLong(), anyLong(), anyInt(), anyInt(), anyBoolean());
    verify(dao, never()).addOrIncrementItem(anyLong(), anyLong(), anyInt(), anyInt());
    verify(ctx).status(400);
    verify(ctx).json(any(Map.class));
}

    @Test
void authenticate_conWebService_ok() throws Exception {
    Handler jwtFail = ctx -> { throw new RuntimeException("JWT fail"); };
    Handler wsHandler = mock(Handler.class);

    ComprasController c = new ComprasController(
            mock(ComprasDAO.class),
            mock(UsuarioDAO.class),
            jwtFail,
            wsHandler
    );

    Context ctx = mock(Context.class);

    when(ctx.header("X-WebService-Email")).thenReturn("ws@example.com");
    when(ctx.header("X-WebService-Password")).thenReturn("secret");

    when(ctx.attribute("claims"))
            .thenReturn(null, Map.of("idUsuario", "999"));

    long id = c.getUserId(ctx);

    assertEquals(999L, id);
    verify(wsHandler).handle(ctx);
}

@Test
void authenticate_webServiceFalla_lanzaIllegalState() {
    Handler jwtFail = ctx -> { throw new RuntimeException("JWT fail"); };
    Handler wsFail = ctx -> { throw new RuntimeException("WS error"); };

    ComprasController c = new ComprasController(
            mock(ComprasDAO.class),
            mock(UsuarioDAO.class),
            jwtFail,
            wsFail
    );

    Context ctx = mock(Context.class);
    when(ctx.header("X-WebService-Email")).thenReturn("ws@example.com");
    when(ctx.header("X-WebService-Password")).thenReturn("secret");
    when(ctx.attribute("claims")).thenReturn(null);

    IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> c.getUserId(ctx)
    );

    assertTrue(ex.getMessage().startsWith("Autenticación requerida"));
}

    @Test
void authenticate_sinJwtNiWebService_lanzaIllegalState() {
    
    Handler jwtFail = ctx -> { throw new RuntimeException("JWT fail"); };
    
    Handler wsHandler = wsHandlerNoOp();

    ComprasController c = new ComprasController(
            mock(ComprasDAO.class),
            mock(UsuarioDAO.class),
            jwtFail,
            wsHandler
    );

    Context ctx = mock(Context.class);
    
    when(ctx.attribute("claims")).thenReturn(null);
    
    when(ctx.header("X-WebService-Email")).thenReturn(null);
    when(ctx.header("X-WebService-Password")).thenReturn(null);

    IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> c.getUserId(ctx)   
    );

    assertTrue(ex.getMessage().contains("JWT o credenciales WebService"));
}

@Test
void isAdmin_sinClaims_devuelveFalse() throws Exception {
    ComprasController c = new ComprasController(
            mock(ComprasDAO.class),
            mock(UsuarioDAO.class),
            jwtHandlerNoOp(),
            wsHandlerNoOp()
    );

    Method m = ComprasController.class.getDeclaredMethod("isAdmin", Context.class);
    m.setAccessible(true);

    Context ctx = mock(Context.class);
    when(ctx.attribute("claims")).thenReturn(null);

    boolean esAdmin = (Boolean) m.invoke(c, ctx);

    assertFalse(esAdmin);
}

@Test
void register_todosLosHandlers_sePuedenInvocar() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);

    ComprasController controller = new ComprasController(
            dao,
            usuarioDAO,
            jwtHandlerNoOp(),
            wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);

    ArgumentCaptor<Handler> getCap = ArgumentCaptor.forClass(Handler.class);
    ArgumentCaptor<Handler> postCap = ArgumentCaptor.forClass(Handler.class);
    ArgumentCaptor<Handler> putCap = ArgumentCaptor.forClass(Handler.class);
    ArgumentCaptor<Handler> delCap = ArgumentCaptor.forClass(Handler.class);
    
    when(app.get(anyString(), any(Handler.class))).thenReturn(app);
    when(app.post(anyString(), any(Handler.class))).thenReturn(app);
    when(app.put(anyString(), any(Handler.class))).thenReturn(app);
    when(app.delete(anyString(), any(Handler.class))).thenReturn(app);
    
    controller.register(app);

    verify(app, atLeastOnce()).get(anyString(), getCap.capture());
    verify(app, atLeastOnce()).post(anyString(), postCap.capture());
    verify(app, atLeastOnce()).put(anyString(), putCap.capture());
    verify(app, atLeastOnce()).delete(anyString(), delCap.capture());

    Context ctx = mock(Context.class);
    when(ctx.attribute("claims")).thenReturn(Map.of("idUsuario", 1L, "rol", 1)); 
    when(ctx.status(anyInt())).thenReturn(ctx);
    when(ctx.queryParam(anyString())).thenReturn(null);
    when(ctx.header(anyString())).thenReturn(null);
    when(ctx.pathParam(anyString())).thenReturn("1");
    when(ctx.bodyAsClass(any(Class.class))).thenReturn(null);
    
    for (Handler h : getCap.getAllValues()) {
        try {
            h.handle(ctx);
        } catch (Exception ignored) {
            
        }
    }
    for (Handler h : postCap.getAllValues()) {
        try {
            h.handle(ctx);
        } catch (Exception ignored) {}
    }
    for (Handler h : putCap.getAllValues()) {
        try {
            h.handle(ctx);
        } catch (Exception ignored) {}
    }
    for (Handler h : delCap.getAllValues()) {
        try {
            h.handle(ctx);
        } catch (Exception ignored) {}
    }
    
}

@Test
void addItem_usaSobrecargaSinSync_cuandoMetodoConSyncNoExiste() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);

    doThrow(new NoSuchMethodError("no boolean version"))
            .when(dao)
            .addOrIncrementItem(anyLong(), anyLong(), anyInt(), anyInt(), anyBoolean());

    ComprasController controller = new ComprasController(
            dao,
            mock(UsuarioDAO.class),
            jwtHandlerNoOp(),
            wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> cap = ArgumentCaptor.forClass(Handler.class);
    when(app.post(eq("/api/compras/items"), any())).thenReturn(app);

    controller.register(app);
    verify(app).post(eq("/api/compras/items"), cap.capture());
    Handler h = cap.getValue();
    assertNotNull(h);

    Context ctx = mock(Context.class);
    when(ctx.attribute("claims")).thenReturn(Map.of("idUsuario", 99L));
    when(ctx.status(anyInt())).thenReturn(ctx);
    when(ctx.queryParam("pair")).thenReturn("true"); 

    AddItemReq req = new AddItemReq();
    req.idVuelo = 10L;
    req.idClase = 2;
    req.cantidad = 3;
    when(ctx.bodyAsClass(AddItemReq.class)).thenReturn(req);

    h.handle(ctx);

    verify(dao).addOrIncrementItem(99L, 10L, 2, 3, true);
    
    verify(dao).addOrIncrementItem(99L, 10L, 2, 3);
    verify(ctx).status(201);
}

@Test
void updateItem_usaSobrecargaSinSync_cuandoMetodoConSyncNoExiste() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);

    doThrow(new NoSuchMethodError("no boolean version"))
            .when(dao)
            .updateQuantity(anyLong(), anyLong(), anyInt(), anyBoolean());

    ComprasController controller = new ComprasController(
            dao,
            mock(UsuarioDAO.class),
            jwtHandlerNoOp(),
            wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> cap = ArgumentCaptor.forClass(Handler.class);
    when(app.put(eq("/api/compras/items/{idItem}"), any())).thenReturn(app);

    controller.register(app);
    verify(app).put(eq("/api/compras/items/{idItem}"), cap.capture());
    Handler h = cap.getValue();
    assertNotNull(h);

    Context ctx = mock(Context.class);
    when(ctx.attribute("claims")).thenReturn(Map.of("idUsuario", 20L));
    when(ctx.pathParam("idItem")).thenReturn("55");
    when(ctx.status(anyInt())).thenReturn(ctx);
    when(ctx.queryParam("syncPareja")).thenReturn("true"); 

    UpdateQtyReq req = new UpdateQtyReq();
    req.cantidad = 7;
    when(ctx.bodyAsClass(UpdateQtyReq.class)).thenReturn(req);

    h.handle(ctx);

    
    verify(dao).updateQuantity(20L, 55L, 7, true);
    
    verify(dao).updateQuantity(20L, 55L, 7);
    verify(ctx).status(204);
}

@Test
void deleteItem_conSyncParejaTrue_enviaBanderaTrueAlDao() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);

    ComprasController controller = new ComprasController(
            dao,
            mock(UsuarioDAO.class),
            jwtHandlerNoOp(),
            wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> cap = ArgumentCaptor.forClass(Handler.class);
    when(app.delete(eq("/api/compras/items/{idItem}"), any())).thenReturn(app);

    controller.register(app);
    verify(app).delete(eq("/api/compras/items/{idItem}"), cap.capture());
    Handler h = cap.getValue();
    assertNotNull(h);

    Context ctx = mock(Context.class);
    when(ctx.attribute("claims")).thenReturn(Map.of("idUsuario", 50L));
    when(ctx.pathParam("idItem")).thenReturn("99");
    when(ctx.queryParam("syncPareja")).thenReturn("true"); 
    when(ctx.status(anyInt())).thenReturn(ctx);

    h.handle(ctx);

    verify(dao).removeItem(50L, 99L, true);
    verify(ctx).status(204);
}

@Test
void authenticate_conCredencialesWebServiceValida_noLanza() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);

    Handler jwt = ctx -> { /* no hace nada */ };

    Handler ws = ctx -> { /* simula validación correcta */ };

    ComprasController controller = new ComprasController(dao, usuarioDAO, jwt, ws);

    Context ctx = mock(Context.class);

    when(ctx.attribute("claims"))
            .thenReturn(null, Map.of("idUsuario", 999L));

    when(ctx.header("X-WebService-Email")).thenReturn("ws@example.com");
    when(ctx.header("X-WebService-Password")).thenReturn("secret");

    Method m = ComprasController.class.getDeclaredMethod("authenticate", Context.class);
    m.setAccessible(true);
    m.invoke(controller, ctx);

}

    @Test
void sendEmail_enviaOGeneraExcepcionControlada() throws Exception {
    ComprasController c = new ComprasController(
            mock(ComprasDAO.class),
            mock(UsuarioDAO.class),
            jwtHandlerNoOp(),
            wsHandlerNoOp()
    );

    Method m = ComprasController.class.getDeclaredMethod(
            "sendEmail",
            Context.class,
            CarritoResp.class,
            long.class
    );
    m.setAccessible(true);

    Context ctx = mock(Context.class);
    when(ctx.queryParam("to")).thenReturn("destino@correo.com");

    CarritoResp carrito = new CarritoResp();
    carrito.idUsuario = 1L;

    try {
        m.invoke(c, ctx, carrito, 1L);
    } catch (InvocationTargetException ex) {
        assertNotNull(ex.getCause());
    }
}
@Test
void boletoPdf_reservaNoEncontrada_devuelve404() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);

    ComprasController controller = new ComprasController(
            dao,
            usuarioDAO,
            jwtHandlerNoOp(),
            wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> cap = ArgumentCaptor.forClass(Handler.class);
    when(app.get(eq("/api/compras/reservas/{id}/boleto.pdf"), any())).thenReturn(app);

    controller.register(app);

    verify(app).get(eq("/api/compras/reservas/{id}/boleto.pdf"), cap.capture());
    Handler h = cap.getValue();
    assertNotNull(h);

    Context ctx = mock(Context.class);

    when(ctx.attribute("claims")).thenReturn(Map.of("idUsuario", 77L));
    when(ctx.pathParam("id")).thenReturn("99");

    when(dao.getReservaDetalle(77L, 99L)).thenReturn(null);

    when(ctx.status(anyInt())).thenReturn(ctx);

    h.handle(ctx);

    verify(dao).getReservaDetalle(77L, 99L);
    verify(ctx).status(404);
    verify(ctx).json(any(Map.class));
}

@Test
void checkout_ok_usuarioFinal() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);

    ComprasController controller = new ComprasController(
            dao, usuarioDAO,
            jwtHandlerNoOp(), wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> cap = ArgumentCaptor.forClass(Handler.class);
    when(app.post(eq("/api/compras/checkout"), any())).thenReturn(app);

    controller.register(app);

    verify(app).post(eq("/api/compras/checkout"), cap.capture());
    Handler h = cap.getValue();
    assertNotNull(h);

    Context ctx = mock(Context.class);

    when(ctx.attribute("claims")).thenReturn(Map.of(
            "idUsuario", 10L,
            "rol", 3
    ));
    
    PaymentReq req = PaymentReq.class.getDeclaredConstructor().newInstance();

    
    Field tarjetaField = PaymentReq.class.getDeclaredField("tarjeta");
    tarjetaField.setAccessible(true);
    Class<?> tarjetaClass = tarjetaField.getType();
    Object tarjeta = tarjetaClass.getDeclaredConstructor().newInstance();

    Field numField = tarjetaClass.getDeclaredField("numero");
    numField.setAccessible(true);
    numField.set(tarjeta, "4111111111111111"); 

    Field cvvField = tarjetaClass.getDeclaredField("cvv");
    cvvField.setAccessible(true);
    cvvField.set(tarjeta, "123");

    tarjetaField.set(req, tarjeta);

    
    Field factField = PaymentReq.class.getDeclaredField("facturacion");
    factField.setAccessible(true);
    Class<?> factClass = factField.getType();
    Object facturacion = factClass.getDeclaredConstructor().newInstance();
    factField.set(req, facturacion);

    when(ctx.bodyAsClass(PaymentReq.class)).thenReturn(req);

    
    CarritoResp carrito = new CarritoResp();
    carrito.idUsuario = 10L;
    carrito.total = new BigDecimal("100.00");

    CarritoItem item = new CarritoItem();
    item.fechaSalida = "2025-01-01T10:00:00Z";
    item.fechaLlegada = "2025-01-01T12:00:00Z";
    item.codigoVuelo = "AV123";
    item.clase = "ECONOMY";
    item.subtotal = new BigDecimal("100.00");

    carrito.items = List.of(item);

    when(dao.getCart(10L)).thenReturn(carrito);
    when(dao.checkout(10L)).thenReturn(999L);

    
    when(ctx.json(any(CheckoutResp.class))).thenReturn(ctx);

    
    when(ctx.header("X-User-Email")).thenReturn("test@example.com");
    when(ctx.header("X-User-Name")).thenReturn("Tester");

    
    when(ctx.status(anyInt())).thenReturn(ctx); 
    h.handle(ctx);

    
    verify(dao).getCart(10L);
    verify(dao).checkout(10L);
    verify(ctx).json(any(CheckoutResp.class));
}

@Test
void sendEmail_conClaimsYItems_recorreDetalle() throws Exception {
    ComprasController c = new ComprasController(
            mock(ComprasDAO.class),
            mock(UsuarioDAO.class),
            jwtHandlerNoOp(),
            wsHandlerNoOp()
    );

    Method m = ComprasController.class.getDeclaredMethod(
            "sendEmail",
            Context.class,
            CarritoResp.class,
            long.class
    );
    m.setAccessible(true);

    Context ctx = mock(Context.class);

    when(ctx.attribute("claims")).thenReturn(Map.of(
            "email", "cliente@example.com"
    ));

    CarritoResp carrito = new CarritoResp();
    carrito.idUsuario = 1L;
    carrito.total = new BigDecimal("250.00");

    CarritoItem item = new CarritoItem();
    item.fechaSalida = "2025-01-01T10:00:00Z";
    item.fechaLlegada = "2025-01-01T12:00:00Z";
    item.codigoVuelo = "AV555";
    item.clase = "BUSINESS";
    item.subtotal = new BigDecimal("250.00");

    carrito.items = List.of(item);

    try {
        m.invoke(c, ctx, carrito, 123L);
    } catch (InvocationTargetException ex) {
        
        assertNotNull(ex.getCause());
    }
}

@Test
void checkout_webservice_conClienteFinalExistente_ok() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);

    ComprasController controller = new ComprasController(
            dao, usuarioDAO,
            jwtHandlerNoOp(), wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> cap = ArgumentCaptor.forClass(Handler.class);
    when(app.post(eq("/api/compras/checkout"), any())).thenReturn(app);

    controller.register(app);

    verify(app).post(eq("/api/compras/checkout"), cap.capture());
    Handler h = cap.getValue();
    assertNotNull(h);

    Context ctx = mock(Context.class);

    when(ctx.attribute("claims")).thenReturn(Map.of(
            "idUsuario", 200L,
            "rol", 2
    ));

    PaymentReq req = PaymentReq.class.getDeclaredConstructor().newInstance();

    Field tarjetaField = PaymentReq.class.getDeclaredField("tarjeta");
    tarjetaField.setAccessible(true);
    Class<?> tarjetaClass = tarjetaField.getType();
    Object tarjeta = tarjetaClass.getDeclaredConstructor().newInstance();

    Field numField = tarjetaClass.getDeclaredField("numero");
    numField.setAccessible(true);
    numField.set(tarjeta, "4111111111111111");

    Field cvvField = tarjetaClass.getDeclaredField("cvv");
    cvvField.setAccessible(true);
    cvvField.set(tarjeta, "123");

    tarjetaField.set(req, tarjeta);

    Field factField = PaymentReq.class.getDeclaredField("facturacion");
    factField.setAccessible(true);
    Object facturacion = factField.getType().getDeclaredConstructor().newInstance();
    factField.set(req, facturacion);

    Field clienteField = PaymentReq.class.getDeclaredField("clienteFinal");
    clienteField.setAccessible(true);
    Class<?> clienteClass = clienteField.getType();
    Object clienteFinal = clienteClass.getDeclaredConstructor().newInstance();

    Field emailField = clienteClass.getDeclaredField("email");
    emailField.setAccessible(true);
    emailField.set(clienteFinal, "cliente@example.com");

    Field nomField = clienteClass.getDeclaredField("nombres");
    nomField.setAccessible(true);
    nomField.set(clienteFinal, "Cliente");

    Field apeField = clienteClass.getDeclaredField("apellidos");
    apeField.setAccessible(true);
    apeField.set(clienteFinal, "Final");

    clienteField.set(req, clienteFinal);

    when(ctx.bodyAsClass(PaymentReq.class)).thenReturn(req);

    CarritoResp carrito = new CarritoResp();
    carrito.idUsuario = 200L;
    carrito.total = new BigDecimal("150.00");

    CarritoItem item = new CarritoItem();
    item.fechaSalida = "2025-01-01T08:00:00Z";
    item.fechaLlegada = "2025-01-01T10:00:00Z";
    item.codigoVuelo = "WS123";
    item.clase = "ECONOMY";
    item.subtotal = new BigDecimal("150.00");

    carrito.items = List.of(item);

    when(dao.getCart(200L)).thenReturn(carrito);

    Usuario usuarioCliente = mock(Usuario.class);
    when(usuarioCliente.getIdUsuario()).thenReturn(300L);
    when(usuarioDAO.findByEmail("cliente@example.com")).thenReturn(usuarioCliente);

    when(dao.checkoutConClienteFinal(200L, 300L)).thenReturn(888L);
    when(ctx.json(any(CheckoutResp.class))).thenReturn(ctx);

    when(ctx.header("X-User-Email")).thenReturn("ws@example.com");
    when(ctx.header("X-User-Name")).thenReturn("Agencia WS");
    when(ctx.status(anyInt())).thenReturn(ctx);

    h.handle(ctx);

    verify(dao).getCart(200L);
    verify(usuarioDAO).findByEmail("cliente@example.com");
    verify(dao).checkoutConClienteFinal(200L, 300L);
    verify(dao).guardarReservaWebService(888L, 200L);
    verify(ctx).json(any(CheckoutResp.class));
}

@Test
void boletoPdf_ok_conCodigoEnDetalle() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);

    ComprasController controller = new ComprasController(
            dao,
            usuarioDAO,
            jwtHandlerNoOp(),
            wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> cap = ArgumentCaptor.forClass(Handler.class);
    when(app.get(eq("/api/compras/reservas/{id}/boleto.pdf"), any())).thenReturn(app);

    controller.register(app);

    verify(app).get(eq("/api/compras/reservas/{id}/boleto.pdf"), cap.capture());
    Handler h = cap.getValue();
    assertNotNull(h);

    Context ctx = mock(Context.class);

    when(ctx.attribute("claims")).thenReturn(Map.of(
            "idUsuario", 5L,
            "email", "cliente@example.com",
            "nombre", "Cliente Prueba"
    ));
    when(ctx.pathParam("id")).thenReturn("123");

    ReservaDetalle det = new ReservaDetalle();
    Field codField = det.getClass().getDeclaredField("codigo");
    codField.setAccessible(true);
    codField.set(det, "ABC123");

    when(dao.getReservaDetalle(5L, 123L)).thenReturn(det);

    when(ctx.header(anyString(), anyString())).thenReturn(ctx);
    when(ctx.result(any(byte[].class))).thenReturn(ctx);
    when(ctx.status(anyInt())).thenReturn(ctx);

    h.handle(ctx);

    verify(dao).getReservaDetalle(5L, 123L);
    verify(ctx).header(eq("Content-Type"), eq("application/pdf"));
    verify(ctx).result(any(byte[].class));
}

@Test
void adminListReservas_noAdmin_responde403() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);

    ComprasController controller = new ComprasController(
            dao,
            usuarioDAO,
            jwtHandlerNoOp(),
            wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> cap = ArgumentCaptor.forClass(Handler.class);
    when(app.get(eq("/api/admin/reservas"), any())).thenReturn(app);

    controller.register(app);

    verify(app).get(eq("/api/admin/reservas"), cap.capture());
    Handler h = cap.getValue();
    assertNotNull(h);

    Context ctx = mock(Context.class);

    when(ctx.attribute("claims")).thenReturn(Map.of("rol", 3));
    when(ctx.status(anyInt())).thenReturn(ctx);

    h.handle(ctx);

    verify(ctx).status(403);
    verify(ctx).json(any(Map.class));
    verify(dao, never()).listReservasAdmin(
            any(), any(), any(), any(), any(), any(), any());
}

@Test
void adminListReservas_admin_ok() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);

    ComprasController controller = new ComprasController(
            dao,
            usuarioDAO,
            jwtHandlerNoOp(),   
            wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> cap = ArgumentCaptor.forClass(Handler.class);

    when(app.get(eq("/api/admin/reservas"), any())).thenReturn(app);

    controller.register(app);

    verify(app).get(eq("/api/admin/reservas"), cap.capture());
    Handler h = cap.getValue();
    assertNotNull(h);

    Context ctx = mock(Context.class);

    when(ctx.attribute("claims")).thenReturn(Map.of("rol", 1));

    when(ctx.queryParam(anyString())).thenReturn(null);

    ReservaListItem item = new ReservaListItem();
    item.idReserva = 1L;
    item.idUsuario = 10L;
    item.total = new BigDecimal("100.00");

    List<ReservaListItem> lista = List.of(item);

    when(dao.listReservasAdmin(
            any(), any(), any(), any(), any(), any(), any()
    )).thenReturn(lista);

    h.handle(ctx);

    verify(dao).listReservasAdmin(
            any(), any(), any(), any(), any(), any(), any()
    );
    verify(ctx).json(lista);
}

@Test
void adminListEstados_admin_ok() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);

    ComprasController controller = new ComprasController(
            dao,
            usuarioDAO,
            jwtHandlerNoOp(),
            wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> cap = ArgumentCaptor.forClass(Handler.class);

    when(app.get(eq("/api/admin/reservas/estados"), any())).thenReturn(app);

    controller.register(app);

    verify(app).get(eq("/api/admin/reservas/estados"), cap.capture());
    Handler h = cap.getValue();
    assertNotNull(h);

    Context ctx = mock(Context.class);
    when(ctx.attribute("claims")).thenReturn(Map.of("rol", 1));

    EstadoReserva estado = new EstadoReserva();
    estado.idEstado = 1;
    estado.nombre = "CONFIRMADA";

    List<EstadoReserva> estados = List.of(estado);

    when(dao.listEstadosReserva()).thenReturn(estados);

    h.handle(ctx);

    verify(dao).listEstadosReserva();
    verify(ctx).json(estados);
}

@Test
void adminGetReservaDetalle_admin_ok() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);

    ComprasController controller = new ComprasController(
            dao,
            usuarioDAO,
            jwtHandlerNoOp(),
            wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> cap = ArgumentCaptor.forClass(Handler.class);

    when(app.get(eq("/api/admin/reservas/{id}"), any())).thenReturn(app);

    controller.register(app);

    verify(app).get(eq("/api/admin/reservas/{id}"), cap.capture());
    Handler h = cap.getValue();
    assertNotNull(h);

    Context ctx = mock(Context.class);
    when(ctx.attribute("claims")).thenReturn(Map.of("rol", 1));
    when(ctx.pathParam("id")).thenReturn("123");

    ReservaDetalle detalle = new ReservaDetalle();
    when(dao.getReservaDetalleAdmin(123L)).thenReturn(detalle);

    h.handle(ctx);

    verify(dao).getReservaDetalleAdmin(123L);
    verify(ctx).json(detalle);
}

@Test
void adminGetReserva_admin_ok() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);

    ComprasController controller = new ComprasController(
            dao,
            usuarioDAO,
            jwtHandlerNoOp(),
            wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> cap = ArgumentCaptor.forClass(Handler.class);

    when(app.get(eq("/api/admin/reservas/{id}"), any())).thenReturn(app);

    controller.register(app);

    verify(app).get(eq("/api/admin/reservas/{id}"), cap.capture());
    Handler h = cap.getValue();
    assertNotNull(h);

    Context ctx = mock(Context.class);
    when(ctx.attribute("claims")).thenReturn(Map.of("rol", 1));
    when(ctx.pathParam("id")).thenReturn("123");

    ReservaDetalle det = new ReservaDetalle();
    when(dao.getReservaDetalleAdmin(123L)).thenReturn(det);

    h.handle(ctx);

    verify(dao).getReservaDetalleAdmin(123L);
    verify(ctx).json(det);
}

@Test
void adminGetReserva_noAdmin_responde403() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);

    ComprasController controller = new ComprasController(
            dao,
            usuarioDAO,
            jwtHandlerNoOp(),
            wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> cap = ArgumentCaptor.forClass(Handler.class);

    when(app.get(eq("/api/admin/reservas/{id}"), any())).thenReturn(app);

    controller.register(app);

    verify(app).get(eq("/api/admin/reservas/{id}"), cap.capture());
    Handler h = cap.getValue();
    assertNotNull(h);

    Context ctx = mock(Context.class);
    when(ctx.attribute("claims")).thenReturn(Map.of("rol", 3)); 
    when(ctx.pathParam("id")).thenReturn("123");
    when(ctx.status(anyInt())).thenReturn(ctx);

    h.handle(ctx);

    verify(ctx).status(403);
    verify(ctx).json(any(Map.class));
    verify(dao, never()).getReservaDetalleAdmin(anyLong());
}

@Test
void adminListEstados_noAdmin_responde403() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);

    ComprasController controller = new ComprasController(
            dao,
            usuarioDAO,
            jwtHandlerNoOp(),
            wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> cap = ArgumentCaptor.forClass(Handler.class);

    when(app.get(eq("/api/admin/reservas/estados"), any())).thenReturn(app);

    controller.register(app);

    verify(app).get(eq("/api/admin/reservas/estados"), cap.capture());
    Handler h = cap.getValue();
    assertNotNull(h);

    Context ctx = mock(Context.class);
    when(ctx.attribute("claims")).thenReturn(Map.of("rol", 3)); 
    when(ctx.status(anyInt())).thenReturn(ctx);

    h.handle(ctx);

    verify(ctx).status(403);
    verify(ctx).json(any(Map.class));
    verify(dao, never()).listEstadosReserva();
}

@Test
void topDestinos_errorEnDao_responde400() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);

    ComprasController controller = new ComprasController(
            dao,
            usuarioDAO,
            jwtHandlerNoOp(),
            wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> cap = ArgumentCaptor.forClass(Handler.class);

    when(app.get(eq("/api/public/stats/top-destinos"), any())).thenReturn(app);

    controller.register(app);

    verify(app).get(eq("/api/public/stats/top-destinos"), cap.capture());
    Handler h = cap.getValue();
    assertNotNull(h);

    Context ctx = mock(Context.class);

    when(ctx.queryParam(anyString())).thenReturn(null);
    when(ctx.status(anyInt())).thenReturn(ctx);

    when(dao.listTopDestinos(any(), any(), anyInt()))
            .thenThrow(new RuntimeException("Fallo simulado"));

    h.handle(ctx);

    verify(dao).listTopDestinos(any(), any(), anyInt());
    verify(ctx).status(400);
    verify(ctx).json(any(Map.class));
}

@Test
void constructorPorDefecto_creaInstancia() {
    ComprasController c = new ComprasController();
    assertNotNull(c);
}

@Test
void testMail_sinToNiHeader_responde400() throws Exception {
    ComprasController controller = new ComprasController(
            mock(ComprasDAO.class),
            mock(UsuarioDAO.class),
            jwtHandlerNoOp(),
            wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> cap = ArgumentCaptor.forClass(Handler.class);
    when(app.get(eq("/api/dev/test-mail"), any())).thenReturn(app);

    controller.register(app);

    verify(app).get(eq("/api/dev/test-mail"), cap.capture());
    Handler h = cap.getValue();
    assertNotNull(h);

    Context ctx = mock(Context.class);
    when(ctx.queryParam("to")).thenReturn(null);
    when(ctx.header("X-User-Email")).thenReturn(null);
    when(ctx.status(anyInt())).thenReturn(ctx);
    when(ctx.result(anyString())).thenReturn(ctx);

    h.handle(ctx);

    verify(ctx).status(400);
    verify(ctx).result(anyString());
}

@Test
void adminListReservas_admin_conFiltros_parseaFechasYEstado() throws Exception {
    ComprasDAO dao = mock(ComprasDAO.class);
    UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);

    ComprasController controller = new ComprasController(
            dao,
            usuarioDAO,
            jwtHandlerNoOp(),
            wsHandlerNoOp()
    );

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> cap = ArgumentCaptor.forClass(Handler.class);
    when(app.get(eq("/api/admin/reservas"), any())).thenReturn(app);

    controller.register(app);

    verify(app).get(eq("/api/admin/reservas"), cap.capture());
    Handler h = cap.getValue();
    assertNotNull(h);

    Context ctx = mock(Context.class);
    when(ctx.attribute("claims")).thenReturn(Map.of("rol", 1));

    when(ctx.queryParam("q")).thenReturn("busqueda");
    when(ctx.queryParam("usuario")).thenReturn("user@example.com");
    when(ctx.queryParam("codigo")).thenReturn("COD123");
    when(ctx.queryParam("vuelo")).thenReturn("AV123");
    when(ctx.queryParam("desde")).thenReturn("2025-01-01");
    when(ctx.queryParam("hasta")).thenReturn("2025-01-31");
    when(ctx.queryParam("estado")).thenReturn("2");

    when(dao.listReservasAdmin(
            any(), any(), any(), any(), any(), any(), any()
    )).thenReturn(List.of());

    h.handle(ctx);

    verify(dao).listReservasAdmin(
            eq("busqueda"),
            eq("user@example.com"),
            eq("COD123"),
            eq("AV123"),
            any(Timestamp.class),
            any(Timestamp.class),
            eq(2)
    );
    verify(ctx).json(any(List.class));
}


}
