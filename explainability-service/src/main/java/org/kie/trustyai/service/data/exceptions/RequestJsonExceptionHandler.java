package org.kie.trustyai.service.data.exceptions;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonProcessingException;

@Provider
public class RequestJsonExceptionHandler implements ExceptionMapper<JsonProcessingException> {

    @Override
    public Response toResponse(JsonProcessingException exception) {

        return Response.status(Response.Status.BAD_REQUEST).entity("JSON processing error: " + exception.getMessage()).build();

    }
}
