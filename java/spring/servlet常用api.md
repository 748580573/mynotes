**servlet一些常用的工具api**

````java
public class BaseController{

    /**
     * 返回json串
     * @param response
     * @param obj
     */
    public void resultJSON(HttpServletResponse response, Object obj){
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control","no-store, max-age=0, no-cache, must-revalidate");
        response.addHeader("Cache-Control", "post-check=0, pre-check=0");
        response.setHeader("Pragma", "no-cache");
        try {
            PrintWriter out = response.getWriter();
            out.write(JSON.toJSONString(obj, SerializerFeature.DisableCircularReferenceDetect));//必须加上第二个参数。忽略循环引用
            out.flush();
            out.close();
        } catch (IOException e) {
            LogUtils.error(null, e);
        }
    }

    /**
     * 翻译常规字符串
     * @param response
     * @param result
     */
    public void resultText(HttpServletResponse response, String result) {
        response.setContentType("text/json;charset=UTF-8");
        response.setHeader("Cache-Control","no-store, max-age=0, no-cache, must-revalidate");
        response.addHeader("Cache-Control", "post-check=0, pre-check=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        try {
            PrintWriter out = response.getWriter();
            out.write(result);
            out.flush();
            out.close();
        } catch (IOException e) {
            LogUtils.error(null, e);
        }
    }

    //获取form表单数据
    @SuppressWarnings("unchecked")
    public Map<String,Object> getFormData(HttpServletRequest request){
        Map<String,Object> map = new HashMap<String,Object>();
        Enumeration en = request.getParameterNames();
        while(en.hasMoreElements()){
            String name=(String)en.nextElement();
            String [] values= request.getParameterValues(name);
            if(!name.contains("[]")){
                if(!values[0].equals(""))
                    map.put(name, values[0]);
            }else {
                map.put(name.replace("[]", ""), values);
            }
        }
        // map.put("monthNO",map.get("monthNO").toString().replace("年", ""));
        // map.put("monthNO",map.get("monthNO").toString().replace("月", ""));
        return map;
    }

    /**
     * 获取IP地址
     * @param request
     * @return
     */
    public String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    public CurrentData getCurrentData(HttpServletRequest request){
        HttpSession session = request.getSession();

        return (CurrentData) session.getAttribute("CurrentData");
    }
}
````

