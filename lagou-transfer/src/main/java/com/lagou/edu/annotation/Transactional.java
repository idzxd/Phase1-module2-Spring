package com.lagou.edu.annotation;

import java.lang.annotation.*;

/**
 * @program: lagou-transfer
 * @description: 自定义Transactional注解
 * @author: Created by zxd
 * @create: 2020-05-08 14:36
 **/
/*@ Inherited注解标识子类将继承父类的注解属性。*/
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Transactional {
    String value() default "";
}
