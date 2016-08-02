package com.lx.platform.fastdfs;

import java.io.File;
import java.io.FileInputStream;

import org.junit.Test;
import org.springframework.util.Assert;

import com.lx.platform.org.csource.fastdfs.FileInfo;
import com.lx.platform.org.csource.fastdfs.ServerInfo;
import com.lx.platform.org.csource.fastdfs.StorageServer;

/**
 * 测试 上传文件到FastDFS、获取FastDFS服务中的文件信息 
 */
public class TestFileManager {
	
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
		
		FastDFSFile file = new FastDFSFile("美女", file_buff, "jpg");

		String fileAbsolutePath = FileManager.upload(file);
		/**
		 * 返回地址：http://192.168.0.108:8080/group1/M00/00/00/wKgAbFeiIv-AcGiBAAMhq9w8VNM100.jpg，
		 * 由于使用nginx监听80端口，故根据地址：http://192.168.0.108/group1/M00/00/00/wKgAbFeiIv-AcGiBAAMhq9w8VNM100.jpg 即可访问到FDFS上的文件
		 */
		System.out.println(fileAbsolutePath);
		fis.close();
	}

	/**
	 * 测试根据存储节点服务器返回的 存储节点组名 和 远程文件名 获取fdfs服务中的
	 * @throws Exception
	 */
	@Test
	public void getFile() throws Exception {
		FileInfo file = FileManager.getFile("group1", "M00/00/00/wKgAbFeiIv-AcGiBAAMhq9w8VNM100.jpg");
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
		StorageServer[] ss = FileManager.getStoreStorages("group1");
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
		ServerInfo[] servers = FileManager.getFetchStorages("group1",
				"M00/00/00/wKgBm1N1-CiANRLmAABygPyzdlw073.jpg");
		Assert.notNull(servers);

		for (int k = 0; k < servers.length; k++) {
			System.err.println(k + 1 + ". " + servers[k].getIpAddr() + ":"
					+ servers[k].getPort());
		}
	}
}
