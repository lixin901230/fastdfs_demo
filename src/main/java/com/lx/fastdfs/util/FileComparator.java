package com.lx.fastdfs.util;

import java.io.File;
import java.util.Comparator;

/**
 * 根据文件名，比较文件
 * @author lx
 */
public class FileComparator implements Comparator<File> {
	@Override
	public int compare(File o1, File o2) {
		return o1.getName().compareTo(o2.getName());
	}
}
