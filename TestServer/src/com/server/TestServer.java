package com.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class TestServer
 */
@WebServlet("/another/TestServer")
public class TestServer extends HttpServlet {
	private static final long serialVersionUID = 1L;
	static Map<String, Integer> workers = new HashMap<String,Integer>();

    /**
     * Default constructor. 
     */
    public TestServer() {
        //System.out.println(111);
    	//workers.put("192.168.1.226", 0);
    	workers.put("192.168.1.227:80/infer.php", 0);
    	//workers.put("http://ec2-34-208-42-160.us-west-2.compute.amazonaws.com/tensorflow/www/infer.php", 0);
    	//workers.put("192.168.1.228", 0);
    	//workers.put("192.168.1.229", 0);
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter wr = response.getWriter();
		//System.out.println(123);
		/*for (Map.Entry<String, Integer> en : workers.entrySet()) {
			wr.print(en.getKey()+": ");
			wr.println(en.getValue());
		}
		//String str = request.getParameter("optimize");
		wr.println(request.getContentType());
		Map<String, String[]> m = request.getParameterMap();
		for (Map.Entry<String, String[]> en : m.entrySet()) {
			wr.print(en.getKey()+": ");
			for(String str : en.getValue()){
				wr.println(str);
			}
			
		}
		wr.println("----------------------------------------------------------");*/
		try{
			if(request.getParameter("dispatch")!=null){
			
				int n = Integer.parseInt(request.getParameter("dispatch"));
				int min = 0;
				String ip = null;
				for (Map.Entry<String, Integer> en : workers.entrySet()) {
					if(ip==null||en.getValue()<=min){
						ip = en.getKey();
						min = en.getValue();
					}
				}
				workers.put(ip, min+n);
				wr.println(ip);
			
			}else if(request.getParameter("finish-ip")!=null&&request.getParameter("finish-count")!=null){
				String ip = request.getParameter("finish-ip");
				if(workers.containsKey(ip)){
					int val = workers.get(ip);
					workers.put(ip, val-Integer.parseInt(request.getParameter("finish-count")));
					wr.println("success");
				}
			}else{
				for (Map.Entry<String, Integer> en : workers.entrySet()) {
					wr.print(en.getKey()+": ");
					wr.println(en.getValue());
				}
			}
		}catch(NumberFormatException e){
			wr.println("NumberFormatException");
		}
		/*wr.println("----------------------------------------------------------");
		*/
		response.getWriter().flush();
		//request.getRequestDispatcher("/index.jsp").forward(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}
	

	@Override
	public void init(ServletConfig config) throws ServletException {
		//System.out.println("awsaaa");
		super.init(config);
	}

}
