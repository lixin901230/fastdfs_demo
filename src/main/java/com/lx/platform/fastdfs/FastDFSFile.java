package com.lx.platform.fastdfs;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * FDFS文件信息bean
 */
public class FastDFSFile {
	
	// 文件名
	private String name;
	
	// 二进制文件内容
	private byte[] content;
	
	// 文件扩展名
	private String ext;
	
	// 文件附加属性键值对，例如图片高度、宽度、作者等等
	private Map<String, String> nameValuePairMap = new LinkedHashMap<String, String>();
	
	/**
	 * @param name	文件名
	 * @param content	文件二进制内容	
	 * @param ext	文件扩展名
	 * @param nameValuePair	文件附加属性名称-值对集合
	 */
	public FastDFSFile(String name, byte[] content, String ext, Map<String, String> nameValuePair) {
		super();
		this.name = name;
		this.content = content;
		this.ext = ext;
		this.nameValuePairMap = nameValuePair;
	}
	
	/**
	 * @param name	文件名
	 * @param content	文件二进制内容	
	 * @param ext	文件扩展名
	 */
	public FastDFSFile(String name, byte[] content, String ext) {
		super();
		this.name = name;
		this.content = content;
		this.ext = ext;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	public String getExt() {
		return ext;
	}

	public void setExt(String ext) {
		this.ext = ext;
	}

	public Map<String, String> getNameValuePairMap() {
		return nameValuePairMap;
	}

	public void setNameValuePairMap(Map<String, String> nameValuePairMap) {
		this.nameValuePairMap = nameValuePairMap;
	}
	
	
}
