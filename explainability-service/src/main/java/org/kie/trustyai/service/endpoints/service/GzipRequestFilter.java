package org.kie.trustyai.service.endpoints.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.jboss.logging.Logger;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS filter that automatically decompresses gzip-encoded request bodies.
 *
 * This filter checks for the "Content-Encoding: gzip" header and transparently
 * decompresses the request body before it reaches the endpoint handlers.
 *
 * This is necessary because the KServe agent sidecar automatically gzip-compresses
 * CloudEvent payloads when logging to TrustyAI in RawDeployment mode.
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class GzipRequestFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(GzipRequestFilter.class);
    private static final String CONTENT_ENCODING = "Content-Encoding";
    private static final String GZIP = "gzip";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String contentEncoding = requestContext.getHeaderString(CONTENT_ENCODING);

        if (contentEncoding != null && contentEncoding.equalsIgnoreCase(GZIP)) {
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
                throw e;
            }
        }
    }
}
