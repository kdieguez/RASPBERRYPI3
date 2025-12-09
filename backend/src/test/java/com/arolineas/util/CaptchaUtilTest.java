package com.aerolineas.util;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvBuilder;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CaptchaUtilTest { 
  
  private static HttpClient swapHttpClient(HttpClient newClient) throws Exception {
    Field f = CaptchaUtil.class.getDeclaredField("client");
    f.setAccessible(true);
    HttpClient old = (HttpClient) f.get(null);
    f.set(null, newClient);
    return old;
  }

  @Test
  void verify_devMode_retornaTrue_cuandoNoHaySecret() {
    Assumptions.assumeTrue(
        System.getenv("RECAPTCHA_SECRET") == null,
        "RECAPTCHA_SECRET definida en el entorno; no se puede probar modo dev de forma determinista"
    );

    try (MockedStatic<Dotenv> dotenvMock = mockStatic(Dotenv.class)) {

      DotenvBuilder builder = mock(DotenvBuilder.class);
      Dotenv dotenv = mock(Dotenv.class);

      dotenvMock.when(Dotenv::configure).thenReturn(builder);
      when(builder.ignoreIfMissing()).thenReturn(builder);
      when(builder.load()).thenReturn(dotenv);
      when(dotenv.get("RECAPTCHA_SECRET")).thenReturn(null); 

      boolean ok = CaptchaUtil.verify("cualquier-token");

      assertTrue(ok);
    }
  }

  
  @Test
  void verify_errorEnDotenv_enGetenv_usaModoDevYRetornaTrue() {
    Assumptions.assumeTrue(
        System.getenv("RECAPTCHA_SECRET") == null ||
        System.getenv("RECAPTCHA_SECRET").isBlank(),
        "RECAPTCHA_SECRET definida y no vacía; no se puede forzar el uso de Dotenv en getenv"
    );

    try (MockedStatic<Dotenv> dotenvMock = mockStatic(Dotenv.class)) {

      DotenvBuilder builder = mock(DotenvBuilder.class);

      dotenvMock.when(Dotenv::configure).thenReturn(builder);
      when(builder.ignoreIfMissing()).thenReturn(builder);
      
      when(builder.load()).thenThrow(new RuntimeException("boom dotenv"));

      boolean ok = CaptchaUtil.verify("token-x");

      
      assertTrue(ok);
    }
  }

  @Test
  void verify_tokenNull_retornaFalse_cuandoHaySecret() {
    try (MockedStatic<Dotenv> dotenvMock = mockStatic(Dotenv.class)) {

      DotenvBuilder builder = mock(DotenvBuilder.class);
      Dotenv dotenv = mock(Dotenv.class);

      dotenvMock.when(Dotenv::configure).thenReturn(builder);
      when(builder.ignoreIfMissing()).thenReturn(builder);
      when(builder.load()).thenReturn(dotenv);
      when(dotenv.get("RECAPTCHA_SECRET")).thenReturn("dummy-secret");

      boolean ok = CaptchaUtil.verify(null);

      assertFalse(ok);
    }
  }

  @Test
  void verify_http200_successTrue_retornaTrue() throws Exception {
    try (MockedStatic<Dotenv> dotenvMock = mockStatic(Dotenv.class)) {

      DotenvBuilder builder = mock(DotenvBuilder.class);
      Dotenv dotenv = mock(Dotenv.class);
      dotenvMock.when(Dotenv::configure).thenReturn(builder);
      when(builder.ignoreIfMissing()).thenReturn(builder);
      when(builder.load()).thenReturn(dotenv);
      when(dotenv.get("RECAPTCHA_SECRET")).thenReturn("dummy-secret");

      HttpClient mockClient = mock(HttpClient.class);
      @SuppressWarnings("unchecked")
      HttpResponse<String> mockRes = (HttpResponse<String>) mock(HttpResponse.class);

      when(mockRes.statusCode()).thenReturn(200);
      when(mockRes.body()).thenReturn("{\"success\": true}");

      HttpClient old = swapHttpClient(mockClient);
      try {
        when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(mockRes);

        boolean ok = CaptchaUtil.verify("token-valido");

        assertTrue(ok);
      } finally {
        swapHttpClient(old);
      }
    }
  }

  @Test
  void verify_httpNo200_retornaFalse() throws Exception {
    try (MockedStatic<Dotenv> dotenvMock = mockStatic(Dotenv.class)) {

      DotenvBuilder builder = mock(DotenvBuilder.class);
      Dotenv dotenv = mock(Dotenv.class);
      dotenvMock.when(Dotenv::configure).thenReturn(builder);
      when(builder.ignoreIfMissing()).thenReturn(builder);
      when(builder.load()).thenReturn(dotenv);
      when(dotenv.get("RECAPTCHA_SECRET")).thenReturn("dummy-secret");

      HttpClient mockClient = mock(HttpClient.class);
      @SuppressWarnings("unchecked")
      HttpResponse<String> mockRes = (HttpResponse<String>) mock(HttpResponse.class);

      when(mockRes.statusCode()).thenReturn(500);
      when(mockRes.body()).thenReturn("error");

      HttpClient old = swapHttpClient(mockClient);
      try {
        when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(mockRes);

        boolean ok = CaptchaUtil.verify("token-x");

        assertFalse(ok);
      } finally {
        swapHttpClient(old);
      }
    }
  }

  @Test
  void verify_excepcionEnHttp_retornaFalse() throws Exception {
    try (MockedStatic<Dotenv> dotenvMock = mockStatic(Dotenv.class)) {

      DotenvBuilder builder = mock(DotenvBuilder.class);
      Dotenv dotenv = mock(Dotenv.class);
      dotenvMock.when(Dotenv::configure).thenReturn(builder);
      when(builder.ignoreIfMissing()).thenReturn(builder);
      when(builder.load()).thenReturn(dotenv);
      when(dotenv.get("RECAPTCHA_SECRET")).thenReturn("dummy-secret");

      HttpClient mockClient = mock(HttpClient.class);

      HttpClient old = swapHttpClient(mockClient);
      try {
        when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(new IOException("fallo de red"));

        boolean ok = CaptchaUtil.verify("token-x");

        assertFalse(ok);
      } finally {
        swapHttpClient(old);
      }
    }
  }
}
