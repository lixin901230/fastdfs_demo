package com.lx.platform.upload;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 
 * Controller公共基类
 * 
 * @author lx
 */
@Controller
public class BaseController {
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	/**
	 * 注入HttpServletRequest对象<br/>
	 * 注意：使用Servlet中的API，需要在web.xml中注入监听：org.springframework.web.context.request.RequestContextListener
	 */
	@Autowired
	private  HttpServletRequest request;
	
	/**
	 * 注入HttpServletResponse对象<br/>
	 * 注意：使用Servlet中的API，需要在web.xml中注入监听：org.springframework.web.context.request.RequestContextListener
	 */
	@Autowired
	private  HttpServletResponse response;
	
	public HttpServletRequest getRequest() {
		return request;
	}
	
	public HttpServletResponse getResponse() {
		return response;
	}
	
	/**
	 * 获取HttpServletRequest对象<br/>
	 * 注意：需要在web.xml中注入监听：org.springframework.web.context.request.RequestContextListener
	 */
	public static HttpServletRequest getHttpServletRequest() {
		HttpServletRequest request = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
		return request;
	}
	
	/**
	 * 获取HttpServletResponse对象<br/>
	 * 注意：需要在web.xml中注入监听：org.springframework.web.context.request.RequestContextListener
	 */
	public static HttpServletResponse getHttpServletResponse() {
		HttpServletResponse response = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getResponse();
		return response;
	}
	
}
