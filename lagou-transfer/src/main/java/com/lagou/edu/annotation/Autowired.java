package com.lagou.edu.annotation;

import java.lang.annotation.*;

/**
 * @program: lagou-transfer
 * @description: 自定义Autowired注解
 * @author: Created by zxd
 * @create: 2020-05-08 14:35
 **/
/*Target 注解限定了该注解的使用场景。
ElementType.CONSTRUCTOR 可以给构造方法进行注解
ElementType.METHOD 可以给方法进行注解
ElementType.PARAMETER 可以给一个方法内的参数进行注解
ElementType.TYPE 可以给一个类型进行注解，比如类、接口、枚举
ElementType.FIELD 可以给属性进行注解
ElementType.ANNOTATION_TYPE 可以给一个注解进行注解*/

/*Retention 注解用来标记这个注解的留存时间。
RetentionPolicy.RUNTIME。注解可以保留到程序运行的时候，
它会被加载进入到 JVM 中，所以在程序运行时可以获取到它们*/

/*@ Documented 注解表示将注解信息写入到 javadoc 文档中。*/
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {
    boolean required() default true;
}
