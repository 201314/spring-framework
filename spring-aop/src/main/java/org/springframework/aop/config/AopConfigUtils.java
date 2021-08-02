/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.config;

import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator;
import org.springframework.aop.framework.autoproxy.InfrastructureAdvisorAutoProxyCreator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for handling registration of AOP auto-proxy creators.
 *
 * <p>Only a single auto-proxy creator should be registered yet multiple concrete
 * implementations are available. This class provides a simple escalation protocol,
 * allowing a caller to request a particular auto-proxy creator and know that creator,
 * <i>or a more capable variant thereof</i>, will be registered as a post-processor.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @see AopNamespaceUtils
 * @since 2.5
 */
public abstract class AopConfigUtils {

    /**
     * The bean name of the internally managed auto-proxy creator.
     */
    public static final String AUTO_PROXY_CREATOR_BEAN_NAME =
            "org.springframework.aop.config.internalAutoProxyCreator";

    /**
     * Stores the auto proxy creator classes in escalation order.
     */
    private static final List<Class<?>> APC_PRIORITY_LIST = new ArrayList<>(3);

    static {
        // Set up the escalation list...
        APC_PRIORITY_LIST.add(InfrastructureAdvisorAutoProxyCreator.class);
        APC_PRIORITY_LIST.add(AspectJAwareAdvisorAutoProxyCreator.class);
        APC_PRIORITY_LIST.add(AnnotationAwareAspectJAutoProxyCreator.class);
    }


    @Nullable
    public static BeanDefinition registerAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
        return registerAutoProxyCreatorIfNecessary(registry, null);
    }

    @Nullable
    public static BeanDefinition registerAutoProxyCreatorIfNecessary(
            BeanDefinitionRegistry registry, @Nullable Object source) {

        return registerOrEscalateApcAsRequired(InfrastructureAdvisorAutoProxyCreator.class, registry, source);
    }

    @Nullable
    public static BeanDefinition registerAspectJAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
        return registerAspectJAutoProxyCreatorIfNecessary(registry, null);
    }

    @Nullable
    public static BeanDefinition registerAspectJAutoProxyCreatorIfNecessary(
            BeanDefinitionRegistry registry, @Nullable Object source) {

        return registerOrEscalateApcAsRequired(AspectJAwareAdvisorAutoProxyCreator.class, registry, source);
    }

    @Nullable
    public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
        return registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry, null);
    }

    @Nullable
    public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(
            BeanDefinitionRegistry registry, @Nullable Object source) {

        return registerOrEscalateApcAsRequired(AnnotationAwareAspectJAutoProxyCreator.class, registry, source);
    }

    /**
     * 强制设置 proxyTargetClass = true
     * <p>
     * proxy-target-class:
     * <p>
     * Spring AOP使用JDK动态代理或者CGLIB来为目标对象创建代理(建议尽量使用JDK的动态代理,面向接口编程+不需要额外增加依赖)。
     * 1. 如果被代理的目标对象至少实现一个接口，则会使用JDK动态代理，所有该目标类型实现的接口都将被代理。
     * 2. 如果目标对象没有实现任何接口，则会使用CGLIB代理。如：希望强制使用CGLIB代理(例如代理目标对象的所有方法，而不只是实现接口的方法)
     * 3. 动态代理无法通知（advise）Final方法，因为它们不能被覆写。
     * <p>
     * JDK动态代理: 代理对象必须是某接口的实现,通过在运行期间创建目标对象接口的实现类来完成代理
     * CBLIB动态代理: 通过在运行期间生成目标对象的子类来完成代理
     *
     * @param registry
     */
    public static void forceAutoProxyCreatorToUseClassProxying(BeanDefinitionRegistry registry) {
        if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
            BeanDefinition definition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
            definition.getPropertyValues().add("proxyTargetClass", Boolean.TRUE);
        }
    }

    /**
     * 强制设置 exposeProxy = true
     * <p>
     * expose-proxy: 有时候目标对象内部的自我调用将无法实施切面中的增强，如下示例：
     * <blockquote><pre>
     * public interface AService{
     *      public void a();
     *      public void b();
     * }
     *
     * @Service
     * public class AServiceImpl1 implements AService{
     *      @Transactional(propagation=Propagation.REQUIRED)
     *      public void a(){
     *          this.b()；
     *      }
     *      @Transactional(propagation=Propagation.REQUIRES_NEW)
     *      public void b(){
     *      }
     * }
     * </pre></blockquote>
     *
     * @Transactional 是通过创建代理来完成事务的。
     * this指向目标对象,因此调用this.b()将不会执行事务增强，因此b方法的事务定义
     * @Transactional(propagation=Propagation.REQUIRES_NEW) 将不会实施，
     * 我们可以这样做解决：
     * 设置 <aop:aspectj-autoproxy expose-proxy="true"/>
     * 再将 this.b(); 修改为 (AService)AopContext.currentProxy()).b(); 即可
     */
    public static void forceAutoProxyCreatorToExposeProxy(BeanDefinitionRegistry registry) {
        if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
            BeanDefinition definition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
            definition.getPropertyValues().add("exposeProxy", Boolean.TRUE);
        }
    }

    @Nullable
    private static BeanDefinition registerOrEscalateApcAsRequired(
            Class<?> cls, BeanDefinitionRegistry registry, @Nullable Object source) {

        Assert.notNull(registry, "BeanDefinitionRegistry must not be null");

        /**
         * 如果已经注册了自动代理创建器 并且与 AnnotationAwareAspectJAutoProxyCreator不一致，那么根据优先级来判断使用哪个
         */
        if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
            BeanDefinition apcDefinition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
            if (!cls.getName().equals(apcDefinition.getBeanClassName())) {
                int currentPriority = findPriorityForClass(apcDefinition.getBeanClassName());
                int requiredPriority = findPriorityForClass(cls);
                if (currentPriority < requiredPriority) {
                    apcDefinition.setBeanClassName(cls.getName());
                }
            }
            return null;
        }

        RootBeanDefinition beanDefinition = new RootBeanDefinition(cls);
        beanDefinition.setSource(source);
        beanDefinition.getPropertyValues().add("order", Ordered.HIGHEST_PRECEDENCE);
        beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        registry.registerBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME, beanDefinition);
        return beanDefinition;
    }

    private static int findPriorityForClass(Class<?> clazz) {
        return APC_PRIORITY_LIST.indexOf(clazz);
    }

    private static int findPriorityForClass(@Nullable String className) {
        for (int i = 0; i < APC_PRIORITY_LIST.size(); i++) {
            Class<?> clazz = APC_PRIORITY_LIST.get(i);
            if (clazz.getName().equals(className)) {
                return i;
            }
        }
        throw new IllegalArgumentException(
                "Class name [" + className + "] is not a known auto-proxy creator class");
    }

}
