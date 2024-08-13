package com.woowacamp.storage.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import jakarta.servlet.MultipartConfigElement;

@Configuration
public class MultipartConfiguration {

	@Value("${file.multipart.maxUploadSize:10485760}")
	private long maxUploadSize;

	@Value("${file.multipart.maxUploadSizePerFile:10485760}")
	private long maxUploadSizePerFile;

	@Bean
	public MultipartResolver multipartResolver() {
		StandardServletMultipartResolver standardServletMultipartResolver = new StandardServletMultipartResolver();
		standardServletMultipartResolver.setResolveLazily(true);
		return standardServletMultipartResolver;
	}

	@Bean
	public MultipartConfigElement multipartConfigElement() {
		MultipartConfigFactory factory = new MultipartConfigFactory();
		factory.setMaxRequestSize(DataSize.ofBytes(maxUploadSize * 1000));
		factory.setMaxFileSize(DataSize.ofBytes(maxUploadSizePerFile * 1000));
		// factory.setLocation("/Users/yoonjungjin/Downloads");

		return factory.createMultipartConfig();
	}
}