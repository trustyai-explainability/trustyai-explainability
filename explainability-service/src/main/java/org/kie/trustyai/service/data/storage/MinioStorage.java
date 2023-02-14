package org.kie.trustyai.service.data.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;
import org.kie.trustyai.service.config.readers.MinioConfig;
import org.kie.trustyai.service.data.exceptions.StorageReadException;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.MinioException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.quarkus.arc.lookup.LookupIfProperty;

@LookupIfProperty(name = "service.storage.format", stringValue = "MINIO")
@ApplicationScoped
public class MinioStorage implements Storage {

    private static final Logger LOG = Logger.getLogger(MinioStorage.class);

    private final MinioClient minioClient;

    private final String bucketName;

    private final String inputFilename;
    private final String outputFilename;

    public MinioStorage(MinioConfig config) {
        LOG.info("Starting MinIO storage consumer");
        this.bucketName = config.bucketName();
        this.inputFilename = config.inputFilename();
        this.outputFilename = config.outputFilename();
        LOG.info("MinIO data location: endpoint=" + config.endpoint() + ", bucket=" + bucketName + ", input file="
                + inputFilename
                + ", output filename=" + outputFilename);
        this.minioClient =
                MinioClient.builder()
                        .endpoint(config.endpoint())
                        .credentials(config.accessKey(), config.secretKey())
                        .build();
    }

    private StatObjectResponse getObjectStats(String bucket, String filename)
            throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException,
            InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        return minioClient.statObject(
                StatObjectArgs.builder().bucket(bucket).object(filename).build());
    }

    private StatObjectResponse isObjectAvailable(String bucketName, String filename) throws MinioException {
        try {
            return getObjectStats(bucketName, filename);
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new MinioException(e.getMessage());
        }
    }

    private byte[] readFile(String bucketName, String filename)
            throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(filename)
                        .build())
                .readAllBytes();
    }

    @Override
    public ByteBuffer getInputData() throws StorageReadException {
        try {
            isObjectAvailable(this.bucketName, this.inputFilename);
        } catch (MinioException e) {
            LOG.error("Input file '" + this.inputFilename + "' at bucket '" + this.bucketName + "' is not available");
            throw new StorageReadException(e.getMessage());
        }
        try {
            return ByteBuffer.wrap(readFile(this.bucketName, this.inputFilename));
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            LOG.error("Error reading input file");
            throw new StorageReadException(e.getMessage());
        }
    }

    @Override
    public ByteBuffer getOutputData() throws StorageReadException {
        try {
            isObjectAvailable(this.bucketName, this.outputFilename);
        } catch (MinioException e) {
            LOG.error("Input file '" + this.outputFilename + "' at bucket '" + this.bucketName + "' is not available");
            throw new StorageReadException(e.getMessage());
        }
        try {
            return ByteBuffer.wrap(readFile(this.bucketName, this.outputFilename));
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            LOG.error("Error reading output file");
            throw new StorageReadException(e.getMessage());
        }
    }
}
