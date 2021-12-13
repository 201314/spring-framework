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

package org.springframework.remoting.rmi;

import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * RMI exporter that exposes the specified service as RMI object with the specified name.
 * Such services can be accessed via plain RMI or via {@link RmiProxyFactoryBean}.
 * Also supports exposing any non-RMI service via RMI invokers, to be accessed via
 * {@link RmiClientInterceptor} / {@link RmiProxyFactoryBean}'s automatic detection
 * of such invokers.
 *
 * <p>With an RMI invoker, RMI communication works on the {@link RmiInvocationHandler}
 * level, needing only one stub for any service. Service interfaces do not have to
 * extend {@code java.rmi.Remote} or throw {@code java.rmi.RemoteException}
 * on all methods, but in and out parameters have to be serializable.
 *
 * <p>The major advantage of RMI, compared to Hessian, is serialization.
 * Effectively, any serializable Java object can be transported without hassle.
 * Hessian has its own (de-)serialization mechanisms, but is HTTP-based and thus
 * much easier to setup than RMI. Alternatively, consider Spring's HTTP invoker
 * to combine Java serialization with HTTP-based transport.
 *
 * <p>Note: RMI makes a best-effort attempt to obtain the fully qualified host name.
 * If one cannot be determined, it will fall back and use the IP address. Depending
 * on your network configuration, in some cases it will resolve the IP to the loopback
 * address. To ensure that RMI will use the host name bound to the correct network
 * interface, you should pass the {@code java.rmi.server.hostname} property to the
 * JVM that will export the registry and/or the service using the "-D" JVM argument.
 * For example: {@code -Djava.rmi.server.hostname=myserver.com}
 *
 * @author Juergen Hoeller
 * @see RmiClientInterceptor
 * @see RmiProxyFactoryBean
 * @see java.rmi.Remote
 * @see java.rmi.RemoteException
 * @see org.springframework.remoting.caucho.HessianServiceExporter
 * @see org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter
 * @since 13.05.2003
 */

/**
 * 服务端实现关键类
 */
public class RmiServiceExporter extends RmiBasedExporter implements InitializingBean, DisposableBean {

    private String serviceName;

    private int servicePort = 0;  // anonymous port

    private RMIClientSocketFactory clientSocketFactory;

    private RMIServerSocketFactory serverSocketFactory;

    private Registry registry;

    private String registryHost;

    private int registryPort = Registry.REGISTRY_PORT;

    private RMIClientSocketFactory registryClientSocketFactory;

    private RMIServerSocketFactory registryServerSocketFactory;

    private boolean alwaysCreateRegistry = false;

    private boolean replaceExistingBinding = true;

    private Remote exportedObject;

    private boolean createdRegistry = false;


    /**
     * Set the name of the exported RMI service,
     * i.e. {@code rmi://host:port/NAME}
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * Set the port that the exported RMI service will use.
     * <p>Default is 0 (anonymous port).
     */
    public void setServicePort(int servicePort) {
        this.servicePort = servicePort;
    }

    /**
     * Set a custom RMI client socket factory to use for exporting the service.
     * <p>If the given object also implements {@code java.rmi.server.RMIServerSocketFactory},
     * it will automatically be registered as server socket factory too.
     *
     * @see #setServerSocketFactory
     * @see java.rmi.server.RMIClientSocketFactory
     * @see java.rmi.server.RMIServerSocketFactory
     * @see UnicastRemoteObject#exportObject(Remote, int, RMIClientSocketFactory, RMIServerSocketFactory)
     */
    public void setClientSocketFactory(RMIClientSocketFactory clientSocketFactory) {
        this.clientSocketFactory = clientSocketFactory;
    }

    /**
     * Set a custom RMI server socket factory to use for exporting the service.
     * <p>Only needs to be specified when the client socket factory does not
     * implement {@code java.rmi.server.RMIServerSocketFactory} already.
     *
     * @see #setClientSocketFactory
     * @see java.rmi.server.RMIClientSocketFactory
     * @see java.rmi.server.RMIServerSocketFactory
     * @see UnicastRemoteObject#exportObject(Remote, int, RMIClientSocketFactory, RMIServerSocketFactory)
     */
    public void setServerSocketFactory(RMIServerSocketFactory serverSocketFactory) {
        this.serverSocketFactory = serverSocketFactory;
    }

