package org.kie.trustyai.service.payloads.exceptions;

import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class HibernateConnectionExceptionMapper implements ExceptionMapper<PersistenceException> {

    @Override
    public Response toResponse(PersistenceException exception) {

        System.out.println("I caught an exception!!");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error connecting to Hibernate: " + exception.getMessage())
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

}
