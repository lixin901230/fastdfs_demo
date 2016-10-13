package com.lx.fastdfs.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

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
	 * @param dirPath	目录路径
	 * @param regex		正则表达式字符串；用于从指定目录中找出与该正则表达式匹配的文件名的文件
	 * @return
	 */
	public static List<File> getDirFiles(String dirPath, final String regex) {
		
		File file = new File(dirPath);
		File[] fileArr = file.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
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
	 * 合并文件流
	 * @param dirPath		文件所在目录路径
	 * @param mergeFileName	合并后的文件名
	 * @param regex	用于根据文件名从dirPath参数指定的目录中筛选文件的正则表达式
	 * @param isDesc 是否需要倒序，true：是，false：否，默认false；在多线程下载使用了并发线程计数器后，则合并文件时需要使用该参数倒序排序子文件后再合并
	 * @throws IOException 
	 */
	public static void mergeSegmentFile(String dirPath, String mergeFileName, String regex, boolean isDesc) {
		
		try {
			List<File> files = FileUtils.getDirFiles(dirPath, regex);
			FileUtils.mergeSegmentFile(dirPath, mergeFileName, files, isDesc);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 合并分段下载后的分片文件为一个完整的文件（使用 java.io.SequenceInputStream 合并多个文件流）
	 * @param dirPath		文件所在目录路径
	 * @param mergeFileName	合并后的文件名
	 * @param files	分片文件名集合
	 * @param isDesc 是否需要倒序，true：是，false：否，默认false；在多线程下载使用了并发线程计数器后，则合并文件时需要使用该参数倒序排序子文件后再合并
	 * @throws IOException 
	 */
	public static void mergeSegmentFile(String dirPath, String mergeFileName, List<File> files, boolean isDesc) throws Exception {
		
		SequenceInputStream seqIStream = null;
		OutputStream out = null;
		try {
			
			// 排序分片文件
			Collections.sort(files, new FileComparator());
			if(isDesc) {
				Collections.reverse(files);
			}
			
			Vector<FileInputStream> vector = new Vector<FileInputStream>();
			for (File file : files) {
				if(file.exists() && file.isFile()) {
					vector.add(new FileInputStream(file));
				} else {
					throw new Exception(file.getParent()+"文件不存在，合并失败");
				}
			}
			
			Enumeration<FileInputStream> enumeration = vector.elements();
			seqIStream = new SequenceInputStream(enumeration);
			
			dirPath = dirPath.endsWith("/") ? dirPath : (dirPath+"/");
			String fileRealPath = dirPath + mergeFileName;
			out = new FileOutputStream(fileRealPath);
			
			int len = 0;
			int count = 0;
			byte[] buff = new byte[4096];
			while((count = seqIStream.read(buff)) != -1) {
				out.write(buff, 0, count);
				len += count;
			}
			System.out.println("合并文件总长度："+len);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(seqIStream != null) {
				seqIStream.close();
			}
			if(out != null) {
				out.close();
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
