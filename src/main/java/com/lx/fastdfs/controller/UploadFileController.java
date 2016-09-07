package com.lx.fastdfs.controller;

import java.util.HashMap;
import java.util.Map;

import org.csource.common.NameValuePair;
import org.csource.fastdfs.ClientGlobal;
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
	public Object uploadToFdfs(@RequestParam MultipartFile file) {

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
		}
		return resultMap;
	}
}
