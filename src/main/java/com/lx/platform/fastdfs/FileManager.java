package com.lx.platform.fastdfs;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.lx.platform.org.csource.common.NameValuePair;
import com.lx.platform.org.csource.fastdfs.ClientGlobal;
import com.lx.platform.org.csource.fastdfs.FileInfo;
import com.lx.platform.org.csource.fastdfs.ServerInfo;
import com.lx.platform.org.csource.fastdfs.StorageClient;
import com.lx.platform.org.csource.fastdfs.StorageServer;
import com.lx.platform.org.csource.fastdfs.TrackerClient;
import com.lx.platform.org.csource.fastdfs.TrackerServer;
import com.lx.platform.util.FileUtils;

/**
 * 调用FDFS提供的java客户端接口管理FDFS服务器上的文件
 */
public class FileManager {
	
	private static Logger logger  = Logger.getLogger(FileManager.class);
	  
	private static TrackerClient  trackerClient;
	private static TrackerServer  trackerServer;
	private static StorageServer  storageServer;
	private static StorageClient  storageClient;
	
	// 初始化 FastDFS 客户端配置信息
	static {
		try {
			String classPath = new File(FileManager.class.getResource("/").getFile()).getCanonicalPath();
			String fdfsClientConfigFilePath = classPath + File.separator + FileManagerConfig.CLIENT_CONFIG_FILE;
			
			logger.info("FastDFS 配置文件路径:"+ fdfsClientConfigFilePath);
			ClientGlobal.init(fdfsClientConfigFilePath);
			
			trackerClient = new TrackerClient();
			trackerServer = trackerClient.getConnection();
			
			storageClient = new StorageClient(trackerServer, storageServer);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 上传文件
	 * @param file
	 * @return
	 */
	public static String upload(FastDFSFile file) {
		
		NameValuePair[] meta_list = new NameValuePair[3];
		meta_list[0] = new NameValuePair("width", "120");
		meta_list[1] = new NameValuePair("heigth", "120");
		meta_list[2] = new NameValuePair("author", "lx");

		long startTime = System.currentTimeMillis();
		String[] uploadResults = null;
		try {
			uploadResults = storageClient.upload_file(file.getContent(), file.getExt(), meta_list);
		} catch (IOException e) {
			logger.error("上传文件过程中IO异常: " + file.getName(), e);
		} catch (Exception e) {
			logger.error("上传文件过程中出现未知异常: "+ file.getName(), e);
		}
		logger.info(">>>>>>upload_file 耗时: "+ new DecimalFormat("0.00").format((System.currentTimeMillis() - startTime)/1000.0) + " 秒");
		
		String fileAbsolutePath = null;
		if (uploadResults == null) {
			 logger.error("上传文件失败, error code: "+ storageClient.getErrorCode());
		} else {

			String groupName = uploadResults[0];
			String remoteFileName = uploadResults[1];
	
			fileAbsolutePath = FileManagerConfig.PROTOCOL
					+ trackerServer.getInetSocketAddress().getHostName()
					+ FileManagerConfig.SEPARATOR
					+ FileManagerConfig.TRACKER_NGNIX_PORT
					+ FileManagerConfig.SEPARATOR + groupName
					+ FileManagerConfig.SEPARATOR + remoteFileName;
	
			logger.info(">>>>>>文件上传成功！group_name：" + groupName + "，remoteFileName："+ " " + remoteFileName);
		}
		return fileAbsolutePath;
	}
	
	/**
	 * 下载文件，返回二进制文件字节码
	 * @param groupName
	 * @param remoteFileName
	 * @return byte[] 文件内容字节数组
	 */
	public static byte[] download(String groupName, String remoteFileName) {
		try {
			byte[] bytes = storageClient.download_file(groupName, remoteFileName);
			return bytes;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 下载文件到本地
	 * @param groupName
	 * @param remoteFileName
	 * @param downloadFileSavePath		文件下载存储绝对路径
	 * @return byte[] 文件内容字节数组
	 */
	public static void download(String groupName, String remoteFileName, String downloadFileSavePath) {
		try {
			if(StringUtils.isEmpty(downloadFileSavePath)) {
				downloadFileSavePath = FileUtils.getWebappPath()+ FileUtils.INCOMING +"/"+ remoteFileName;
			}
			// 1、从fdfs存储服务器上下载获取二进制文件
			byte[] bytes = storageClient.download_file(groupName, remoteFileName);
			
			// 2、将获取到的二进制文件下载到本地
			FileUtils.download(bytes, downloadFileSavePath);
			logger.info("文件下载完成，下载文件保存地址："+ downloadFileSavePath);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 根据存储服务器组名 和 文件名 获取文件信息
	 * @param groupName		存储服务器组名
	 * @param remoteFileName	文件上传到存储服务器上后的文件名
	 * @return
	 */
	public static FileInfo getFile(String groupName, String remoteFileName) {
		try {
			return storageClient.get_file_info(groupName, remoteFileName);
		} catch (IOException e) {
			logger.error("IO Exception: Get File from Fast DFS failed", e);
		} catch (Exception e) {
			logger.error("Non IO Exception: Get File from Fast DFS failed", e);
		}
		return null;
	}
	
	
	/**
	 * 删除文件
	 * @param groupName		存储服务器所在的组名
	 * @param remoteFileName	文件上传到存储服务器上后的文件名
	 * @throws Exception
	 */
	public static void deleteFile(String groupName, String remoteFileName) throws Exception {
		storageClient.delete_file(groupName, remoteFileName);
	}

	/**
	 * 根据存储服务节点组名获取该组下的所有存储节点服务器信息
	 * @param groupName		存储服务器所在的组名
	 * @return
	 * @throws IOException
	 */
	public static StorageServer[] getStoreStorages(String groupName) throws IOException {
		return trackerClient.getStoreStorages(trackerServer, groupName);
	}

	/**
	 * 根据上传到存储服务器上的文件名 和 存储服务器组名 获取该文件所在的服务器节点信息
	 * @param groupName		存储服务器组名
	 * @param remoteFileName	文件上传到存储服务器上后的文件名
	 * @return
	 * @throws IOException
	 */
	public static ServerInfo[] getFetchStorages(String groupName, String remoteFileName) throws IOException {
		return trackerClient.getFetchStorages(trackerServer, groupName, remoteFileName);
	}
}
