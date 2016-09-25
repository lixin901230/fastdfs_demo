package com.lx.fastdfs.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.csource.common.NameValuePair;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.DownloadStream;
import org.csource.fastdfs.StorageClient1;
import org.csource.fastdfs.StorageServer;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.lx.fastdfs.util.HandleResultUtils;
import com.lx.fastdfs.util.JsonUtils;

@Controller
@RequestMapping("/uploadFile")
public class UploadFileController extends BaseController {
	
	private static Logger logger = LoggerFactory.getLogger(UploadFileController.class);
	
	@RequestMapping(value = "/uploadToFdfs", method = RequestMethod.POST)
	@ResponseBody
	public Object upload(HttpServletRequest request, HttpServletResponse response, 
			@RequestParam MultipartFile file) {

		Map<String, Object> resultMap = new HashMap<String, Object>();
		try {
			if(file.isEmpty()){
				resultMap = HandleResultUtils.getResultMap(false, "上传内容为空");
			}else{
				logger.info("UserController-upload-request-file=" + file.getOriginalFilename());
				
				String tempFileName = file.getOriginalFilename();
				//fastDFS方式
				ClassPathResource cpr = new ClassPathResource("fdfs_client.conf");
				ClientGlobal.init(cpr.getClassLoader().getResource("fdfs_client.conf").getPath());
				byte[] fileBuff = file.getBytes();
			    String fileId = "";
			    String fileExtName = tempFileName.substring(tempFileName.lastIndexOf("."));
				
			    //建立连接
			    TrackerClient tracker = new TrackerClient();
			    TrackerServer trackerServer = tracker.getConnection();
			    StorageServer storageServer = null;
			    StorageClient1 client = new StorageClient1(trackerServer, storageServer);
			    
			    //设置元信息（文件的附加信息）
			    NameValuePair[] metaList = new NameValuePair[3];
			    metaList[0] = new NameValuePair("fileName", tempFileName);
			    metaList[1] = new NameValuePair("fileExtName", fileExtName);
			    metaList[2] = new NameValuePair("fileLength", String.valueOf(file.getSize()));
			    
			    //上传文件
			    fileId = client.upload_file1(fileBuff, fileExtName, metaList);
			    
			    resultMap = HandleResultUtils.getResultMap(true, fileId);
			}
			
			logger.info("UploadFileController-upload-response-" + JsonUtils.objToJsonStr(resultMap));
		} catch (Exception e) {
			resultMap = HandleResultUtils.getResultMap(false, e.getMessage());
			logger.error("UploadFileController-upload-error", e);
			e.printStackTrace();
		}
		return resultMap;
	}
	
	/**
	 * 文件下载——支持断点续传下载
	 * @param request
	 * @param response
	 */
	@RequestMapping(value = "/downloadFromFdfs", method = RequestMethod.POST)
	public void downloadAppend(HttpServletRequest request, HttpServletResponse response) {
		
		String fileId = request.getParameter("fileId");	// 上传文件到fdfs后返回的fileId，下载将根据该id找到文件进行下载
		
		//测试数据
		fileId = "group1/M00/00/00/wKgAllfVgn-EW1h9AAAAAAAAAAA319.zip";
		
		/* 断点续传下载http请求头参数，客户端断点续传下载时需要记录本次下载的位置，下次下载是传入这个值接着下载；
		 * 典型的格式如：
		 * 	1、Range: bytes=0-499 下载第0-499字节范围的内容，
		 * 	2、Range: bytes=500-999 下载第500-999字节范围的内容，
		 * 	3、Range: bytes=0- 下载从第0字节开始到文件结束部分的内容，
		 * 	4、Range: bytes=-500  下载最后500字节的内容
		 */
		String range = request.getHeader("Range");
		String[] bytes = range.split("=");
		long startPos = 0; 
		long endPos = 0; 
		if(bytes != null && bytes.length > 0) {
			if(bytes[1].contains("-")) {
				String[] rangeVal = bytes[1].split("-");
				try {
					startPos = StringUtils.isNotEmpty(rangeVal[0]) ? Long.valueOf(rangeVal[0]) : 0;
				} catch (NumberFormatException e) {
					e.printStackTrace();
					startPos = 0;
				}
				try {
					endPos = StringUtils.isNotEmpty(rangeVal[1]) ? Long.valueOf(rangeVal[1]) : 0;
				} catch (NumberFormatException e) {
					e.printStackTrace();
					endPos = 0;
				}
			}
		}
		if(endPos > 0 && startPos > endPos) {	// 验证若开始位置比结束位置要大，则默认结束位置为整个文件结束位置
			endPos = 0;	// fdfs的StorageClient1的download_file1方法中，参数download_bytes=0表示文件末尾位置
		}
		
		ServletOutputStream out = null;
		try {	
			out = response.getOutputStream();
			
			//fastDFS方式
			ClassPathResource cpr = new ClassPathResource("fdfs_client.conf");
			ClientGlobal.init(cpr.getClassLoader().getResource("fdfs_client.conf").getPath());
		    			
		    //建立连接
		    TrackerClient tracker = new TrackerClient();
		    TrackerServer trackerServer = tracker.getConnection();
		    StorageServer storageServer = null;
		    StorageClient1 client = new StorageClient1(trackerServer, storageServer);
		    
		    //下载文件
		    int flag = client.download_file1(fileId, startPos, endPos, new DownloadStream(out));	//该方法中参数file_offset表示文件下载的开始位置，可用于断点续传下载；参数download_bytes表示文件下载的结束位置，0代表整个文件结尾位置
		    if(flag == 0) {
		    	logger.info(fileId+"文件下载成功！");
		    } else {
		    	logger.error(fileId+"文件下载失败");
		    }
		} catch (Exception e) {
			logger.error("UploadFileController-download-error", e);
			e.printStackTrace();
		} finally {
			if(out != null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
