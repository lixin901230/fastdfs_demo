package com.lx.platform.upload;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

//import com.lx.complete.service.IProductSearchService;

/**
 * 框架环境测试Controller
 * 
 * @author lx
 *
 */
@Controller
@RequestMapping("/testController")
public class TestController extends BaseController {
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
//	@Autowired
//	private IProductSearchService productSearchService;
	
	/**
	 * 方式1：<br/>
	 * 测试springmvc框架环境方法
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/test.do")
	public String test(Model model) throws Exception {
		
//		String info = ">>>>>>接口注入成功："+productSearchService.getClass().getName();
		
//		logger.info(info);
		model.addAttribute("testInfo", "返回数据");
		
		HttpServletRequest request = getRequest();
		System.out.println("getRequest="+request.getRequestURL());
		
		HttpServletRequest request2 = getHttpServletRequest();
		System.out.println("getHttpServletRequest="+request2.getRequestURL());
		
		HttpServletResponse response = getResponse();
		System.out.println("getResponse="+response.getCharacterEncoding());
		
		HttpServletResponse response2 = getHttpServletResponse();
		System.out.println("getHttpServletResponse="+response2.getCharacterEncoding());
		
		return "/test";
	}
	
	/**
	 * 方式2：<br/>
	 * 测试springmvc框架环境方法
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/test2.do")
	public ModelAndView test2(Model model) throws Exception {
		
//		String info = ">>>>>>test2 接口注入成功："+productSearchService.getClass().getName();
//		logger.info(info);
		model.addAttribute("testInfo", "返回数据");
		
		return new ModelAndView("/test");
	}
}
