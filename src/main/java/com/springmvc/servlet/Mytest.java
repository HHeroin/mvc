package com.springmvc.servlet;

import org.junit.Test;

import java.net.URL;

public class Mytest {
    @Test
    public void test() {
        URL url = Mytest.class.getClassLoader().getResource("com/example");


        System.out.println(url.getFile());
    }
}
