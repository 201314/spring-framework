/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.instrument.classloading;

import java.lang.instrument.ClassFileTransformer;

/**
 * 静态AOP代理
 * <p>
 * AOP的静态代理主要是在虚拟机启动时通过改变目标对象字节码的方式来完成对目标对象的增强，它与动态代理相比具有更高的效率，
 * 因为在动态代理调用的过程中，还需要一个动态创建代理类并代理目标对象的步骤，
 * 而静态代理则是在启动时便完成了字节码增强，当系统再次调用目标类时与调用正常的类并无差别，所以在效率上会相对高些。
 * <p>
 * Defines the contract for adding one or more
 * {@link ClassFileTransformer ClassFileTransformers} to a {@link ClassLoader}.
 *
 * <p>Implementations may operate on the current context {@code ClassLoader}
 * or expose their own instrumentable {@code ClassLoader}.
 *
 * @author Rod Johnson
 * @author Costin Leau
 * @see java.lang.instrument.ClassFileTransformer
 * @since 2.0
 */
public interface LoadTimeWeaver {

    /**
     * Add a {@code ClassFileTransformer} to be applied by this
     * {@code LoadTimeWeaver}.
     *
     * @param transformer the {@code ClassFileTransformer} to add
     */
    void addTransformer(ClassFileTransformer transformer);

    /**
     * Return a {@code ClassLoader} that supports instrumentation
     * through AspectJ-style load-time weaving based on user-defined
     * {@link ClassFileTransformer ClassFileTransformers}.
     * <p>May be the current {@code ClassLoader}, or a {@code ClassLoader}
     * created by this {@link LoadTimeWeaver} instance.
     *
     * @return the {@code ClassLoader} which will expose
     * instrumented classes according to the registered transformers
     */
    ClassLoader getInstrumentableClassLoader();

    /**
     * Return a throwaway {@code ClassLoader}, enabling classes to be
     * loaded and inspected without affecting the parent {@code ClassLoader}.
     * <p>Should <i>not</i> return the same instance of the {@link ClassLoader}
     * returned from an invocation of {@link #getInstrumentableClassLoader()}.
     *
     * @return a temporary throwaway {@code ClassLoader}; should return
     * a new instance for each call, with no existing state
     */
    ClassLoader getThrowawayClassLoader();

}
