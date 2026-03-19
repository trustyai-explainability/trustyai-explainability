package org.kie.trustyai.service.endpoints.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
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
 * JAX-RS filter that automatically decompresses gzip-encoded request bodies.
 *
 * This filter checks for the "Content-Encoding: gzip" header and transparently
 * decompresses the request body before it reaches the endpoint handlers.
 *
 * This is necessary because the KServe agent sidecar automatically gzip-compresses
 * CloudEvent payloads when logging to TrustyAI in RawDeployment mode.
 *
 * The filter handles multiple/stacked encodings (e.g., "gzip, br") by checking if
 * the Content-Encoding header contains "gzip" rather than requiring an exact match.
 *
 * If decompression fails, returns HTTP 400 (Bad Request) with a clear error message
 * rather than allowing the IOException to surface as a generic 500 error.
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class GzipRequestFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(GzipRequestFilter.class);
    private static final String CONTENT_ENCODING = "Content-Encoding";
    private static final String GZIP = "gzip";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String contentEncoding = requestContext.getHeaderString(CONTENT_ENCODING);

        // Support multiple/stacked encodings (e.g., "gzip, br") by checking if gzip is present
        if (contentEncoding != null && contentEncoding.toLowerCase(Locale.ROOT).contains(GZIP)) {
            LOG.debugf("Decompressing gzip-encoded request body for path: %s", requestContext.getUriInfo().getPath());

            try {
                InputStream originalStream = requestContext.getEntityStream();
                GZIPInputStream gzipStream = new GZIPInputStream(originalStream);
                requestContext.setEntityStream(gzipStream);

                // Remove the Content-Encoding header since we've decompressed the body
                requestContext.getHeaders().remove(CONTENT_ENCODING);

                LOG.debugf("Successfully decompressed gzip request for path: %s", requestContext.getUriInfo().getPath());
            } catch (IOException e) {
                LOG.errorf(e, "Failed to decompress gzip-encoded request body for path: %s", requestContext.getUriInfo().getPath());

                // Return 400 Bad Request with clear message instead of letting it surface as 500
                Response response = Response.status(Response.Status.BAD_REQUEST)
                        .type(MediaType.TEXT_PLAIN_TYPE)
                        .entity("Request body could not be decompressed as gzip: invalid or corrupted content.")
                        .build();

                requestContext.abortWith(response);
            }
        }
    }
}
