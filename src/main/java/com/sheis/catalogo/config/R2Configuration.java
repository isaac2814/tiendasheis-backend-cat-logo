package com.sheis.catalogo.config;

import com.sheis.catalogo.service.media.MediaStorage;
import com.sheis.catalogo.service.media.NoopMediaStorage;
import com.sheis.catalogo.service.media.R2MediaStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@Slf4j
public class R2Configuration {

    @Bean
    @ConditionalOnProperty(name = "app.r2.enabled", havingValue = "true")
    public S3Client s3Client(AppProperties appProperties) {
        AppProperties.R2 r2 = appProperties.getR2();
        log.info("Inicializando S3Client para Cloudflare R2 (account: {}, bucket: {})",
                r2.getAccountId(), r2.getBucket());

        URI endpoint = URI.create("https://" + r2.getAccountId() + ".r2.cloudflarestorage.com");
        AwsBasicCredentials credentials = AwsBasicCredentials.create(r2.getAccessKeyId(), r2.getSecretAccessKey());

        return S3Client.builder()
                .endpointOverride(endpoint)
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.r2.enabled", havingValue = "true")
    public S3Presigner s3Presigner(AppProperties appProperties) {
        AppProperties.R2 r2 = appProperties.getR2();
        URI endpoint = URI.create("https://" + r2.getAccountId() + ".r2.cloudflarestorage.com");
        AwsBasicCredentials credentials = AwsBasicCredentials.create(r2.getAccessKeyId(), r2.getSecretAccessKey());

        return S3Presigner.builder()
                .endpointOverride(endpoint)
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.r2.enabled", havingValue = "true")
    public MediaStorage r2MediaStorage(S3Client s3Client, S3Presigner s3Presigner, AppProperties appProperties) {
        return new R2MediaStorage(s3Client, s3Presigner, appProperties.getR2());
    }

    @Bean
    @ConditionalOnMissingBean(MediaStorage.class)
    public MediaStorage noopMediaStorage() {
        log.info("R2 deshabilitado o no configurado: utilizando NoopMediaStorage");
        return new NoopMediaStorage();
    }
}
