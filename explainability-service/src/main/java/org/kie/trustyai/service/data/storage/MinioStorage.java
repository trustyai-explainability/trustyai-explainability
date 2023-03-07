package org.kie.trustyai.service.data.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;
import org.kie.trustyai.service.config.storage.MinioConfig;
import org.kie.trustyai.service.config.storage.StorageConfig;
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

    private final String dataFilename;
    private final String endpoint;
    private final String accessKey;
    private final String secretKey;

    public MinioStorage(MinioConfig config, StorageConfig storageConfig) {
        LOG.info("Starting MinIO storage consumer");
        if (config.bucketName().isEmpty()) {
            throw new IllegalArgumentException("Missing MinIO bucket");
        } else {
            this.bucketName = config.bucketName().get();
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
        this.dataFilename = storageConfig.dataFilename();

        LOG.info("MinIO data location: endpoint=" + config.endpoint() + ", bucket=" + bucketName + ", data file="
                + this.dataFilename);
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
    public ByteBuffer readData(String modelId) throws StorageReadException {
        isObjectAvailable(this.bucketName, this.dataFilename);
        try {
            return ByteBuffer.wrap(readFile(this.bucketName, this.dataFilename));
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            LOG.error("Error reading input file");
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
    public boolean dataExists(String modelId) throws StorageReadException {
        try {
            isObjectAvailable(this.bucketName, this.dataFilename);
            return true;
        } catch (StorageReadException e) {
            return false;
        }
    }

    @Override
    public void save(ByteBuffer data, String location) throws StorageWriteException {
        saveData(data, this.bucketName, location);
    }

    @Override
    public void append(ByteBuffer data, String location) throws StorageWriteException {
        appendData(data, this.bucketName, location);
    }

    @Override
    public void appendData(ByteBuffer data, String modelId) throws StorageWriteException {
        append(data, this.dataFilename);
    }

    @Override
    public ByteBuffer read(String location) throws StorageReadException {
        try {
            return ByteBuffer.wrap(readFile(this.bucketName, location));
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new StorageReadException("Could not read file: " + e.getMessage());
        }
    }

    @Override
    public void saveData(ByteBuffer data, String modelId) throws StorageWriteException {
        save(data, this.dataFilename);
    }

    @Override
    public boolean fileExists(String location) throws StorageReadException {
        try {
            isObjectAvailable(this.bucketName, location);
            return true;
        } catch (StorageReadException e) {
            return false;
        }
    }

    @Override
    public String getDataFilename(String modelId) {
        return this.dataFilename;
    }

    @Override
    public Path buildDataPath(String modelId) {
        return Path.of(this.bucketName, getDataFilename(modelId));
    }

    @Override
    public long getLastModified(final String modelId) {
        try {
            return isObjectAvailable(bucketName, getDataFilename(modelId)).lastModified().toInstant().toEpochMilli();
        } catch (StorageReadException e) {
            return new Random().nextLong();
        }
    }
}