package io.github.benxincaomu.oa.base.minio;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.MinioClient;
import jakarta.annotation.Resource;

@Configuration
public class MinioConfiguration {

    @Resource
    private MinioProperties minioProperties;


    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }

}
