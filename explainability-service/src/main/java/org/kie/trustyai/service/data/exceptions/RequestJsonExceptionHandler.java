package org.kie.trustyai.service.data.exceptions;

import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class RequestJsonExceptionHandler implements ExceptionMapper<JsonProcessingException> {

    @Override
    public Response toResponse(JsonProcessingException exception) {

        return Response.status(Response.Status.BAD_REQUEST).entity("JSON processing error: " + exception.getMessage()).build();

    }
}
