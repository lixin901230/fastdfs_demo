package com.lx.platform.fastdfs;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Map;

import org.apache.log4j.Logger;

import com.lx.platform.org.csource.common.NameValuePair;
import com.lx.platform.org.csource.fastdfs.ClientGlobal;
import com.lx.platform.org.csource.fastdfs.FileInfo;
import com.lx.platform.org.csource.fastdfs.ServerInfo;
import com.lx.platform.org.csource.fastdfs.StorageClient;
import com.lx.platform.org.csource.fastdfs.StorageServer;
import com.lx.platform.org.csource.fastdfs.TrackerClient;
import com.lx.platform.org.csource.fastdfs.TrackerServer;

/**
 * 调用FDFS提供的java客户端接口管理FDFS服务器上的文件
 */
public class FastDFSFileManager {
	
	private static Logger logger  = Logger.getLogger(FastDFSFileManager.class);
	  
	private static TrackerClient  trackerClient;
	private static TrackerServer  trackerServer;
	private static StorageServer  storageServer;
	private static StorageClient  storageClient;
	
	// 初始化 FastDFS 客户端配置信息
	static {
		try {
			String classPath = new File(FastDFSFileManager.class.getResource("/").getFile()).getCanonicalPath();
			String fdfsClientConfigFilePath = classPath + File.separator + FastDFSFileManagerConfig.CLIENT_CONFIG_FILE;
			
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
	 * 得到文件附加属性名称-值对数组
	 * @param nameValuePairMap	文件附加属性名称-值对Map集合
	 * @return NameValuePair[]	文件附加属性名称-值对对象数组
	 */
	public static NameValuePair[] getNameValuePair(Map<String, String> nameValuePairMap) throws Exception {
		
		if(nameValuePairMap != null && nameValuePairMap.size() > 0) {
			NameValuePair[] meta_list = new NameValuePair[nameValuePairMap.size()];
			if(nameValuePairMap != null && nameValuePairMap.size() > 0) {
				Object[] keys = nameValuePairMap.keySet().toArray();
				for (int i = 0; i < keys.length; i++) {
					meta_list[i] = new NameValuePair(String.valueOf(keys[i]), nameValuePairMap.get(keys[i]));
				}
			}
			return meta_list;
		}
		return null;
	}

	/**
	 * 上传文件——普通上传
	 * @param file
	 * @return
	 */
	public static String upload(FastDFSFile file) {
		
		long startTime = System.currentTimeMillis();
		
		try {
			if(file == null) {
				throw new Exception("上传的文件对象不能为空！");
			}
			
			// 获取文件附加属性
			NameValuePair[] meta_list = getNameValuePair(file.getNameValuePairMap());
			
			// 上传文件
			String[] uploadResults = storageClient.upload_file(file.getContent(), file.getExt(), meta_list);
			
			// 处理上传文件返回的文件信息
			if (uploadResults != null) {
				
				String groupName = uploadResults[0];
				String remoteFileName = uploadResults[1];
				
				String fileAbsolutePath = FastDFSFileManagerConfig.PROTOCOL
						+ trackerServer.getInetSocketAddress().getHostName()
						+ FastDFSFileManagerConfig.SEPARATOR
						+ FastDFSFileManagerConfig.TRACKER_NGNIX_PORT
						+ FastDFSFileManagerConfig.SEPARATOR + groupName
						+ FastDFSFileManagerConfig.SEPARATOR + remoteFileName;
				
				logger.info(">>>>>>文件上传成功！group_name：" + groupName + "，remoteFileName："+ " " + remoteFileName);
				logger.info(">>>>>>upload_file 耗时: "+ new DecimalFormat("0.00").format((System.currentTimeMillis() - startTime) / 1000.0) + " 秒");
				
				return fileAbsolutePath;
				
			} else {
				logger.error("上传文件失败, error code: "+ storageClient.getErrorCode());
			}
		} catch (Exception e) {
			logger.error("上传文件异常，文件名: "+ file.getName(), e);
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 文件上传——断点续传
	 * @param file
	 * @return
	 */
	public static String uploadAppend(FastDFSFile file) {
		
		try {
			// 获取文件附加属性
			NameValuePair[] meta_list = getNameValuePair(file.getNameValuePairMap());
			
			String[] uploadResults = storageClient.upload_appender_file(file.getContent(), file.getExt(), meta_list);
			
			if(uploadResults != null) {
				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 下载文件，返回二进制文件字节码
	 * @param groupName
	 * @param remoteFileName
	 * @return byte[] 文件内容字节数组
	 */
	public static byte[] download(String groupName, String remoteFileName) {
		try {
			return storageClient.download_file(groupName, remoteFileName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 删除文件
	 * @param groupName		存储服务器所在的组名
	 * @param remoteFileName	文件上传到存储服务器上后的文件名
	 * @return	0：成功；非0：失败
	 */
	public static int deleteFile(String groupName, String remoteFileName) {
		int success = -1;
		try {
			success = storageClient.delete_file(groupName, remoteFileName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return success;
	}
	
	/**
	 * 根据存储服务器组名 和 文件名 获取文件信息
	 * @param groupName		存储服务器组名
	 * @param remoteFileName	文件上传到存储服务器上后的文件名
	 * @return FileInfo
	 */
	public static FileInfo getFileInfo(String groupName, String remoteFileName) {
		try {
			return storageClient.get_file_info(groupName, remoteFileName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 根据存储服务节点组名获取该组下的所有存储节点服务器信息
	 * @param groupName		存储服务器所在的组名
	 * @return
	 */
	public static StorageServer[] getStoreStorages(String groupName) {
		try {
			return trackerClient.getStoreStorages(trackerServer, groupName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 根据上传到存储服务器上的文件名 和 存储服务器组名 获取该文件所在的服务器节点信息
	 * @param groupName		存储服务器组名
	 * @param remoteFileName	文件上传到存储服务器上后的文件名
	 * @return
	 * @throws IOException
	 */
	public static ServerInfo[] getFetchStorages(String groupName, String remoteFileName) {
		try {
			return trackerClient.getFetchStorages(trackerServer, groupName, remoteFileName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
