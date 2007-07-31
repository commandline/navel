/**
 * Copyright (c) 2003, Thomas Gideon
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above copyright notice,
 *       this list of conditions and the following disclaimer in the documentation
 *       and/or other materials provided with the distribution.
 *
 *     * Neither the name of the Navel project team nor the names of its
 *       contributors may be used to endorse or promote products derived from this
 *       software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.sf.navel.beans;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Invocation handler that supports a dynamic proxy that implements interfaces
 * that follow the JavaBean specification.
 * 
 * @author cmdln
 * 
 */
public class JavaBeanHandler implements InvocationHandler, Serializable
{

    private static final long serialVersionUID = 5765950784911097987L;

    private static final Logger LOGGER = LogManager
            .getLogger(JavaBeanHandler.class);

    private final ProxyDescriptor proxyDescriptor;

    private final PropertyHandler propertyHandler;

    private final MethodHandler methodHandler;

    final PropertyValues propertyValues;

    final InterfaceDelegateMapping delegateMapping;

    /**
     * Used only by the {@see ProxyFactory} and validates that the requested
     * classes can be proxied.
     * 
     * @param initialValues
     *            Values the bean should initially have, checked at
     *            initialization to ensure they are valid.
     * @param proxiedClasses
     *            Elements must be non-null and interfaces, the constructor
     *            validates this.
     * @param delegates
     *            Delegates to wire up initially.
     */
    JavaBeanHandler(Map<String, Object> initialValues,
            Class<?>[] proxiedClasses, InterfaceDelegate[] delegates)
    {
        this.proxyDescriptor = ProxyDescriptor.create(proxiedClasses);

        this.propertyValues = new PropertyValues(proxyDescriptor, initialValues);
        this.propertyHandler = new PropertyHandler(this.propertyValues);

        this.delegateMapping = new InterfaceDelegateMapping(this,
                proxyDescriptor.getProxiedBeanInfo(), delegates, propertyValues);
        this.methodHandler = new MethodHandler(delegateMapping);
    }

    JavaBeanHandler(JavaBeanHandler source)
    {
        // since ProxyDescriptor is immutable, this is a safe assignment
        this.proxyDescriptor = source.proxyDescriptor;
        this.propertyValues = new PropertyValues(source.propertyValues);
        this.propertyHandler = new PropertyHandler(this.propertyValues);
        this.delegateMapping = new InterfaceDelegateMapping(
                this.propertyValues, source.delegateMapping);
        this.methodHandler = new MethodHandler(this.delegateMapping);
    }

    static BeanInfo introspect(Class<?> proxiedInterface)
    {
        try
        {
            return Introspector.getBeanInfo(proxiedInterface);
        }
        catch (IntrospectionException e)
        {
            throw new IllegalArgumentException(String.format(
                    "Cannot introspect interface, %1$s.", proxiedInterface
                            .getName()), e);
        }
    }

    /**
     * Return the first interface provided during the construction of the proxy.
     */
    public Class<?> getPrimaryInterface()
    {
        return proxyDescriptor.getPrimaryType();
    }

    /**
     * Main entry point for the handler.
     * 
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object,
     *      java.lang.reflect.Method, java.lang.Object[])
     */
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable
    {
        Class<?> declaringClass = method.getDeclaringClass();
        String methodName = method.getName();

        if (Object.class.equals(declaringClass))
        {
            if (LOGGER.isInfoEnabled())
            {
                LOGGER
                        .info(String
                                .format(
                                        "Forwarding call for method, %1$s, to internal storage Map.",
                                        method.getName()));
            }

            return propertyValues.proxyToObject("", method, args);
        }

        if (!proxyDescriptor.getProxiedInterfaces().contains(declaringClass))
        {
            throw new IllegalStateException(
                    String
                            .format(
                                    "This proxy does not implement the interface, %1$s, on which the method being invoked, %2$s, is declared!",
                                    declaringClass.getName(), methodName));
        }

        if (PropertyHandler.handles(method))
        {
            if (LOGGER.isInfoEnabled())
            {
                LOGGER.info(String.format(
                        "Handling property access for method, %1$s.",
                        methodName));
            }

            return propertyHandler.handle(proxy, method, args);
        }

        if (methodHandler.handles(method))
        {
            if (LOGGER.isInfoEnabled())
            {
                LOGGER.info(String.format(
                        "Forwarding call for method, %1$s, to delegates.",
                        methodName));
            }

            return methodHandler.handle(proxy, method, args);
        }

        throw new UnsupportedFeatureException(String
                .format("Could not find a usable target for  method, %1$s.",
                        methodName));
    }

    /**
     * Useful for reflecting on the set of interfaces this proxy supports.
     * 
     * @return This set is unmodifiable so is returned directly.
     */
    public Set<Class<?>> getProxiedClasses()
    {
        return proxyDescriptor.getProxiedInterfaces();
    }

    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder("JavaBeanHandler: ");
        buffer.append(proxyDescriptor.getPrimaryType().getName());

        for (Class<?> proxiedInterface : proxyDescriptor.getProxiedInterfaces())
        {
            if (proxyDescriptor.getPrimaryType().equals(proxiedInterface))
            {
                continue;
            }

            buffer.append(", ");
            buffer.append(proxiedInterface.getName());
        }

        return buffer.toString();
    }

    Object copy()
    {
        Class<?>[] copyTypes = new ArrayList<Class<?>>(proxyDescriptor
                .getProxiedInterfaces()).toArray(new Class<?>[proxyDescriptor
                .getProxiedInterfaces().size()]);

        return Proxy.newProxyInstance(ProxyFactory.class.getClassLoader(),
                copyTypes, new JavaBeanHandler(this));
    }

    boolean proxiesFor(Class<?> proxyInterface)
    {
        return proxyDescriptor.getProxiedInterfaces().contains(proxyInterface);
    }
}
