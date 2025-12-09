package com.aerolineas.middleware;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorsTest {

  @Test
  void before_sinRequestHeadersUsaDefaultAllowHeaders() throws Exception {
    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> beforeCaptor = ArgumentCaptor.forClass(Handler.class);

    Cors.install(app);

    verify(app).before(beforeCaptor.capture());
    Handler beforeHandler = beforeCaptor.getValue();
    assertNotNull(beforeHandler);

    Context ctx = mock(Context.class);
    when(ctx.header("Access-Control-Request-Headers")).thenReturn(null);

    beforeHandler.handle(ctx);

    verify(ctx).header("Access-Control-Allow-Origin", "*");
    verify(ctx).header("Vary", "Origin");
    verify(ctx).header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,PATCH");
    verify(ctx).header("Access-Control-Max-Age", "86400");
    verify(ctx).header("Access-Control-Allow-Headers",
        "Authorization, Content-Type, X-User-Id, x-user-id");
  }

  @Test
  void before_conRequestHeadersUsaLosDelRequest() throws Exception {
    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> beforeCaptor = ArgumentCaptor.forClass(Handler.class);

    Cors.install(app);

    verify(app).before(beforeCaptor.capture());
    Handler beforeHandler = beforeCaptor.getValue();

    Context ctx = mock(Context.class);
    String requested = "X-Custom-1, X-Custom-2";
    when(ctx.header("Access-Control-Request-Headers")).thenReturn(requested);

    beforeHandler.handle(ctx);

    verify(ctx).header("Access-Control-Allow-Headers", requested);
    verify(ctx).header("Access-Control-Allow-Origin", "*");
    verify(ctx).header("Vary", "Origin");
    verify(ctx).header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,PATCH");
  }

  @Test
  void optionsHandler_responde204YSeteaHeaders() throws Exception {
    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> optionsCaptor = ArgumentCaptor.forClass(Handler.class);

    Cors.install(app);

    verify(app).options(eq("/*"), optionsCaptor.capture());
    Handler optionsHandler = optionsCaptor.getValue();
    assertNotNull(optionsHandler);

    Context ctx = mock(Context.class);
    when(ctx.header("Access-Control-Request-Headers")).thenReturn(null);

    optionsHandler.handle(ctx);

    verify(ctx).status(204);
    verify(ctx).header("Access-Control-Allow-Origin", "*");
  }
}
