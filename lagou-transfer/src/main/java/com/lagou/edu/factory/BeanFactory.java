package com.lagou.edu.factory;

import com.lagou.edu.annotation.Autowired;
import com.lagou.edu.annotation.Service;
import com.lagou.edu.annotation.Transactional;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lagou.edu.constants.SpringConstants.CLASS;


/**
 * @author 应癫
 *
 * 工厂类，生产对象（使用反射技术）
 */
public class BeanFactory {

    /**
     * 任务一：读取解析xml，通过反射技术实例化对象并且存储待用（map集合）
     * 任务二：对外提供获取实例对象的接口（根据id获取）
     */

    private static Map<String,Object> map = new HashMap<>();  // 存储对象

    private static List<String> classPaths = new ArrayList<>();

    private static List<String> beanClassNameList = new ArrayList<>();

    static {
        // 任务一：读取解析xml，通过反射技术实例化对象并且存储待用（map集合）
        // 加载xml
        InputStream resourceAsStream = BeanFactory.class.getClassLoader().getResourceAsStream("beans.xml");
        // 解析xml
        SAXReader saxReader = new SAXReader();
        try {
            Document document = saxReader.read(resourceAsStream);
            Element rootElement = document.getRootElement();


            List<Element> packageList = rootElement.selectNodes("//context");
            if (packageList != null && !packageList.isEmpty()) {
                Element element = packageList.get(0);
                String packagename = element.attributeValue("basepackage");
                // 扫描包
                doScanPackage(packagename);
                // 组装service对象
                doLoadBeanDefinition();
                // 扫描autowired
                doScanAutowired();
                // 扫描声明式事务注解
                doScanTransactional();
            }


            List<Element> beanList = rootElement.selectNodes("//bean");
            for (int i = 0; i < beanList.size(); i++) {
                Element element =  beanList.get(i);
                // 处理每个bean元素，获取到该元素的id 和 class 属性
                String id = element.attributeValue("id");        // accountDao
                String clazz = element.attributeValue("class");  // com.lagou.edu.dao.impl.JdbcAccountDaoImpl
                // 通过反射技术实例化对象
                Class<?> aClass = Class.forName(clazz);
                Object o = aClass.newInstance();  // 实例化之后的对象

                // 存储到map中待用
                map.put(id,o);

            }

            // 实例化完成之后维护对象的依赖关系，检查哪些对象需要传值进入，根据它的配置，我们传入相应的值
            // 有property子元素的bean就有传值需求
            List<Element> propertyList = rootElement.selectNodes("//property");
            // 解析property，获取父元素
            for (int i = 0; i < propertyList.size(); i++) {
                Element element =  propertyList.get(i);   //<property name="AccountDao" ref="accountDao"></property>
                String name = element.attributeValue("name");
                String ref = element.attributeValue("ref");

                // 找到当前需要被处理依赖关系的bean
                Element parent = element.getParent();

                // 调用父元素对象的反射功能
                String parentId = parent.attributeValue("id");
                Object parentObject = map.get(parentId);
                // 遍历父对象中的所有方法，找到"set" + name
                Method[] methods = parentObject.getClass().getMethods();
                for (int j = 0; j < methods.length; j++) {
                    Method method = methods[j];
                    if(method.getName().equalsIgnoreCase("set" + name)) {  // 该方法就是 setAccountDao(AccountDao accountDao)
                        method.invoke(parentObject,map.get(ref));
                    }
                }

                // 把处理之后的parentObject重新放到map中
                map.put(parentId,parentObject);

            }

        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }


    }

    private static void doScanTransactional() {

        for (String beanClassname : beanClassNameList) {
              try {
                Class<?> clazz = Class.forName(beanClassname);
                // 判断是否为server注解
                if (clazz.isAnnotationPresent(Transactional.class)) {

                    Transactional transactional = clazz.getAnnotation(Transactional.class);
                    // 这里没处理别名的情况
                    String beanId = lowerCase(beanClassname);
                    Class<?>[] interfaces = clazz.getInterfaces();
                    ProxyFactory proxyFactory = (ProxyFactory) map.get("proxyFactory");
                    if (proxyFactory == null) {
                        System.out.println("没有代理工厂");
                        continue;
                    }
                    Object proxyObj = null;
                    Object originalObject = map.get(beanId);

                    proxyObj = doProxyObject(interfaces, proxyFactory, originalObject);
                    map.put(beanId, proxyObj);
                } else {
                    continue;
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private static Object doProxyObject(Class<?>[] interfaces, ProxyFactory proxyFactory, Object originalObject) {
        Object proxyObj;
        if (interfaces.length == 0) {
            System.out.println("使用cglib");
            proxyObj = proxyFactory.getCglibProxy(originalObject);
        } else {
            System.out.println("使用jdk动态代理");
            proxyObj = proxyFactory.getJdkProxy(originalObject);
        }
        return proxyObj;
    }

    private static void doScanAutowired() {
        if (map.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(Autowired.class)) {// autowired没有注解
                    continue;
                }
                String beanName = "";
                if (field.getAnnotation(Autowired.class) != null) {
                    beanName = field.getType().getName();
                }
                String property = lowerCase(beanName);
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(), map.get(property));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }

    private static void doLoadBeanDefinition() {
        // 对获取到的文件进行遍历，将有service注解的生成bean注入到ioc容器中
        for (String beanClassname : beanClassNameList) {

            try {
                Class<?> clazz = Class.forName(beanClassname);
                if (clazz.isAnnotationPresent(Service.class)) {
                    Service service = clazz.getAnnotation(Service.class);
                    String beanName = service.value();
                    if (!"".equals(beanName.trim())) {
                        map.put(beanName, clazz.newInstance());
                        continue;
                    }
                    map.put(lowerCase(beanClassname), clazz.newInstance());
                } else {
                    continue;
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }
    }

    private static void doScanPackage(String packageName) throws ClassNotFoundException {
        String classpath = BeanFactory.class.getResource("/").getPath();
        packageName = packageName.replace(".", File.separator);
        String searchPath = classpath + packageName;
        File file = new File(searchPath);
        doGetPath(file);
        for (String  beanClassname : classPaths) {
            beanClassname = beanClassname.replace(classpath, "").replace("/", ".")
                    .replace(CLASS, "");
            beanClassNameList.add(beanClassname);
        }
    }
    private static void doGetPath(File file) {
        if (file.isDirectory()) {
            //如果是文件夹进行递归
            File[] files = file.listFiles();
            for (File temp : files) {
                doGetPath(temp);
            }
        } else {
            //如果是文件，判断是否是class文件
            if (file.getName().endsWith(CLASS)) {
                classPaths.add(file.getPath());
            }
        }
    }

    private static String lowerCase(String name) {
        if (name == null || "".equals(name)) {
            return null;
        }
        String fName = name.substring(name.lastIndexOf(".") + 1);
        String s = String.valueOf(fName.charAt(0)).toLowerCase() + fName.substring(1);
        // 去掉Impl结尾
        return s.replace("Impl", "");
    }
    // 对外提供获取实例对象的接口（根据id获取）
    public static Object getBean(String id) {
        return map.get(id);
    }

}
