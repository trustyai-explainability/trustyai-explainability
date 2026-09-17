package org.kie.trustyai.service.endpoints.service;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import jakarta.annotation.Priority;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(9999)
public class EndpointResponseFilter implements ContainerResponseFilter {
    public static String NOT_FOUND_MESSAGE_FMT = "Error: The queried endpoint \"%s\" does not exist within or is not provided by this version of TrustyAI.";

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        int status = responseContext.getStatus();

        // 404 Mapper
        if (status == Response.Status.NOT_FOUND.getStatusCode() && !responseContext.hasEntity()) {
            String queriedPath = requestContext.getUriInfo().getPath();
            responseContext.setEntity(String.format(NOT_FOUND_MESSAGE_FMT, queriedPath));
        }
    }

    @ServerExceptionMapper
    public Response mapNotFound(NotFoundException exception, ContainerRequestContext requestContext) {
        String queriedPath = requestContext.getUriInfo().getPath();
        return Response.status(Response.Status.NOT_FOUND)
                .entity(String.format(NOT_FOUND_MESSAGE_FMT, queriedPath))
                .build();
    }
}
