package com.woowacamp.storage.global.constant;

import java.time.LocalDateTime;

public class CommonConstant {
	public static final Character[] FILE_NAME_BLACK_LIST = {'\\', '/', ':', '*', '?', '"', '<', '>', '|'};
	public static final int MAX_FOLDER_DEPTH = 50;
	public static final int FILE_WRITER_CORE_POOL_SIZE = 20;
	public static final int FILE_WRITER_MAXIMUM_POOL_SIZE = 40;
	public static final int FILE_WRITER_KEEP_ALIVE_TIME = 10;
	public static final int FILE_WRITER_QUEUE_SIZE = 400;
	public static final int ORPHAN_PARENT_ID = -1;
	public static final int SHARED_LINK_VALID_TIME = 3 * 60 * 60;
	public static final LocalDateTime UNAVAILABLE_TIME = LocalDateTime.of(1970, 1, 1, 1, 0);
	public static final String SHARED_LINK_URI = "/api/v1/share?sharedId=";
	public static final String FOLDER_READ_URI_TEMPLATE = "/api/v1/folders/%d";
	public static final String FILE_READ_URI_TEMPLATE = "/api/v1/files/%d";
}
