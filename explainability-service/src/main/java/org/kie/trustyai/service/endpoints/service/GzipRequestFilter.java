package org.kie.trustyai.service.endpoints.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.jboss.logging.Logger;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS filter that automatically decompresses gzip-encoded request bodies for data upload endpoints.
 *
 * This filter is scoped to the /data/upload endpoint to avoid unexpected behavior for other consumers.
 * It checks for the "Content-Encoding: gzip" header and transparently decompresses the request body
 * before it reaches the endpoint handlers.
 *
 * This is necessary because the KServe agent sidecar automatically gzip-compresses CloudEvent
 * payloads when logging to TrustyAI in RawDeployment mode.
 *
 * The filter handles multiple/stacked encodings (e.g., "gzip, br") by:
 * 1. Detecting if "gzip" is present in the Content-Encoding header
 * 2. Decompressing the gzip layer
 * 3. Removing only "gzip" from the header, preserving other encodings for downstream processing
 *
 * If decompression fails, returns HTTP 400 (Bad Request) with a clear error message rather than
 * allowing the IOException to surface as a generic 500 error.
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class GzipRequestFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(GzipRequestFilter.class);
    private static final String CONTENT_ENCODING = "Content-Encoding";
    private static final String GZIP = "gzip";
    private static final String DATA_UPLOAD_PATH = "/data/upload";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        String contentEncoding = requestContext.getHeaderString(CONTENT_ENCODING);

        // Only apply filter to /data/upload endpoint to avoid unexpected behavior for other consumers
        if (!path.endsWith(DATA_UPLOAD_PATH)) {
            return;
        }

        // Support multiple/stacked encodings (e.g., "gzip, br") by checking if gzip is present
        if (contentEncoding != null && contentEncoding.toLowerCase(Locale.ROOT).contains(GZIP)) {
            LOG.debugf("Decompressing gzip-encoded request body for path: %s", path);

            try {
                InputStream originalStream = requestContext.getEntityStream();
                GZIPInputStream gzipStream = new GZIPInputStream(originalStream);
                requestContext.setEntityStream(gzipStream);

                // Remove only "gzip" from Content-Encoding, preserving other encodings for downstream processing
                updateContentEncoding(requestContext, contentEncoding);

                LOG.debugf("Successfully decompressed gzip request for path: %s", path);
            } catch (IOException e) {
                LOG.errorf(e, "Failed to decompress gzip-encoded request body for path: %s", path);

                // Return 400 Bad Request with clear message instead of letting it surface as 500
                Response response = Response.status(Response.Status.BAD_REQUEST)
                        .type(MediaType.TEXT_PLAIN_TYPE)
                        .entity("Request body could not be decompressed as gzip: invalid or corrupted content.")
                        .build();

                requestContext.abortWith(response);
            }
        }
    }

    /**
     * Removes "gzip" from the Content-Encoding header while preserving other encodings.
     * For example: "gzip, br" becomes "br", and "gzip" is removed entirely.
     */
    private void updateContentEncoding(ContainerRequestContext requestContext, String contentEncoding) {
        String remainingEncodings = Arrays.stream(contentEncoding.split(","))
                .map(String::trim)
                .filter(encoding -> !encoding.equalsIgnoreCase(GZIP))
                .collect(Collectors.joining(", "));

        if (remainingEncodings.isEmpty()) {
            // No other encodings remain, remove the header entirely
            requestContext.getHeaders().remove(CONTENT_ENCODING);
        } else {
            // Update header with remaining encodings
            requestContext.getHeaders().putSingle(CONTENT_ENCODING, remainingEncodings);
        }
    }
}
