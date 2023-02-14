package org.kie.trustyai.service.payloads.exceptions;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.kie.trustyai.service.data.exceptions.DataframeCreateException;

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
