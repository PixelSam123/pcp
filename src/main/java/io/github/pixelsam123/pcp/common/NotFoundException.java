package io.github.pixelsam123.pcp.common;

import jakarta.ws.rs.core.Response;

public class NotFoundException extends HttpException {
    public NotFoundException(String entityName) {
        super(Response.Status.NOT_FOUND, entityName + " Not Found");
    }
}
