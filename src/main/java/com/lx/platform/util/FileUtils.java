package com.lx.platform.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {
	
	private static Logger logger = LoggerFactory.getLogger(FileUtils.class);

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
