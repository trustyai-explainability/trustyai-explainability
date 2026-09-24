package org.kie.trustyai.service.endpoints.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import org.jboss.logging.Logger;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@PreMatching
public class GzipRequestFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(GzipRequestFilter.class);
    static final int MAX_DECOMPRESSED_SIZE = 100 * 1024 * 1024; // 100 MB

    @Override
    public void filter(ContainerRequestContext ctx) {
        String encoding = ctx.getHeaderString("Content-Encoding");
        if (encoding == null || !encoding.toLowerCase(Locale.ROOT).contains("gzip")) {
            return;
        }

        try {
            byte[] compressed = ctx.getEntityStream().readAllBytes();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = gzis.read(buffer)) != -1) {
                    bos.write(buffer, 0, len);
                    if (bos.size() > MAX_DECOMPRESSED_SIZE) {
                        throw new IOException("Decompressed payload exceeds " + MAX_DECOMPRESSED_SIZE + " bytes");
                    }
                }
            }
            ctx.setEntityStream(new ByteArrayInputStream(bos.toByteArray()));
            ctx.getHeaders().remove("Content-Encoding");
            LOG.debugf("Decompressed gzip request body (%d -> %d bytes)", compressed.length, bos.size());
        } catch (IOException e) {
            LOG.warnf("Failed to decompress gzip request: %s", e.getMessage());
            ctx.abortWith(Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.TEXT_PLAIN_TYPE)
                    .entity("Request body could not be decompressed as gzip: invalid or corrupted content.")
                    .build());
        }
    }
}
