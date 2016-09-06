package com.lx.platform.fastdfs;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.util.Assert;

import com.lx.platform.org.csource.fastdfs.FileInfo;
import com.lx.platform.org.csource.fastdfs.ServerInfo;
import com.lx.platform.org.csource.fastdfs.StorageServer;
import com.lx.platform.util.FileUtils;

/**
 * 测试 上传文件到FastDFS、获取FastDFS服务中的文件信息 
 */
public class TestFastDFSFileManager {
	
	/**
	 * 上传文件到fdfs存储节点服务器中，并返回存储节点组名 和 远程文件名称
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
	
	@Test
	public void testDownload() {
		try {
			String fileSavePath = FileUtils.getWebappPath()+"incoming/wKgAl1erTRGAcp8MAAjVftix7WI572.jpg";

			// 获取文件二进制字节码
			byte[] bytes = FastDFSFileManager.download("group1", "M00/00/00/wKgAl1erTRGAcp8MAAjVftix7WI572.jpg");
			FileUtils.download(bytes, fileSavePath);
			
			System.out.println("下载文件："+ fileSavePath);
			
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
		FileInfo file = FastDFSFileManager.getFileInfo("group1", "M00/00/00/wKgAl1erTRGAcp8MAAjVftix7WI572.jpg");
		Assert.notNull(file);
		String sourceIpAddr = file.getSourceIpAddr();
		long size = file.getFileSize();
		System.out.println("ip:" + sourceIpAddr + ",size:" + size);
		System.out.println("文件访问地址："+"http://"+sourceIpAddr+"/group1/M00/00/00/wKgAbFeiIv-AcGiBAAMhq9w8VNM100.jpg");
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
			System.err.println(k
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
				"M00/00/00/wKgBm1N1-CiANRLmAABygPyzdlw073.jpg");
		Assert.notNull(servers);

		for (int k = 0; k < servers.length; k++) {
			System.err.println(k + 1 + ". " + servers[k].getIpAddr() + ":"
					+ servers[k].getPort());
		}
	}
}