    /**
     * Specify the RMI registry to register the exported service with.
     * Typically used in combination with RmiRegistryFactoryBean.
     * <p>Alternatively, you can specify all registry properties locally.
     * This exporter will then try to locate the specified registry,
     * automatically creating a new local one if appropriate.
     * <p>Default is a local registry at the default port (1099),
     * created on the fly if necessary.
     *
     * @see RmiRegistryFactoryBean
     * @see #setRegistryHost
     * @see #setRegistryPort
     * @see #setRegistryClientSocketFactory
     * @see #setRegistryServerSocketFactory
     */
    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    /**
     * Set the host of the registry for the exported RMI service,
     * i.e. {@code rmi://HOST:port/name}
     * <p>Default is localhost.
     */
    public void setRegistryHost(String registryHost) {
        this.registryHost = registryHost;
    }

    /**
     * Set the port of the registry for the exported RMI service,
     * i.e. {@code rmi://host:PORT/name}
     * <p>Default is {@code Registry.REGISTRY_PORT} (1099).
     *
     * @see java.rmi.registry.Registry#REGISTRY_PORT
     */
    public void setRegistryPort(int registryPort) {
        this.registryPort = registryPort;
    }

    /**
     * Set a custom RMI client socket factory to use for the RMI registry.
     * <p>If the given object also implements {@code java.rmi.server.RMIServerSocketFactory},
     * it will automatically be registered as server socket factory too.
     *
     * @see #setRegistryServerSocketFactory
     * @see java.rmi.server.RMIClientSocketFactory
     * @see java.rmi.server.RMIServerSocketFactory
     * @see LocateRegistry#getRegistry(String, int, RMIClientSocketFactory)
     */
    public void setRegistryClientSocketFactory(RMIClientSocketFactory registryClientSocketFactory) {
        this.registryClientSocketFactory = registryClientSocketFactory;
    }

    /**
     * Set a custom RMI server socket factory to use for the RMI registry.
     * <p>Only needs to be specified when the client socket factory does not
     * implement {@code java.rmi.server.RMIServerSocketFactory} already.
     *
     * @see #setRegistryClientSocketFactory
     * @see java.rmi.server.RMIClientSocketFactory
     * @see java.rmi.server.RMIServerSocketFactory
     * @see LocateRegistry#createRegistry(int, RMIClientSocketFactory, RMIServerSocketFactory)
     */
    public void setRegistryServerSocketFactory(RMIServerSocketFactory registryServerSocketFactory) {
        this.registryServerSocketFactory = registryServerSocketFactory;
    }

    /**
     * Set whether to always create the registry in-process,
     * not attempting to locate an existing registry at the specified port.
     * <p>Default is "false". Switch this flag to "true" in order to avoid
     * the overhead of locating an existing registry when you always
     * intend to create a new registry in any case.
     */
    public void setAlwaysCreateRegistry(boolean alwaysCreateRegistry) {
        this.alwaysCreateRegistry = alwaysCreateRegistry;
    }

    /**
     * Set whether to replace an existing binding in the RMI registry,
     * that is, whether to simply override an existing binding with the
     * specified service in case of a naming conflict in the registry.
     * <p>Default is "true", assuming that an existing binding for this
     * exporter's service name is an accidental leftover from a previous
     * execution. Switch this to "false" to make the exporter fail in such
     * a scenario, indicating that there was already an RMI object bound.
     */
    public void setReplaceExistingBinding(boolean replaceExistingBinding) {
        this.replaceExistingBinding = replaceExistingBinding;
    }


    @Override
    public void afterPropertiesSet() throws RemoteException {
        prepare();
    }

