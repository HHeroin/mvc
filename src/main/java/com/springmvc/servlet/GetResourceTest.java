package com.springmvc.servlet;
import org.junit.Test;

import java.io.*;
import java.net.*;
import java.util.Properties;
import java.util.ResourceBundle;

public class GetResourceTest {

    @Test
    public void test() throws IOException, URISyntaxException {
//        InputStream is = Mytest.class.getClassLoader().getResourceAsStream("application.properties");
//        InputStream is = new FileInputStream(new File("E:\\java\\code git\\mvc\\src\\main\\resources\\application.properties"));
        InputStream is = GetResourceTest.class.getResourceAsStream("/application.properties");
        Properties properties = new Properties();
        properties.load(is);
        String scanPackage = (String) properties.get("scanPackage");


        /*ResourceBundle bundle = ResourceBundle.getBundle("application");
        String scanPackage = bundle.getString("scanPackage");*/

        System.out.println(scanPackage);
    }

}
