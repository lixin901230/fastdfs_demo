package com.lx.platform.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/**
 * 结果处理工具类
 * @author lx
 *
 */
public class HandleResultUtils {
	
	public static Map<String, Object> getResultMap(boolean success, String msg) {
		
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", success);
		result.put("msg", msg);
		
		return result;
	}

	public static Map<String, Object> getResultMap(boolean success, Object data) {
		return getResultMap(success, data, "");
	}

	public static Map<String, Object> getResultMap(boolean success, Object data, String msg) {
		
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", success);
		result.put("data", data);
		result.put("msg", msg);
		
		return result;
	}
	
	public static void writeJsonStr(HttpServletResponse response, String jsonStr) {
		PrintWriter writer = null;
		try {
			response.setCharacterEncoding("UTF-8");
			writer = response.getWriter();
			writer.write(jsonStr);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(writer != null) {
				writer.close();
			}
		}
	}
}
