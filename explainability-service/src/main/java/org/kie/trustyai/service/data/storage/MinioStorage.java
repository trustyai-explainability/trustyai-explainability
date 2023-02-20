package org.kie.trustyai.service.data.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;
import org.kie.trustyai.service.config.readers.MinioConfig;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;

import io.minio.*;
import io.minio.errors.*;
import io.quarkus.arc.lookup.LookupIfProperty;

@LookupIfProperty(name = "service.storage.format", stringValue = "MINIO")
@ApplicationScoped
public class MinioStorage extends Storage {

    private static final Logger LOG = Logger.getLogger(MinioStorage.class);

    private final MinioClient minioClient;

    private final String bucketName;

    private final String inputFilename;
    private final String outputFilename;
    private final String endpoint;
    private final String accessKey;
    private final String secretKey;

    public MinioStorage(MinioConfig config) {
        LOG.info("Starting MinIO storage consumer");
        if (config.bucketName().isEmpty()) {
            throw new IllegalArgumentException("Missing MinIO bucket");
        } else {
            this.bucketName = config.bucketName().get();
        }
        if (config.inputFilename().isEmpty()) {
            throw new IllegalArgumentException("Missing MinIO input filename");
        } else {
            this.inputFilename = config.inputFilename().get();
        }
        if (config.outputFilename().isEmpty()) {
            throw new IllegalArgumentException("Missing MinIO output filename");
        } else {
            this.outputFilename = config.outputFilename().get();
        }
        if (config.endpoint().isEmpty()) {
            throw new IllegalArgumentException("Missing MinIO endpoint");
        } else {
            this.endpoint = config.endpoint().get();
        }
        if (config.accessKey().isEmpty()) {
            throw new IllegalArgumentException("Missing MinIO access key");
        } else {
            this.accessKey = config.accessKey().get();
        }
        if (config.secretKey().isEmpty()) {
            throw new IllegalArgumentException("Missing MinIO secret key");
        } else {
            this.secretKey = config.secretKey().get();
        }

        LOG.info("MinIO data location: endpoint=" + config.endpoint() + ", bucket=" + bucketName + ", input file="
                + inputFilename
                + ", output filename=" + outputFilename);
        this.minioClient =
                MinioClient.builder()
                        .endpoint(this.endpoint)
                        .credentials(this.accessKey, this.secretKey)
                        .build();
    }

    private StatObjectResponse getObjectStats(String bucket, String filename)
            throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException,
            InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        return minioClient.statObject(
                StatObjectArgs.builder().bucket(bucket).object(filename).build());
    }

    private StatObjectResponse isObjectAvailable(String bucketName, String filename) throws StorageReadException {
        try {
            return getObjectStats(bucketName, filename);
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new StorageReadException(e.getMessage());
        }
    }

    private boolean bucketExists(String bucketName) throws StorageReadException {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException | InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException
                | XmlParserException e) {
            throw new StorageReadException(e.getMessage());
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
        isObjectAvailable(this.bucketName, this.inputFilename);
        try {
            return ByteBuffer.wrap(readFile(this.bucketName, this.inputFilename));
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            LOG.error("Error reading input file");
            throw new StorageReadException(e.getMessage());
        }
    }

    @Override
    public ByteBuffer getOutputData() throws StorageReadException {
        isObjectAvailable(this.bucketName, this.outputFilename);
        try {
            return ByteBuffer.wrap(readFile(this.bucketName, this.outputFilename));
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            LOG.error("Error reading output file");
            throw new StorageReadException(e.getMessage());
        }
    }

    private void saveData(ByteBuffer byteBuffer, String bucketName, String filename) throws StorageWriteException, StorageReadException {

        try {
            if (!bucketExists(bucketName)) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            final String data = new String(byteBuffer.array(), StandardCharsets.UTF_8);
            final InputStream inputStream = new ByteArrayInputStream(data.getBytes());

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .stream(inputStream, data.length(), -1).build());
        } catch (MinioException | NoSuchAlgorithmException | IOException | InvalidKeyException e) {
            throw new StorageWriteException(e.getMessage());
        } catch (StorageReadException e) {
            throw new StorageReadException(e.getMessage());
        }
    }

    private synchronized void appendData(ByteBuffer byteBuffer, String bucketName, String filename) throws StorageWriteException {
        final String tempFilename = "tmp-" + UUID.randomUUID();
        final List<ComposeSource> sources = List.of(
                ComposeSource.builder().bucket(bucketName).object(filename).build(),
                ComposeSource.builder().bucket(bucketName).object(tempFilename).build());
        try {
            saveData(byteBuffer, bucketName, tempFilename);
            minioClient.composeObject(ComposeObjectArgs.builder().bucket(bucketName).object(filename)
                    .sources(sources).build());
            // Delete temporary file
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(tempFilename).build());
        } catch (StorageWriteException | StorageReadException | ServerException | InsufficientDataException | ErrorResponseException | IOException | NoSuchAlgorithmException | InvalidKeyException
                | InvalidResponseException | XmlParserException | InternalException e) {
            throw new StorageWriteException(e.getMessage());
        }
    }

    @Override
    public void saveInputData(ByteBuffer byteBuffer) throws StorageWriteException, StorageReadException {
        saveData(byteBuffer, bucketName, inputFilename);
    }

    @Override
    public void saveOutputData(ByteBuffer byteBuffer) throws StorageWriteException, StorageReadException {
        saveData(byteBuffer, bucketName, outputFilename);
    }

    @Override
    public void appendInputData(ByteBuffer byteBuffer) throws StorageWriteException, StorageReadException {
        // TODO: Append data on MinIO
    }

    @Override
    public void appendOutputData(ByteBuffer byteBuffer) throws StorageWriteException, StorageReadException {
        // TODO: Append data on MinIO
    }

    @Override
    public boolean inputExists() throws StorageReadException {
        return false;
    }

    @Override
    public boolean outputExists() throws StorageReadException {
        return false;
    }
}