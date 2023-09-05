package io.github.pixelsam123.pcp;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.Map;

/**
 * WebApplicationException with return body construction
 */
public class HttpException extends WebApplicationException {
    public HttpException(Response.Status status, String message) {
        super(Response.status(status).entity(Map.of("details", message)).build());
    }
}
