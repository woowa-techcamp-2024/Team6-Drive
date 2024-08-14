package com.woowacamp.storage.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.amazonaws.services.s3.AmazonS3;


@Configuration
@Profile("test")  // 이 설정은 테스트 프로파일에서만 적용됩니다.
public class S3MockConfig {
	@Bean
	AmazonS3 s3Client() {
		return Mockito.mock(AmazonS3.class);
	}
}
