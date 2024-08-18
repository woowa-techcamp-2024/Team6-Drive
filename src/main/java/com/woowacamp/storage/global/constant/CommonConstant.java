package com.woowacamp.storage.global.constant;

public class CommonConstant {
	public static final Character[] FILE_NAME_BLACK_LIST = {'\\', '/', ':', '*', '?', '"', '<', '>', '|'};
	public static final int MAX_FOLDER_DEPTH = 50;
	public static final int FILE_WRITER_CORE_POOL_SIZE = 20;
	public static final int FILE_WRITER_MAXIMUM_POOL_SIZE = 40;
	public static final int FILE_WRITER_KEEP_ALIVE_TIME = 10;
	public static final int FILE_WRITER_QUEUE_SIZE = 400;
	public static final int ORPHAN_PARENT_ID = -1;
}