    /**
     * 初始化服务
     * Initialize this service exporter, registering the service as RMI object.
     * <p>Creates an RMI registry on the specified port if none exists.
     *
     * @throws RemoteException if service registration failed
     */
    public void prepare() throws RemoteException {
        /**
         * 检查是否配置了服务实现类
         */
        checkService();

        if (this.serviceName == null) {
            throw new IllegalArgumentException("Property 'serviceName' is required");
        }

        //如果用户在配置文件中配置了clientSocketFactory 或者 serverSocketFactory的处理
        /**
         *如果配置中的clientSocketFactory同时实现RMIClientSocketFactory、RMIServerSocketFactory，那么接口会忽略
         *配置中的serverSocketFactory而使用clientSocketFactory代替
         */
        // Check socket factories for exported object.
        if (this.clientSocketFactory instanceof RMIServerSocketFactory) {
            this.serverSocketFactory = (RMIServerSocketFactory) this.clientSocketFactory;
        }
        /**
         * clientSocketFactory 和 serverSocketFactory 要么同时出现、要么都不出现
         */
        if ((this.clientSocketFactory != null && this.serverSocketFactory == null) ||
            (this.clientSocketFactory == null && this.serverSocketFactory != null)) {
            throw new IllegalArgumentException(
                "Both RMIClientSocketFactory and RMIServerSocketFactory or none required");
        }

        /**
         *如果配置中的registryClientSocketFactory同时实现RMIClientSocketFactory、RMIServerSocketFactory，那么接口会忽略
         *配置中的registryServerSocketFactory而使用registryClientSocketFactory代替
         */
        // Check socket factories for RMI registry.
        if (this.registryClientSocketFactory instanceof RMIServerSocketFactory) {
            this.registryServerSocketFactory = (RMIServerSocketFactory) this.registryClientSocketFactory;
        }
        /**
         * 不允许出现只配置registryServerSocketFactory却没有配置registryclientSocketFactory的情况出现
         * 必须同时配置或不配置
         */
        if (this.registryClientSocketFactory == null && this.registryServerSocketFactory != null) {
            throw new IllegalArgumentException(
                "RMIServerSocketFactory without RMIClientSocketFactory for registry not supported");
        }

        this.createdRegistry = false;

        /**
         * 确定RMI registry
         */
        // Determine RMI registry to use.
        if (this.registry == null) {
            this.registry = getRegistry(this.registryHost, this.registryPort,
                this.registryClientSocketFactory, this.registryServerSocketFactory);
            this.createdRegistry = true;
        }

        /**
         * 初始化以及缓存导出的object
         * 此时通常情况下是使用RMIInvocationwrapper 封装的JDK 代理类，切面为RemoteInvocationTraceInterceptor
         */
        // Initialize and cache exported object.
        this.exportedObject = getObjectToExport();

        if (logger.isDebugEnabled()) {
            logger.debug("Binding service '" + this.serviceName + "' to RMI registry: " + this.registry);
        }

        // Export RMI object.
        if (this.clientSocketFactory != null) {
            /**
             *使用由给定的套接字工厂指定的传送方式导出远程对象，以便能够接收传入的调用。
             *clientSocketFactory：进行远程对象调用的客户端套接字工厂
             *serverSocketFactory：接收远程调用的服务端套接字工厂
             */
            UnicastRemoteObject.exportObject(
                this.exportedObject, this.servicePort, this.clientSocketFactory, this.serverSocketFactory);
        } else {
            //导出remote object，以使它能接收特定端口的调用
            UnicastRemoteObject.exportObject(this.exportedObject, this.servicePort);
        }

        // Bind RMI object to registry.
        try {
            if (this.replaceExistingBinding) {
                this.registry.rebind(this.serviceName, this.exportedObject);
            } else {
                /**
                 * 绑定服务名称到remote object，外界调用servicelName的时候会被exportedObject接收
                 */
                this.registry.bind(this.serviceName, this.exportedObject);
            }
        } catch (AlreadyBoundException ex) {
            // Already an RMI object bound for the specified service name...
            unexportObjectSilently();
            throw new IllegalStateException(
                "Already an RMI object bound for name '" + this.serviceName + "': " + ex.toString());
        } catch (RemoteException ex) {
            // Registry binding failed: let's unexport the RMI object as well.
            unexportObjectSilently();
            throw ex;
        }
    }


    /**
     * Locate or create the RMI registry for this exporter.
     *
     * @param registryHost        the registry host to use (if this is specified,
     *                            no implicit creation of a RMI registry will happen)
     * @param registryPort        the registry port to use
     * @param clientSocketFactory the RMI client socket factory for the registry (if any)
     * @param serverSocketFactory the RMI server socket factory for the registry (if any)
     * @return the RMI registry
     * @throws RemoteException if the registry couldn't be located or created
     */
    protected Registry getRegistry(String registryHost, int registryPort,
                                   @Nullable RMIClientSocketFactory clientSocketFactory, @Nullable RMIServerSocketFactory serverSocketFactory)
        throws RemoteException {

        if (registryHost != null) {
            // 远程连接测试
            // Host explicitly specified: only lookup possible.
            if (logger.isDebugEnabled()) {
                logger.debug("Looking for RMI registry at port '" + registryPort + "' of host [" + registryHost + "]");
            }
            Registry reg = LocateRegistry.getRegistry(registryHost, registryPort, clientSocketFactory);
            testRegistry(reg);
            return reg;
        } else {
            /**
             * 获取本机的Registry
             */
            return getRegistry(registryPort, clientSocketFactory, serverSocketFactory);
        }
    }

