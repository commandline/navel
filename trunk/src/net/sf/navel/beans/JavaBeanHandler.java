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
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectInputValidation;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
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
public class JavaBeanHandler implements InvocationHandler, Serializable,
        ObjectInputValidation
{

    private static final long serialVersionUID = 5765950784911097987L;

    private static final Logger LOGGER = LogManager
            .getLogger(JavaBeanHandler.class);

    private transient Set<BeanInfo> proxiedBeanInfo;

    private final Set<Class<?>> proxiedInterfaces;

    private final PropertyHandler propertyHandler;

    private final MethodHandler methodHandler;

    final PropertyValues propertyValues;

    final DelegateMapping delegateMapping;

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
        Set<Class<?>> tempClasses = new HashSet<Class<?>>(proxiedClasses.length);
        Set<BeanInfo> tempInfo = new HashSet<BeanInfo>(proxiedClasses.length);

        String primaryClassName = mapTypes(proxiedClasses, tempInfo,
                tempClasses);

        this.proxiedInterfaces = Collections.unmodifiableSet(tempClasses);
        this.proxiedBeanInfo = Collections.unmodifiableSet(tempInfo);
        this.propertyValues = new PropertyValues(primaryClassName,
                proxiedBeanInfo, initialValues);
        this.propertyHandler = new PropertyHandler(this.propertyValues);
        this.delegateMapping = new DelegateMapping(proxiedBeanInfo, delegates,
                propertyValues);
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

        if (!proxiedInterfaces.contains(declaringClass))
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

    public void validateObject() throws InvalidObjectException
    {
        Set<BeanInfo> tempInfo = new HashSet<BeanInfo>();

        for (Class<?> proxiedInterface : proxiedInterfaces)
        {
            BeanInfo beanInfo = JavaBeanHandler.introspect(proxiedInterface);

            tempInfo.add(beanInfo);
        }

        proxiedBeanInfo = Collections.unmodifiableSet(tempInfo);

        propertyValues.restore(proxiedBeanInfo);
    }

    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder();

        for (Class<?> proxiedInterface : proxiedInterfaces)
        {
            if (buffer.length() == 0)
            {
                buffer.append(", ");
            }

            buffer.append(proxiedInterface.getName());
        }

        return "JavaBeanHandler: " + buffer.toString();
    }

    boolean proxiesFor(Class<?> proxyInterface)
    {
        return proxiedInterfaces.contains(proxyInterface);
    }

    private final void readObject(ObjectInputStream input) throws IOException,
            ClassNotFoundException
    {
        input.defaultReadObject();

        input.registerValidation(this, 0);
    }

    private final String mapTypes(Class<?>[] proxiedClasses,
            Set<BeanInfo> tempInfo, Set<Class<?>> tempClasses)
    {
        String primaryClassName = null;

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

            if (null == primaryClassName)
            {
                primaryClassName = proxiedInterface.getName();
            }

            BeanInfo beanInfo = JavaBeanHandler.introspect(proxiedInterface);

            tempInfo.add(beanInfo);

            forbidEvents(beanInfo);

            tempClasses.add(proxiedInterface);

            // recurse any interfaces this one extends
            mapTypes(proxiedInterface.getInterfaces(), tempInfo, tempClasses);
        }

        return primaryClassName;
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
