package com.lx.fastdfs.util;

import java.io.FileOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {
	
	private static Logger logger = LoggerFactory.getLogger(FileUtils.class);
	
	public static final String INCOMING = "incoming";	// 项目下资源文件存储目录
	
	/**
	 * 下载二进制文件到本地
	 * @param bytes
	 * @param filePath
	 */
	public static void download(byte[] bytes, String filePath) {
		
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(filePath);
			fos.write(bytes);
			fos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}

	/**
	 * 获取webapp物理路径
	 * @return
	 */
	public static String getWebappPath() {
		
		try {
			String classesPath = FileUtils.class.getResource("/").getPath();
			classesPath = classesPath.startsWith("/") ? classesPath.substring(1) : classesPath;
			String webPath = "";
			if(classesPath.indexOf("WEB-INF") > -1) {
				webPath = classesPath.substring(0, classesPath.indexOf("WEB-INF"));
			} else if(classesPath.indexOf("target") > -1) {	//junit 测试获取的路径
				webPath = classesPath.substring(0, classesPath.indexOf("target"));
				webPath += "src/main/webapp/";	//索引存放路径
			}
			logger.info("webPath=="+ webPath);
			return webPath;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
