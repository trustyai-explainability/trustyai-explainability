package org.kie.trustyai.service.payloads.exceptions;

import org.kie.trustyai.service.data.exceptions.DataframeCreateException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class DataframeCreateExceptionMapper implements ExceptionMapper<DataframeCreateException> {

    @Override
    public Response toResponse(DataframeCreateException exception) {

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error reading dataset: " + exception.getMessage())
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

}
