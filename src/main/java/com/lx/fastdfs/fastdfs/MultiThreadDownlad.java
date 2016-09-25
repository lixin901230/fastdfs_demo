package com.lx.fastdfs.fastdfs;

import java.io.File;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.StorageClient1;
import org.csource.fastdfs.StorageServer;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;

/**
 * 大文件多线程分段下载
 * @author lx
 */
public class MultiThreadDownlad {
	
	private static Logger logger  = Logger.getLogger(MultiThreadDownlad.class);
	
	/**
	 * 大文件多线程分段下载的线程数
	 */
	private static final int TCOUNT = 10;
	
	/**
	 * 线程计数器，作用：使一个线程等待其他线程完成各自的工作后再执行，多线程分段下载文件时，用该类来控制主线程等各个子线程下载完成后，再进行文件合并
	 */
	private CountDownLatch latch = new CountDownLatch(TCOUNT);
	
	// 初始化fdfs配置文件
	static {
		try {
			String classPath = new File(MultiThreadDownlad.class.getResource("/").getFile()).getCanonicalPath();
			String fdfsClientConfPath = classPath + File.separator + FastDFSFileManager.CLIENT_CONFIG_FILE;
			
			logger.info("FastDFS 配置文件路径:"+ fdfsClientConfPath);
			ClientGlobal.init(fdfsClientConfPath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 大文件分段多线程下载，将一个文件分成多个片段通过多线程同时下载各片段
	 * @param groupName	组名
	 * @param remoteFileName 文件名
	 * @param fileSize		文件大小
	 * @param localFilePath 下载文件存放路径
	 * @return
	 * @throws Exception 
	 */
	public void downloadByMultiThread(String groupName, String remoteFileName, 
			long fileSize, String localFilePath) throws Exception {

		ExecutorService executor = Executors.newFixedThreadPool(TCOUNT); // 默认分10个分段，10个线程同时下载，该分段数可作为参数传入
		
		long segmentSize = fileSize / TCOUNT;	// 分段的文件的大小
		long lastLength = fileSize % TCOUNT;	// 文件分成N份后，剩下的部分		
		
		// 计算每个线程请求文件的开始和结束位置
		long startPos = 0;	// 分段的文件的开始位置
		long endPos = startPos + segmentSize;	// 分段文件的结束位置
		
		for (int i = 0; i < TCOUNT; i++) {
			
			if(lastLength > 0) {//将分成TCOUNT个分段后剩下的部分包分再分给每个分段，因为剩余的这部分包是求余得到的，故这个余数不会超过TCOUNT值，所以可将这剩下的这部分包分给每个分段文件包
				endPos++;
				lastLength--;
			}
			
			System.out.println("startPos="+startPos+"\t   endPos="+endPos);
			
			// 此处需要改进使用连接池
			TrackerClient trackerClient = new TrackerClient();
			TrackerServer trackerServer = trackerClient.getConnection();
			StorageServer storageServer = trackerClient.getFetchStorage(trackerServer, groupName, remoteFileName);
			StorageClient1 storageClient1 = new StorageClient1(trackerServer, storageServer);
			executor.execute(new DownloadThread(storageClient1, groupName, remoteFileName, localFilePath, startPos, endPos));
			
			startPos = endPos;	//更新下个分段的开始位置
		}
		latch.await();	// 让主线程处于等待状态，当所有子线程完成后再执行主线程
	}
	
	/**
	 * 多线程下载
	 * @author lx
	 */
	class DownloadThread implements Runnable {
		
		private StorageClient storageClient;
		private String groupName;
		private String remoteFileName;
		private long fileStartPos;
		private long fileEndPos;
		private String localFilePath;
		
		private Object lock = new Object();
		
		public DownloadThread(StorageClient storageClient, String groupName, 
				String remoteFileName, String localFilePath, long fileStartPos, long fileEndPos) {
			this.storageClient = storageClient;
			this.groupName = groupName;
			this.remoteFileName = remoteFileName;
			this.fileStartPos = fileStartPos;
			this.fileEndPos = fileEndPos;
			this.localFilePath = localFilePath;
		}
		
		@Override
		public void run() {
			synchronized(DownloadThread.class){
			
			long threadNo = latch.getCount();
			Thread.currentThread().setName("线程："+threadNo);
			System.out.println(Thread.currentThread().getName());
			
			String extName = remoteFileName.substring(remoteFileName.lastIndexOf("."));
			String fileName = remoteFileName.substring(remoteFileName.lastIndexOf("/")+1, remoteFileName.lastIndexOf(".")) +"_"+ threadNo +"_"+ new Date().getTime() + extName;
			String downloadRealFilePath = localFilePath +"/"+ fileName;
			try {
				File tempFile = new File(downloadRealFilePath);
				if(tempFile.exists()) {
					fileStartPos = tempFile.length();
				}
				
				System.out.println(downloadRealFilePath);
				System.out.println("fileStartPos="+fileStartPos);
				
				int flag = storageClient.download_file(groupName, remoteFileName, fileStartPos, fileEndPos, downloadRealFilePath);
				latch.countDown();	//当前子线程下载任务完成，通知线程计数器该线程执行完成
				
				System.out.println("线程："+latch.getCount()+" 下载完成");
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			}
		}
	}
	

	/**
	 * 合并下载后的分片文件
	 */
	public void mergeSegmentFile() {
		
	}
}
