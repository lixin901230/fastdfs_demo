package com.lx.fastdfs.fastdfs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.csource.common.IniFileReader;
import org.csource.common.NameValuePair;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.DownloadStream;
import org.csource.fastdfs.FileInfo;
import org.csource.fastdfs.ServerInfo;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.StorageClient1;
import org.csource.fastdfs.StorageServer;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.csource.fastdfs.test.DownloadFileWriter;


/**
 * 调用FDFS提供的java客户端接口管理FDFS服务器上的文件
 */
public class FastDFSFileManager {
	
	private static Logger logger  = Logger.getLogger(FastDFSFileManager.class);
	
	public static final String PROTOCOL = "http://";
	public static final String SEPARATOR = "/";
	public static final String TRACKER_NGNIX_PORT = "8080";
	public static final String CLIENT_CONFIG_FILE = "fdfs_client.conf";
	
	private static final int MIN_BUFF_SIZE = 2;			// 最小上传、下载缓冲区单位大小；1024的2倍
	private static final int MAX_BUFF_SIZE = 20;		// 最大上传、下载缓冲区单位大小；1024的20倍
	private static final int DEFAULT_BUFF_SIZE = 5;		// 默认上传、下载缓冲区单位大小；1024的5倍
	private static int g_buff_size_unit;				// 配置文件中配置的上传、下载文件缓冲区大小单位
	
	private static int MIN_DOWNLOAD_FAIL_RETRY_NUM = 0;		// 下载失败时，最小重试次数
	private static int MAX_DOWNLOAD_FAIL_RETRY_NUM = 10;	// 下载失败时，最大重试次数
	private static int DEFAULT_DOWNLOAD_FAIL_RETRY_NUM = 5;	// 下载失败时，默认重试次数
	private static int g_download_fail_retry_num;			// 下载失败时，默认重试次数
	