    /**
     * Locate or create the RMI registry for this exporter.
     *
     * @param registryPort        the registry port to use
     * @param clientSocketFactory the RMI client socket factory for the registry (if any)
     * @param serverSocketFactory the RMI server socket factory for the registry (if any)
     * @return the RMI registry
     * @throws RemoteException if the registry couldn't be located or created
     */
    protected Registry getRegistry(int registryPort,
                                   @Nullable RMIClientSocketFactory clientSocketFactory, @Nullable RMIServerSocketFactory serverSocketFactory)
        throws RemoteException {
        /**
         * 之前的判断也保证了clientSocketFactory与serverSocketFactory要么同时出现，要么同时不出现，所以这里只对clientSocketFactory是否为空进行了判断。
         */
        if (clientSocketFactory != null) {
            if (this.alwaysCreateRegistry) {
                /**
                 * 在获取Registry实例时会首先测试是否已经建立了对指定端口的连接，如果已经建立则复用已经创建的实例，否则重新创建。
                 */
                logger.debug("Creating new RMI registry");
                return LocateRegistry.createRegistry(registryPort, clientSocketFactory, serverSocketFactory);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Looking for RMI registry at port '" + registryPort + "', using custom socket factory");
            }
            synchronized (LocateRegistry.class) {
                try {
                    // 复用测试
                    // Retrieve existing registry.
                    Registry reg = LocateRegistry.getRegistry(null, registryPort, clientSocketFactory);
                    testRegistry(reg);
                    return reg;
                } catch (RemoteException ex) {
                    logger.trace("RMI registry access threw exception", ex);
                    logger.debug("Could not detect RMI registry - creating new one");
                    // Assume no registry found -> create new one.
                    return LocateRegistry.createRegistry(registryPort, clientSocketFactory, serverSocketFactory);
                }
            }
        } else {
            return getRegistry(registryPort);
        }
    }

    /**
     * 如果创建Registry实例时不需要使用自定义的套接字工厂，那么就可以直接使用LocateRegistry.createRegistry(..)方法来创建了，当然复用的检测还是必要的。
     * <p>
     * Locate or create the RMI registry for this exporter.
     *
     * @param registryPort the registry port to use
     * @return the RMI registry
     * @throws RemoteException if the registry couldn't be located or created
     */
    protected Registry getRegistry(int registryPort) throws RemoteException {
        if (this.alwaysCreateRegistry) {
            logger.debug("Creating new RMI registry");
            return LocateRegistry.createRegistry(registryPort);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Looking for RMI registry at port '" + registryPort + "'");
        }
        synchronized (LocateRegistry.class) {
            try {
                // Retrieve existing registry.
                Registry reg = LocateRegistry.getRegistry(registryPort);
                testRegistry(reg);
                return reg;
            } catch (RemoteException ex) {
                logger.trace("RMI registry access threw exception", ex);
                logger.debug("Could not detect RMI registry - creating new one");
                // Assume no registry found -> create new one.
                return LocateRegistry.createRegistry(registryPort);
            }
        }
    }

    /**
     * Test the given RMI registry, calling some operation on it to
     * check whether it is still active.
     * <p>Default implementation calls {@code Registry.list()}.
     *
     * @param registry the RMI registry to test
     * @throws RemoteException if thrown by registry methods
     * @see java.rmi.registry.Registry#list()
     */
    protected void testRegistry(Registry registry) throws RemoteException {
        registry.list();
    }


    /**
     * Unbind the RMI service from the registry on bean factory shutdown.
     */
    @Override
    public void destroy() throws RemoteException {
        if (logger.isDebugEnabled()) {
            logger.debug("Unbinding RMI service '" + this.serviceName +
                "' from registry" + (this.createdRegistry ? (" at port '" + this.registryPort + "'") : ""));
        }
        try {
            this.registry.unbind(this.serviceName);
        } catch (NotBoundException ex) {
            if (logger.isInfoEnabled()) {
                logger.info("RMI service '" + this.serviceName + "' is not bound to registry" +
                    (this.createdRegistry ? (" at port '" + this.registryPort + "' anymore") : ""), ex);
            }
        } finally {
            unexportObjectSilently();
        }
    }

    /**
     * Unexport the registered RMI object, logging any exception that arises.
     */
    private void unexportObjectSilently() {
        try {
            UnicastRemoteObject.unexportObject(this.exportedObject, true);
        } catch (NoSuchObjectException ex) {
            if (logger.isInfoEnabled()) {
                logger.info("RMI object for service '" + this.serviceName + "' is not exported anymore", ex);
            }
        }
    }

}
