package com.lx.fastdfs.fastdfs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.csource.common.NameValuePair;
import org.csource.fastdfs.FileInfo;
import org.csource.fastdfs.ServerInfo;
import org.csource.fastdfs.StorageServer;
import org.junit.Test;
import org.springframework.util.Assert;

import com.lx.fastdfs.util.FileUtils;

/**
 * 测试 上传文件到FastDFS、获取FastDFS服务中的文件信息 
 */
public class FastDFSFileManagerTest {
	
	/**
	 * 上传文件到fdfs存储节点服务器中，并返回存储节点组名 和 远程文件名称
	 * 普通上传
	 * @throws Exception
	 */
	@Test
	public void upload() throws Exception {
		
		String filePath = "D:\\Workspaces\\eclipse-jee-luna-SR2\\workspace1\\fastdfs_demo\\src\\main\\webapp\\incoming\\美女.jpg";;
		File content = new File(filePath);
		
		FileInputStream fis = new FileInputStream(content);
		byte[] file_buff = null;
		if (fis != null) {
			int len = fis.available();
			file_buff = new byte[len];
			fis.read(file_buff);
		}
		
		// 文件附加属性
		Map<String, String> fileAttachAttrMap = new LinkedHashMap<String, String>();
		fileAttachAttrMap.put("width", "1400");
		fileAttachAttrMap.put("height", "900");
		fileAttachAttrMap.put("author", "lx");
		
		FastDFSFile file = new FastDFSFile("美女", file_buff, "jpg", fileAttachAttrMap);

		String fileAbsolutePath = FastDFSFileManager.upload(file);
		/**
		 * 返回地址：http://192.168.0.108:8080/group1/M00/00/00/wKgAbFeiIv-AcGiBAAMhq9w8VNM100.jpg，
		 * 由于使用nginx监听80端口，故根据地址：http://192.168.0.108/group1/M00/00/00/wKgAbFeiIv-AcGiBAAMhq9w8VNM100.jpg 即可访问到FDFS上的文件
		 */
		System.out.println(fileAbsolutePath);
		fis.close();
	}

