package io.github.benxincaomu.oa.minio;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.SnowballObject;
import io.minio.UploadSnowballObjectsArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import jakarta.annotation.Resource;

@SpringBootTest
public class MinioTest {

    @Resource
    private MinioClient minioClient;

    private String bucketName = "oafile";

    private Logger logger = LoggerFactory.getLogger(MinioTest.class);

    @Test
    public void testUpload() throws InvalidKeyException, ErrorResponseException, InsufficientDataException, InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException, XmlParserException, IllegalArgumentException, IOException {
        String filePath = "/home/sunft/Pictures/pic1.png";
        File file = new File(filePath);
        ZonedDateTime zonedDateTime = ZonedDateTime.now();
        SnowballObject snowballObject = new SnowballObject("oa/fileupload/pic2.png",new FileInputStream(file),file.length(),zonedDateTime);
        List<SnowballObject> snowballObjects = List.of(snowballObject);
        ObjectWriteResponse resp = minioClient.uploadSnowballObjects(UploadSnowballObjectsArgs.builder().bucket(bucketName).compression(true).objects(snowballObjects).build());
        logger.info(resp.versionId());
    }
}
