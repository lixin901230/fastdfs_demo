package com.lx.fastdfs.fastdfs;

import org.csource.fastdfs.FileInfo;
import org.junit.Test;

import com.lx.fastdfs.util.FileUtils;

public class MultiTreadDownloadTest {

	
	@Test
	public void testDownloadByMultiThread() {
		String fileSavePath = FileUtils.getWebappPath()+"incoming";
		String groupName = "group1";
		String remoteFileName = "M00/00/00/wKgAllfVgn-EW1h9AAAAAAAAAAA319.zip";
		FileInfo fileInfo = FastDFSFileManager.getFileInfo(groupName, remoteFileName);
		long fileSize = fileInfo.getFileSize();
		try {
			MultiThreadDownlad multiThreadDownlad = new MultiThreadDownlad();
			multiThreadDownlad.downloadByMultiThread(groupName, remoteFileName, fileSize, fileSavePath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