	private static TrackerClient  trackerClient;
	private static TrackerServer  trackerServer;
	private static StorageServer  storageServer;
	private static StorageClient  storageClient;
	
	
	// 初始化 FastDFS 客户端配置信息
	private static String fdfsClientConfPath = "";
	static {
		try {
			String classPath = new File(FastDFSFileManager.class.getResource("/").getFile()).getCanonicalPath();
			fdfsClientConfPath = classPath + File.separator + FastDFSFileManager.CLIENT_CONFIG_FILE;
			
			logger.info("FastDFS 配置文件路径:"+ fdfsClientConfPath);
			ClientGlobal.init(fdfsClientConfPath);
			
			trackerClient = new TrackerClient();
			trackerServer = trackerClient.getConnection();
			
			storageClient = new StorageClient(trackerServer, storageServer);
			
			// 获取上传文件缓冲区单位大小
			getUploadBuffSizeUnit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 得到配置的上传文件缓冲区单位大小配置
	 * @return
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	private static void getUploadBuffSizeUnit() throws FileNotFoundException, IOException {
		
		IniFileReader iniReader = new IniFileReader(fdfsClientConfPath);
		
		// 获取上传、下载缓冲区单位大小（用于乘以1024的整数倍倍数）
		g_buff_size_unit = iniReader.getIntValue("buff_size_unit", DEFAULT_BUFF_SIZE);
		if(g_buff_size_unit < MIN_BUFF_SIZE) {
			g_buff_size_unit = MIN_BUFF_SIZE;
		} else if(g_buff_size_unit > MAX_BUFF_SIZE) {
			g_buff_size_unit = MAX_BUFF_SIZE;
		}
		
		// 获取下载失败重试次数配置，未配置则启用默认配置
		g_download_fail_retry_num = iniReader.getIntValue("download_fail_retry_num", DEFAULT_DOWNLOAD_FAIL_RETRY_NUM);
		if(g_download_fail_retry_num < MIN_DOWNLOAD_FAIL_RETRY_NUM) {
			g_download_fail_retry_num = MIN_DOWNLOAD_FAIL_RETRY_NUM;
		} else if(g_download_fail_retry_num > MAX_DOWNLOAD_FAIL_RETRY_NUM) {
			g_download_fail_retry_num = MAX_DOWNLOAD_FAIL_RETRY_NUM;
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
				
				String fileAbsolutePath = FastDFSFileManager.PROTOCOL
						+ trackerServer.getInetSocketAddress().getHostName()
						+ FastDFSFileManager.SEPARATOR
						//+ FastDFSFileManager.TRACKER_NGNIX_PORT
						+ FastDFSFileManager.SEPARATOR + groupName
						+ FastDFSFileManager.SEPARATOR + remoteFileName;
				
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
	 * 获取文件扩展名
	 * @param fileName	文件名
	 * @return
	 * @throws Exception 
	 */
	public static String getFileExtName(String fileName) throws Exception {
		
		if(StringUtils.isEmpty(fileName)) {
			throw new Exception("文件名不能为空");
		}
		int pos = fileName.lastIndexOf(".");
		return pos > 0 ? fileName.substring(pos+1) : "";
	}
	
	/**
	 * 文件上传，如果fileid不为空则代表是续传
	 * @param localFilePath	本地文件绝对路径
	 * @param fileId		远程文件id，不为空为续传，可为空
	 * @param meta_list		文件附加属性，可为空
	 * @return 1、文件完整上传成功，返回文件远程访问url；</br>2、上传中途中断，未完成上传，返回null
	 */
	public static String uploadAppend(String localFilePath, String fileId, NameValuePair[] meta_list) {
		
		try {
			
			File file = new File(localFilePath);
			if(!file.exists()) {
				throw new Exception("文件不存在，localFilePath："+localFilePath);
			}
			
			long length = file.length();
			
			// 1、文件id为空，说明是新文件上传，不是续传
			if(StringUtils.isEmpty(fileId)) {

				StorageServer storageServer = trackerClient.getStoreStorage(trackerServer, null);
				StorageClient1 storageClient1 = new StorageClient1(trackerServer, storageServer);

				/*// 方式一、使用 StorageClient，上传一个0字节，获取一个fileId
				storageClient = new StorageClient(trackerServer, storageServer);
				String[] uploadResults = storageClient.upload_appender_file(new byte[]{}, getFileExtName(file.getName()), meta_list);
				if(uploadResults != null) {
					fileId = uploadResults[0] + FastDFSFileManager.SEPARATOR + uploadResults[1];
				}*/
				
				// 方式二、使用 StorageClient1，上传一个0字节，获取一个fileId
				fileId = storageClient1.upload_appender_file1(new byte[]{}, getFileExtName(file.getName()), meta_list);
				
				System.out.println(">>>>>>文件id："+fileId);
			}
			
			
			// ------------开始按照文件切片追加文件----------------------
			
			long remoteFileSize = 0;	// 文件已上传完成部分的大小
			
			// 根据上面上传一个0字节获取到fileId后，开始对现在要上传的文件进行切片，写入到服务器中该fileId对于的文件中
			int pos = fileId.indexOf(StorageClient1.SPLIT_GROUP_NAME_AND_FILENAME_SEPERATOR);
			String groupName = fileId.substring(0, pos);
			String fileName = fileId.substring(pos + 1);
			StorageServer storageServer = trackerClient.getFetchStorage(trackerServer, groupName, fileName);
			StorageClient1 storageClient1 = new StorageClient1(trackerServer, storageServer);
			
			logger.info(">>>>>>StorageServer："+storageServer.getInetSocketAddress().getHostName());

			FileInfo fileInfo = getFileInfo(storageClient1, fileId);
			remoteFileSize = fileInfo.getFileSize();
			
			logger.info(">>>>>>续传前服务器上已上传上去的文件大小："+remoteFileSize);
			
			remoteFileSize= append_file(storageClient1, fileId, remoteFileSize, length, new FileInputStream(file));
			
			logger.info(">>>>>>本次续传上传的文件大小："+remoteFileSize);
			
			// 服务器上文件大小与本地文件大小一致，说明上传完成
			if(remoteFileSize == length) {
				String remoteFileUrl = FastDFSFileManager.PROTOCOL.concat(fileInfo.getSourceIpAddr()).concat("/").concat(fileId);
				logger.info("文件上传成功，远程文件访问地址："+remoteFileUrl);
				return remoteFileUrl;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 续传处理
	 * @param storageClient1	
	 * @param fileId	远程文件标识用于追加
	 * @param skipSize	已成功上传的文件大小
	 * @param length	完整文件大小
	 * @param is		完整文件流
	 * @return 分两种情况：</br>1、整个晚间上传完成后，返回整个文件大小；</br>2、中途上传异常了，则返回异常前一次上传成功后的文件大小（即下次续传时需跳过的字节数skipSize）
	 */
	public static long append_file(StorageClient1 storageClient1, String fileId, 
			long skipSize, long length, InputStream is) throws Exception {

		if(skipSize > length) {
			return length;
		}

		try {
			byte[] buff = new byte[g_buff_size_unit * 1024];
			is.skip(skipSize);
			while(skipSize <= length) {
				
				// 延时方便测试
//				Thread.sleep(1000);
				
				int result = -1;	// 0：成功
				
				int readEndPos = length - skipSize > buff.length ? buff.length : (int)(length - skipSize);	// 每次读取结束的位置
				int readCount = is.read(buff, 0, readEndPos);
				System.out.println(">>>>>>本次写入文件字节："+readCount);
				if(readCount <= 0) {
					break;
				}
				
				if(readCount > buff.length) {	// 读到结尾了，而且读取到内容未塞满缓冲区
					byte[] smallByte = new byte[readCount];
					System.arraycopy(buff, 0, smallByte, 0, readCount);
					result = storageClient1.append_file1(fileId, smallByte);
				} else {
					result = storageClient1.append_file1(fileId, buff);
				}
				
				if(result != 0) {	// 若失败了，则返回下次续传时需跳过的字节数
					return skipSize;
				}
				skipSize += readCount;	// 本次分片上传成功，则更新续传开始需要跳过的字节数
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("文件续传失败，fileId："+fileId);
		}
		// 整个文件续传完成，则返回完整的文件大小
		return length;
	}
	
	/**
	 * 下载文件，返回二进制文件字节数组
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
	 * 断点下载（将文件内容写入到输出流中）
	 * @param groupName			组名
	 * @param remoteFileName	下载的文件的远程文件名
	 * @param size		客户端已下载到本地的文件的大小（断点下载时将跳过已下载过的部分接着下载）
	 * @param out		输出流（客户端下载时，可获取到客户端的输出流后，调用此接口直接将服务端的文件通过该输出流下载到客户端）
	 * @return 0 success, return none zero errno if fail
	 */
	public static int downloadAppend(String groupName, String remoteFileName, int size, OutputStream out) {
		int result = -1;
		int reTryCount = 0;
		while(result != 0 && reTryCount < 10) {	
			//如果下载失败，继续下载，在这可以设置一定的规则（如:下载出现异常时，每间隔一段时间重试（间隔时长可配置化），超过重试次数后停止下载重试（重试次数可配置化））
			try {
				// 方式1：此处download_file方法中倒数第二个参数download_bytes设置为0，指下载时从size位置到整个文件结束位置
				result = storageClient.download_file(groupName, remoteFileName, size, 0, new DownloadStream(out));
				reTryCount++;	// 更新重试次数
				
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("断点下载（将文件内容写入到输出流中）失败，6 秒后 准备重试第："+reTryCount+"次");
				try {
					Thread.sleep(6000);	// 如果遇到异常下载失败，则等1秒再重试（此处6秒间隔时长可进行配置化）
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
		return result;
	}
	
	/**
	 * 断点下载（根据本地文件路径直接写入文件）
	 * @param groupName			组名
	 * @param remoteFileName	下载的文件的远程文件名（groupName +"/"+ remoteFileName）
	 * @param localFilePath		下载到上次未完成下载的文件在客户端本地存储的文件路径
	 * @return 0 success, return none zero errno if fail
	 */
	public static int downloadAppend(String groupName, String remoteFileName, String localFilePath) {
		int result = -1;
		try {
			File file = new File(localFilePath);
			if(file.exists()) {
				long size = file.length();
				result = storageClient.download_file(groupName, remoteFileName, size, 0, localFilePath);
			} else {
				logger.error(">>>>>>本地断点下载文件"+localFilePath+"不能存在！");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * 大文件分段下载（将大文件按指定大小分割后进行多线程下载）
	 * @param groupName			组名
	 * @param remoteFileName	下载的文件的远程文件名
	 * @param segmentSize		大文件切割成小文件分段下载时，切片分割成的小文件大小
	 * @param localFilePath		下载时本地文件路径
	 * @return 0 success, return none zero errno if fail
	 */
	public static int downloadBySegment(String groupName, String remoteFileName, 
			int segmentSize, int size, String localFilePath) {
		
		int result = -1;
		int index = 1;
		long writeCount = 0;
		long fileSize = getFileInfo(groupName, remoteFileName).getFileSize();
		while(writeCount < fileSize) {	
			//如果下载失败，继续下载，在这可以设置一定的规则（如:下载出现异常时，每间隔一段时间重试（间隔时长可配置化），超过重试次数后停止下载重试（重试次数可配置化））
			try {
				
				String pathPrefix = remoteFileName.substring(0, remoteFileName.indexOf(groupName) + groupName.length()+1);
				String fileExtName = remoteFileName.substring(remoteFileName.lastIndexOf("."));
				String fileName = remoteFileName.substring(remoteFileName.indexOf(groupName) + groupName.length()+1, remoteFileName.lastIndexOf("."));
				
				// 生成分段下载每个分段的文件名（规则：在原文件名的前面加上序号）
				fileName = fileName.replaceAll("/", "_") +"-"+ index;
				String newFilePath = pathPrefix + fileName + fileExtName;
				
				result = storageClient.download_file(groupName, remoteFileName, size, segmentSize, 
						new DownloadFileWriter(newFilePath));
				
				writeCount += segmentSize;
				index ++;
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	/**
	 * 断点下载（根据本地文件路径直接写入文件）
	 * @param groupName			组名
	 * @param remoteFileName	下载的文件的远程文件名
	 * @param size				客户端已下载到本地的文件的大小（断点下载时将跳过已下载过的部分接着下载）
	 * @param localFilePath		下载到客户端本地的文件路径（上次未完成下载的文件在客户端本地存储的文件路径，本次断点下载后讲下载内容直接在此文件上继续追加）
	 * @return 0 success, return none zero errno if fail
	 */
	public static int downloadAppend(String groupName, String remoteFileName, int size, String localFilePath) {
		int result = -1;
		try {
			result = storageClient.download_file(groupName, remoteFileName, size, 0, new DownloadFileWriter(localFilePath));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * 删除文件
	 * @param fileId	文件id（fileId = groupName +"/"+ fileName）
	 * @return	0：成功；非0：失败
	 */
	public static int deleteFile(String fileId) {
		int result = -1;
		try {
			TrackerServer trackerServer = trackerClient.getConnection();
			StorageServer storageServer = trackerClient.getFetchStorage1(trackerServer, fileId);
			StorageClient1 storageClient1 = new StorageClient1(trackerServer, storageServer);
			result = storageClient1.delete_file1(fileId);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * 删除文件
	 * @param groupName		存储服务器所在的组名
	 * @param remoteFileName	文件上传到存储服务器上后的文件名
	 * @return	0：成功；非0：失败
	 */
	public static int deleteFile(String groupName, String remoteFileName) {
		int result = -1;
		try {
			result = storageClient.delete_file(groupName, remoteFileName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
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
	 * 根据存储服务器组名 和 文件名 获取文件信息
	 * @param storageClient1
	 * @param fileId		文件id
	 * @return FileInfo
	 */
	public static FileInfo getFileInfo(StorageClient1 storageClient1, String fileId) {
		try {
			return storageClient1.get_file_info1(fileId);
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
