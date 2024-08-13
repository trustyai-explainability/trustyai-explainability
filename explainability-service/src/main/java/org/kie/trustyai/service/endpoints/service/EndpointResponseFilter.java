package org.kie.trustyai.service.endpoints.service;


import jakarta.annotation.Priority;

import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(9999)
public class EndpointResponseFilter implements ContainerResponseFilter {
    public static String NOT_FOUND_MESSAGE_FMT = "Error: The queried endpoint \"%s\" does not exist within TrustyAI.";

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext){
        int status = responseContext.getStatus();

        // 404 Mapper
        if (status == Response.Status.NOT_FOUND.getStatusCode() && !responseContext.hasEntity()) {
            String queriedPath = requestContext.getUriInfo().getPath();
            responseContext.setEntity(String.format(NOT_FOUND_MESSAGE_FMT, queriedPath));
        }
    }
}
