package com.springmvc.servlet;

import com.springmvc.annotation.Autowired;
import com.springmvc.annotation.Controller;
import com.springmvc.annotation.RequestMapping;
import com.springmvc.annotation.Service;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;

import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;

public class DispatcherServlet extends HttpServlet {

    // 保存所有的配置信息
    private Properties properties = new Properties();

    // 保存所有被扫描到的相关类名
    private List<String> classNames = new ArrayList<String>();

    //  核心Ioc容器,保存所有初始化的bean
    private Map<String, Object> ioc = new HashMap<String, Object>();

    private Map<String, Method> handlerMapping = new HashMap<String, Method>();


    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        System.out.println("初始化DispatcherServlet");
        // 1 加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        // 2 根据配置文件扫描所有的相关的类
        try {
            doScanner(properties.getProperty("scanPackage"));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        // 3 初始化所有的相关类的实例,并将器放入到IOC容器之中(Map)
        doInstance();
        // 4 实现自动依赖注入
        doAutowired();

        // 5 初始化HandlerMappping
        initHandlerMapping();


        System.out.println("haha");
    }

    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();

            if (!clazz.isAnnotationPresent(Controller.class)) {
                continue;
            }

            String baseUrl = "";
            if (clazz.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
                baseUrl = requestMapping.value();
            }

            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(RequestMapping.class)) {
                    continue;
                }

                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                String url = (baseUrl + requestMapping.value()).replaceAll("/+", "/");

                handlerMapping.put(url, method);
                System.out.println("mapping: " + url + method);
            }


        }
    }

    private void doAutowired() {
        if (ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            // 获取所有的字段
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                // 只有加了Autowired注解的成员变量才注入
                if (!field.isAnnotationPresent(Autowired.class)) {
                    return;
                }

                Autowired autowired = field.getAnnotation(Autowired.class);
                String beanName = autowired.value().trim();
                if ("".equals(beanName)) {
                    beanName = field.getType().getName();
                }

                // 强制授权访问,暴力反射
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }


            }
        }

    }

    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        try {
            for (String className : classNames) {

                Class<?> clazz = Class.forName(className);

                // 初始化IOC容器
                // ioc容器规则
                //  1 key默认用类名首字母小写
                // 2 如果用户自定义名字,那么优先选择用户自定义名字
                // 3 如果是接口的话,用接口的类型作为key

                if (clazz.isAnnotationPresent(Controller.class)) {
                    //  1 key默认用类名首字母小写
                    String beanName = lowerFisrtCase(clazz.getSimpleName());
                    ioc.put(beanName, clazz.newInstance());


                } else if (clazz.isAnnotationPresent(Service.class)) {
                    // 2 如果用户自定义名字,那么优先选择用户自定义名字
                    Service service = clazz.getAnnotation(Service.class);
                    String beanName = service.value();
                    if ("".equals(beanName.trim())) {
                        beanName = lowerFisrtCase(clazz.getSimpleName());
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);

                    // 3 如果是接口的话,用接口的类型作为key
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> i : interfaces) {
                        // 将接口类型作为key
                        ioc.put(i.getSimpleName(), instance);
                    }
                } else {
                    continue;
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String lowerFisrtCase(String str) {
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);

    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");

        if (!handlerMapping.containsKey(url)) {
            resp.getWriter().write("404");
            return;
        }
        Method method = handlerMapping.get(url);
        System.out.println(method);


      /*  //获取方法的参数列表
        Class<?>[] parameterTypes = method.getParameterTypes();

        //获取请求的参数
        Map<String, String[]> parameterMap = req.getParameterMap();

        //保存参数值
        Object [] paramValues= new Object[parameterTypes.length];

        //方法的参数列表
        for (int i = 0; i<parameterTypes.length; i++){
            //根据参数名称，做某些处理
            String requestParam = parameterTypes[i].getSimpleName();


            if (requestParam.equals("HttpServletRequest")){
                //参数类型已明确，这边强转类型
                paramValues[i]=req;
                continue;
            }
            if (requestParam.equals("HttpServletResponse")){
                paramValues[i]=resp;
                continue;
            }
            if(requestParam.equals("String")){
                for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
                    String value =Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
                    paramValues[i]=value;
                }
            }
        }
        //利用反射机制来调用
        try {
            method.invoke(this.handlerMapping.get(url), paramValues);//第一个参数是method所对应的实例 在ioc容器中
        } catch (Exception e) {
            e.printStackTrace();
        }
*/

    }

    /*****
     *  读取配置文件中的配置并存储到properties中
     */
    private void doLoadConfig(String location) {
        System.out.println("配置文件:" + location);
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(location);
        try {
            properties.load(resourceAsStream);
            String scanPackage = properties.getProperty("scanPackage");
            System.out.println("==========" + scanPackage);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (resourceAsStream != null) {
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    private void doScanner(String packageName) throws URISyntaxException {
        /****
         *  getResource 路径中不能有空格,否则File类不能使用
         *  URL url =  this.getClass().getClassLoader().getResource(packageName.replaceAll("\\.", "/"));
         *  File dir = new File(url.getPath());
         */

        /*URL url =  this.getClass().getClassLoader().getResource(packageName.replaceAll("\\.", "/"));
        String path = null;
        try {
            path = URLDecoder.decode(url.getPath(),"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }*/


        String path = this.getClass().getClassLoader().getResource(packageName.replaceAll("\\.", "/")).toURI().getPath();
        File dir = new File(path);

        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                //递归读取包
                doScanner(packageName + "." + file.getName());
            } else {
                String className = packageName + "." + file.getName().replace(".class", "");
                classNames.add(className);
            }
        }
    }

    private String replaceTo(String path) {
        return path.replaceAll("\\.", "/");
    }


}
