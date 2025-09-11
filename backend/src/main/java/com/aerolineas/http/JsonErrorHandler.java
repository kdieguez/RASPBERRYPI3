package com.aerolineas.http;

import io.javalin.http.ExceptionHandler;

public class JsonErrorHandler {
    public static <T extends Exception> ExceptionHandler<T> of(int status) {
        return (e, ctx) -> {
            ctx.status(status).json(new ErrorResponse(e.getMessage()));
        };
    }

    public record ErrorResponse(String error) {}
}
