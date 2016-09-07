package com.lx.fastdfs.util;

import net.sf.json.JSONObject;

public class JsonUtils {

	public static String objToJsonStr(Object object) {
		
		try {
			JSONObject jsonObject = JSONObject.fromObject(object);
			String json = jsonObject.toString();
			return json;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
