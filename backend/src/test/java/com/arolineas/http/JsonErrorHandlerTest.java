package com.aerolineas.http;

import io.javalin.http.Context;
import io.javalin.http.ExceptionHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JsonErrorHandlerTest {

  @Test
  void of_creaHandlerQueSeteaStatusYJsonConErrorResponse() throws Exception {
    Context ctx = mock(Context.class);

    when(ctx.status(anyInt())).thenReturn(ctx);
    when(ctx.json(any())).thenReturn(ctx);

    int status = 422;
    ExceptionHandler<RuntimeException> handler = JsonErrorHandler.of(status);
    RuntimeException ex = new RuntimeException("algo salió mal");

    handler.handle(ex, ctx);

    verify(ctx).status(status);

    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(ctx).json(captor.capture());

    Object body = captor.getValue();
    assertNotNull(body);
    assertInstanceOf(JsonErrorHandler.ErrorResponse.class, body);

    JsonErrorHandler.ErrorResponse err = (JsonErrorHandler.ErrorResponse) body;
    assertEquals("algo salió mal", err.error());
  }

  @Test
  void errorResponse_guardaElMensajeCorrectamente() {
    JsonErrorHandler.ErrorResponse err =
        new JsonErrorHandler.ErrorResponse("mensaje x");

    assertEquals("mensaje x", err.error());
  }
}
