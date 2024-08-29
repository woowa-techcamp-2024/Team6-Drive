package com.woowacamp.storage.domain.file.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import lombok.extern.slf4j.Slf4j;

/**
 * S3에 파일 쓰기 작업을 하는
 */
@Slf4j
public class CustomS3BlockingQueuePolicy implements RejectedExecutionHandler {

	@Override
	public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
		BlockingQueue<Runnable> queue = executor.getQueue();
		try {
			// 큐에 공간이 생길 때까지 무기한 대기
			log.info("current thread {} is blocked", Thread.currentThread().getName());
			queue.put(runnable);
		} catch (InterruptedException e) {
			// 인터럽트 발생 시 현재 스레드의 인터럽트 상태를 설정
			Thread.currentThread().interrupt();
			throw new RejectedExecutionException("Task interrupted while waiting for queue space", e);
		}
	}
}
