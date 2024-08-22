package com.woowacamp.storage.domain.file.service;

import static com.woowacamp.storage.global.constant.CommonConstant.*;
import static java.awt.image.BufferedImage.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.woowacamp.storage.domain.file.dto.UploadContext;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ThumbnailWriterThreadPool {

	private final AmazonS3 amazonS3;
	private final ExecutorService executorService;

	@Value("${cloud.aws.credentials.bucketName}")
	private String BUCKET_NAME;

	public ThumbnailWriterThreadPool(AmazonS3 amazonS3) {
		this.amazonS3 = amazonS3;
		executorService = new ThreadPoolExecutor(
			THUMBNAIL_WRITER_CORE_POOL_SIZE,
			THUMBNAIL_WRITER_MAXIMUM_POOL_SIZE, // TODO: 톰캣 커넥션 풀 스레드 개수랑 맞춰야함
			THUMBNAIL_WRITER_KEEP_ALIVE_TIME,
			TimeUnit.SECONDS,
			new SynchronousQueue<>()
		);
	}

	public void createThumbnail(UploadContext context) {
		executorService.execute(() -> {
			ImageReader reader = null;
			try (ImageInputStream iis = ImageIO.createImageInputStream(context.getPis());
				 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
				if (!iter.hasNext()) {
					throw new RuntimeException("No image readers found");
				}
				reader = iter.next();
				reader.setInput(iis, true, true);

				// 썸네일은 1MB 고정
				int width = reader.getWidth(0);
				int height = reader.getHeight(0);
				double rate = (double)width / height;

				int thumbnailSize = THUMBNAIL_SIZE / 3;
				int thumbnailHeight = (int)Math.sqrt(thumbnailSize / rate);
				int thumbnailWidth = (int)(thumbnailHeight * rate);

				// 썸네일 비율 계산
				double scale = (double)thumbnailWidth / width;
				// ImageReadParam 설정
				ImageReadParam param = reader.getDefaultReadParam();
				param.setSourceSubsampling((int)(1 / scale), (int)(1 / scale), 0, 0);
				// 썸네일 이미지 읽기
				BufferedImage thumbnailImage = reader.read(0, param);
				// 정확한 크기로 조정
				BufferedImage finalThumbnail = new BufferedImage(thumbnailWidth, thumbnailHeight, TYPE_INT_RGB);
				Graphics2D g2d = finalThumbnail.createGraphics();
				g2d.drawImage(thumbnailImage, 0, 0, thumbnailWidth, thumbnailHeight, null);
				g2d.dispose();

				// finalThumbnail에 imageFormat 형태로 write
				ImageIO.write(finalThumbnail, context.getImageFormat(), baos);
				ObjectMetadata metadata = new ObjectMetadata();
				metadata.setContentType("image/" + context.getImageFormat());
				byte[] byteArray = baos.toByteArray();
				metadata.setContentLength(byteArray.length);
				amazonS3.putObject(BUCKET_NAME, context.getFileMetadata().thumbnailUUID(),
					new ByteArrayInputStream(byteArray), metadata);
			} catch (RuntimeException | IOException e) {
				context.abortCreateThumbnail();
				try {
					context.getPis().close();
					context.getPos().close();
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			} finally {
				if (reader != null) {
					reader.dispose();
				}
			}
		});
	}
}
