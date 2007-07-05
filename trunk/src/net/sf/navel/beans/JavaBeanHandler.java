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
import java.beans.EventSetDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

    final Set<BeanInfo> proxiedBeanInfo;

    final Set<Class<?>> proxiedInterfaces;

    final PropertyHandler propertyHandler;

    final PropertyValues propertyValues;
    
    final DelegateMapping delegateMapping;

    final MethodHandler methodHandler;

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
            Class<?>[] proxiedClasses, DelegationTarget[] delegates)
    {
        Set<Class<?>> tempClasses = new HashSet<Class<?>>(proxiedClasses.length);
        Set<BeanInfo> tempInfo = new HashSet<BeanInfo>(proxiedClasses.length);

        Map<String, Object> initialCopy = new HashMap<String, Object>(
                initialValues.size());

        for (int i = 0; i < proxiedClasses.length; i++)
        {
            Class<?> proxiedInterface = proxiedClasses[i];

            if (null == proxiedInterface)
            {
                throw new IllegalArgumentException(String.format(
                        "Found a null class at index, %1$d!", i));
            }

            if (!proxiedInterface.isInterface())
            {

                throw new IllegalArgumentException(
                        String
                                .format(
                                        "The class, %1$s, at index, %2$d, is not an interface.  Only interfaces may be proxied.",
                                        proxiedInterface.getName(), i));
            }

            BeanInfo beanInfo = JavaBeanHandler.introspect(proxiedInterface);

            tempInfo.add(beanInfo);

            PropertyValueResolver.resolve(beanInfo, initialCopy);

            forbidEvents(beanInfo);

            tempClasses.add(proxiedInterface);
        }

        this.proxiedInterfaces = Collections.unmodifiableSet(tempClasses);
        this.proxiedBeanInfo = Collections.unmodifiableSet(tempInfo);
        this.propertyValues = new PropertyValues(initialValues);
        this.propertyHandler = new PropertyHandler(this.propertyValues);
        this.delegateMapping = new DelegateMapping(proxiedBeanInfo, delegates, propertyValues);
        this.methodHandler = new MethodHandler(delegateMapping);
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

        if (!proxiedInterfaces.contains(declaringClass))
        {
            throw new IllegalStateException(
                    String
                            .format(
                                    "This proxy does not implement the interface, %1$s, on which the method being invoked, %2$s, is declared!",
                                    declaringClass.getName(), methodName));
        }

        if (propertyHandler.handles(method))
        {
            if (LOGGER.isInfoEnabled())
            {
                LOGGER.info(String.format(
                        "Handling property access for method, %1$s.", methodName));
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

        if (LOGGER.isInfoEnabled())
        {
            LOGGER.info(String.format(
                    "Forwarding call for method, %1$s, to internal storage Map.",
                    method.getName()));
        }

        return propertyValues.proxyToObject("", method, args);
    }

    /**
     * Ensure that for the proxied class, there are no events declared, since
     * PropertyBeanHandler doesn't support events.
     * 
     * @param beanInfo
     *            Introspection data about the proxied class.
     */
    private final void forbidEvents(BeanInfo beanInfo)
            throws UnsupportedFeatureException
    {
        EventSetDescriptor[] events = beanInfo.getEventSetDescriptors();

        if ((null != events) && (0 != events.length))
        {
            throw new UnsupportedFeatureException(
                    "PropertyBeanHandler does not support JavaBeans with events.");
        }
    }
}
