package io.github.benxincaomu.oa.bussiness.files;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.github.benxincaomu.notry.utils.Asserts;
import io.github.benxincaomu.oa.base.minio.MinioProperties;
import io.github.benxincaomu.oa.base.web.OaResponseCode;
import io.minio.MinioClient;
import io.minio.SnowballObject;
import io.minio.UploadSnowballObjectsArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import jakarta.annotation.Resource;

@RestController
@RequestMapping("file")
public class FileController {
    @Resource
    private MinioClient minioClient;

    @Resource
    private MinioProperties minioProperties;

    @Value("${minio.static-url}")
    private String staticPath;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @PostMapping("upload")
    public String upload(@RequestParam("file") MultipartFile file) {

        Asserts.isFalse(file.isEmpty(), OaResponseCode.UPLOAD_FILE_NOT_EXIST);
        ZonedDateTime zonedDateTime = ZonedDateTime.now();
        String originalFilename = file.getOriginalFilename();
        String fileName = UUID.randomUUID().toString()+originalFilename.substring(originalFilename.lastIndexOf("."));
        try {
            SnowballObject object = new SnowballObject(fileName, file.getInputStream(),file.getSize(), zonedDateTime);
            List<SnowballObject> snowballObjects = List.of(object);
            minioClient.uploadSnowballObjects(UploadSnowballObjectsArgs.builder().bucket(minioProperties.getBucketName()).compression(true).objects(snowballObjects).build());
            return fileName;
        } catch (IOException | InvalidKeyException | ErrorResponseException | InsufficientDataException | InternalException | InvalidResponseException | NoSuchAlgorithmException | ServerException | XmlParserException | IllegalArgumentException e) {
            logger.error("上传文件失败", e);
        }

        return null;
    }

    @GetMapping("baseUrl")
    public String getBaseUrl() {
        return staticPath;

    }
}
