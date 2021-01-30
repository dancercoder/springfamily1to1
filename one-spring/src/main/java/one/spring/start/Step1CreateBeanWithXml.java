package one.spring.start;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * 1. xml 文档定义各个部分的作用
 * 2. xsd是什么？有什么作用？
 * 3. 列出BeanFactory的继承关系
 * 4. beans-schema中default-autowire的不同类型处理有什么不同？AbstractAutowireCapableBeanFactory中根据类型的不同查找备用property；
 * 5. AbstractBeanFactory中alreadyCreated、mergedBeanDefinitions等Set、Map有什么作用？
 * 6. DefaultSingletonBeanRegistry中的singletonObjects等Map有什么作用？
 * 7. AbstractBeanDefinition中加载指定的Class时使用了ClassLoader.loadClass方法，该方法中有锁
 * 8. AbstractAutowireCapableBeanFactory中getInstantiationStrategy()方法管理Bean实例化策略：Cglib+Simple
 * 9. xml文件中schema的解析过程
 *
 */
public class Step1CreateBeanWithXml {

    public static void main(String[] args) {

        BeanFactory bf = new XmlBeanFactory(new ClassPathResource("step1.xml"));
        Step1Bean bean = (Step1Bean) bf.getBean("step1Bean");
        System.out.println(bean.getName());

        Step1Depend1 depend1=(Step1Depend1)bf.getBean("step1Depend1");
        System.out.println(depend1.getName());

        Step1Depend2 depend2=(Step1Depend2)bf.getBean("step1Depend2");
        System.out.println(depend2.getDepend1().getName());
    }

}
