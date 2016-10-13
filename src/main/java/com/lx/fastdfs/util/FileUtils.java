package com.lx.fastdfs.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {
	
	private static Logger logger = LoggerFactory.getLogger(FileUtils.class);
	
	public static final String INCOMING = "incoming";	// 项目下资源文件存储目录

	private static final String HttpURLConnection = null;
	
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
	 * 根据网络文件的url地址下载文件
	 * @param srcFileUrl	源文件地址
	 * @param saveFilePath	下载保存文件路径
	 * @throws IOException 
	 */
	public static void downloadForNet(String srcFileUrl, String saveFilePath) throws IOException {
		
		InputStream is = null;
		OutputStream out = null;
		try {
			URL url = new URL(srcFileUrl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			
			is = conn.getInputStream();
			out = new FileOutputStream(saveFilePath);
			
			int len = 0;
			int count = 0;
			byte[] buff = new byte[4096];
			while((count=is.read(buff)) != -1) {
				out.write(buff, 0, count);
				len += count;
			}
			conn.disconnect();
			System.out.println("下载文件大小："+len);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(out != null) {
				out.close();
			}
			if(is != null) {
				is.close();
			}
		}
	}
	
	/**
	 * 根据网络文件的url地址下载文件
	 * @param srcFileUrl	源文件地址
	 * @param outputStream	文件输出流
	 * @throws IOException 
	 */
	public static void downloadForNet(String srcFileUrl, OutputStream outputStream) throws IOException {
		
		InputStream is = null;
		try {
			URL url = new URL(srcFileUrl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			is = conn.getInputStream();
			
			int len = 0;
			int count = 0;
			byte[] buff = new byte[4096];
			while((count=is.read(buff)) != -1) {
				outputStream.write(buff, 0, count);
				len += count;
			}
			conn.disconnect();
			System.out.println("下载文件大小："+len);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(outputStream != null) {
				outputStream.close();
			}
			if(is != null) {
				is.close();
			}
		}
	}
	
	/**
	 * 文件下载
	 * @param srcFileUrl	源文件地址
	 * @param outputStream	文件输出流
	 * @throws IOException 
	 */
	public static void download(InputStream inputStream, OutputStream outputStream) throws IOException {
		
		try {
			int len = 0;
			int count = 0;
			byte[] buff = new byte[4096];
			while((count=inputStream.read(buff)) != -1) {
				outputStream.write(buff, 0, count);
				len += count;
			}
			System.out.println("下载文件大小："+len);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(outputStream != null) {
				outputStream.close();
			}
			if(inputStream != null) {
				inputStream.close();
			}
		}
	}
	
	
	/**
	 * 获取指定目录下的所有文件（不包含文件夹）
	 * @param dirPath	目录路径
	 * @return
	 */
	public static List<File> getDirFiles(String dirPath) {
		
		List<File> files = new Vector<File>();
		File file = new File(dirPath);
		File[] fileArr = file.listFiles();
		for (File _file : fileArr) {
			if(_file.isFile()) {
				files.add(_file);
			}
		}
		return files;
	}
	
	/**
	 * 通过正则匹配获取指定目录下包含指定文件标识和缀文件名的所有文件（不包含文件夹）
	 * @param dirPath		目录路径
	 * @param fileSegmentFlag	分片文件表示，分段下载文件后各个分段文件的文件名标记
	 * @param fileExtName		分段文件名扩展名
	 * @return
	 */
	public static List<File> getDirFiles(String dirPath, final String fileSegmentFlag, final String fileExtName) {
		
		File file = new File(dirPath);
		File[] fileArr = file.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				
				//正则匹配文件名，如：String name = "M00_00_00_wKgAllfVgn-EW1h9AAAAAAAAAAA319.zip_part-20.segment";
				String regex = ".*"+fileSegmentFlag.toLowerCase()+"[0-9]+"+fileExtName.toLowerCase(); // 正则匹配多个任意字符，后面拼有fileSegmentFlag变量值，接着拼有1个或多个数字数字，且后缀名为fileExtName变量值的文件名
				if(name.toLowerCase().matches(regex)) {
					return true;
				}
				return false;
			}
		});
		
		List<File> files = new Vector<File>();
		for (File _file : fileArr) {
			if(_file.isFile()) {
				files.add(_file);
			}
		}
		return files;
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