	/**
	 * 测试普通下载
	 */
	@Test
	public void testDownload() {
		try {
			String fileSavePath = FileUtils.getWebappPath()+"incoming/wKgAllfVdD2AYp5tAAMhq9w8VNM087.jpg";

			// 获取文件二进制字节码
			byte[] bytes = FastDFSFileManager.download("group1", "M00/00/00/wKgAllfVdD2AYp5tAAMhq9w8VNM087.jpg");
			FileUtils.download(bytes, fileSavePath);
			
			System.out.println("下载文件："+ fileSavePath);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 测试断点续传
	 */
	@Test
	public void testUploadAppend() {
		
		String localFilePath = "C:\\Users\\Administrator\\Desktop\\lucene-5.4.0.zip";
		String fileId = "";
		fileId = "group1/M00/00/00/wKgAllfVgn-EW1h9AAAAAAAAAAA319.zip";
		String url = FastDFSFileManager.uploadAppend(localFilePath, fileId, new NameValuePair[]{});
		System.out.println(url);
	}
	
	/**
	 * 测试断点下载
	 */
	@Test
	public void testDownloadAppend() {
		try {
			String fileSavePath = FileUtils.getWebappPath()+"incoming/wKgAllfVgn-EW1h9AAAAAAAAAAA319.zip";
			
			String groupName = "group1";
			String remoteFileName = "M00/00/00/wKgAllfVgn-EW1h9AAAAAAAAAAA319.zip";
			
			// 普通下载：获取文件二进制字节码
			/*byte[] bytes = FastDFSFileManager.download(groupName, remoteFileName);
			FileUtils.download(bytes, fileSavePath);*/
			
			// 断点下载1：
			/*OutputStream out = new FileOutputStream(fileSavePath);
			int state = FastDFSFileManager.downloadAppend(groupName, remoteFileName, 0, out);
			if(state == 0) {
				System.out.println("下载完毕");
			}*/
			
			// 断点下载2：？？？？
			/*int state2 = FastDFSFileManager.downloadAppend(groupName, remoteFileName, fileSavePath);
			if(state2 == 0) {
				System.out.println("下载完毕");
			}*/
			
			// 断点下载3：
			long length = 0;	//默认已经下载到本地的文件大小
			File downloadedFile = new File(fileSavePath);
			if(downloadedFile.exists()) {	// 如果本地已经下载过该文件，但未下完，则获取该文件大小，并在本次下载时跳过已下载部分接着下载
				length = downloadedFile.length();
			}
			int state3 = FastDFSFileManager.downloadAppend(groupName, remoteFileName, length, fileSavePath);
			if(state3 == 0) {
				System.out.println("下载完毕");
			}
			
			System.out.println("下载文件："+ fileSavePath);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 大文件分段多线程下载，将一个文件分成多个片段下载
	 */
	@Test
	public void testDownloadBySegment() {
		try {
			String fileSavePath = FileUtils.getWebappPath()+"incoming/wKgAllfVgn-EW1h9AAAAAAAAAAA319.zip";
			
			String groupName = "group1";
			String remoteFileName = "M00/00/00/wKgAllfVgn-EW1h9AAAAAAAAAAA319.zip";
			
			// 将大文件分割下载成20M大小的小文件
			long segmentSize = 20 * 1024 * 1024;	//切片分段下载时每个分段文件的大小，1：设置为一个正整数表示下载的每个分片文件大小为该值，2：设置为0表示从开始位置一直到文件结尾
			long completeFlag = FastDFSFileManager.downloadBySegment(groupName, remoteFileName, segmentSize, fileSavePath);
			if(completeFlag == 0) {
				System.out.println("下载成功");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 测试合并下载的多个分段文件
	 */
	@Test
	public void testMergeSegmentFile() {
		String dirPath = FileUtils.getWebappPath()+"incoming";
		FastDFSFileManager fileManager = new FastDFSFileManager();
		try {
			fileManager.mergeSegmentFile(dirPath, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 测试根据存储节点服务器返回的 存储节点组名 和 远程文件名 获取fdfs服务中的
	 * @throws Exception
	 */
	@Test
	public void getFile() throws Exception {
		FileInfo file = FastDFSFileManager.getFileInfo("group1", "M00/00/00/wKgAllfVdD2AYp5tAAMhq9w8VNM087.jpg");
		Assert.notNull(file);
		String sourceIpAddr = file.getSourceIpAddr();
		long size = file.getFileSize();
		System.out.println("ip:" + sourceIpAddr + ",size:" + size);
		System.out.println("文件访问地址："+"http://"+sourceIpAddr+"/group1/M00/00/00/wKgAllfVdD2AYp5tAAMhq9w8VNM087.jpg");
	}

	/**
	 * 获取存储节点服务信息
	 * @throws Exception
	 */
	@Test
	public void getStorageServer() throws Exception {
		StorageServer[] ss = FastDFSFileManager.getStoreStorages("group1");
		Assert.notNull(ss);

		for (int k = 0; k < ss.length; k++) {
			System.out.println(k
					+ 1
					+ ". "
					+ ss[k].getInetSocketAddress().getAddress()
							.getHostAddress() + ":"
					+ ss[k].getInetSocketAddress().getPort());
		}
	}

	@Test
	public void getFetchStorages() throws Exception {
		ServerInfo[] servers = FastDFSFileManager.getFetchStorages("group1",
				"M00/00/00/wKgAllfVdD2AYp5tAAMhq9w8VNM087.jpg");
		Assert.notNull(servers);

		for (int k = 0; k < servers.length; k++) {
			System.out.println(k + 1 + ". " + servers[k].getIpAddr() + ":"
					+ servers[k].getPort());
		}
	}
}
