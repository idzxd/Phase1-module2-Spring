package com.lagou.edu.annotation;

import java.lang.annotation.*;

/**
 * @program: lagou-transfer
 * @description: 自定义Service注解
 * @author: Created by zxd
 * @create: 2020-05-08 14:36
 **/
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Service {
    String value() default "";
}
