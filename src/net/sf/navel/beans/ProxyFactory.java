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

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import net.sf.navel.example.TypesBean;

/**
 * This is the starting point for working with Navel. It encapsulates the
 * creation of dynamic proxies, constructing them with the supplied delegation
 * pieces and the common data backing code.
 * 
 * @author thomas
 * 
 */
public class ProxyFactory
{

    /**
     * Overload that narrows the new bean down to the primary type of interest
     * and set the initial values.
     * 
     * @param <B>
     *            The preferred return type.
     * @param primaryType
     *            Class argument to fulfill the return type generic parameter.
     * @param additionalTypes
     *            Optional, additional types this object will implement.
     * @return An instance of the primary type that also extends all of the
     *         optionalTypes.
     */
    @SuppressWarnings("unchecked")
    public static <B> B createAs(Class<B> primaryType,
            Class<?>... additionalTypes)
    {
        return ProxyFactory.createAs(primaryType,
                new HashMap<String, Object>(), additionalTypes);
    }

    /**
     * Overload that does not set any initial values.
     * 
     * @param allTypes
     *            All of the interfaces the proxy will implement.
     * @return A proxy that extends all of the specified types and has no
     *         initial property values.
     */
    public static Object create(Class<?>... allTypes)
    {
        return ProxyFactory.create(new HashMap<String, Object>(), allTypes,
                new InterfaceDelegate[0]);
    }

    /**
     * Overload that narrows the new bean down to the primary type of interest
     * and does not set any initial values.
     * 
     * @param <B>
     *            The preferred return type.
     * @param primaryType
     *            Class argument to fulfill the return type generic parameter.
     * @param initialValues
     *            Initial property values the proxy will have, checked to see if
     *            they are valid.
     * @param additionalTypes
     *            Optional, additional types this object will implement.
     * @return An instance of the primary type that also extends all of the
     *         optionalTypes.
     */
    @SuppressWarnings("unchecked")
    public static <B> B createAs(Class<B> primaryType,
            Map<String, Object> initialValues, Class<?>... additionalTypes)
    {
        Class<?>[] allTypes = new Class<?>[additionalTypes.length + 1];

        allTypes[0] = primaryType;
        System.arraycopy(additionalTypes, 0, allTypes, 1,
                additionalTypes.length);

        return (B) ProxyFactory.create(initialValues, allTypes,
                new InterfaceDelegate[0]);
    }

    /**
     * Fully specified factory method for generating a new Navel backed dynamic
     * proxy.
     * 
     * @param initialValues
     *            Initial property values the proxy will have, checked to see if
     *            they are valid.
     * @param allTypes
     *            All of the interfaces the proxy will implement.
     * @param initialDelegates
     *            Delegates to map in initially.
     * @return A proxy that extends all of the specified types and has the
     *         specified initial property values.
     */
    public static Object create(Map<String, Object> initialValues,
            Class<?>[] allTypes, InterfaceDelegate[] initialDelegates)
    {
        if (allTypes.length <= 0)
        {
            throw new IllegalArgumentException(
                    "Must supply at least interface for the proxy to implement!");
        }

        return Proxy.newProxyInstance(allTypes[0].getClassLoader(), allTypes,
                new JavaBeanHandler(initialValues, allTypes, initialDelegates));
    }

    /**
     * Checks to see if there is a delegate for the specified interface.
     * 
     * @param bean
     *            Proxy to check.
     * @param delegatingInterface
     *            Interface to check.
     * @return Whether there is a delegate for the specified interface.
     */
    public static boolean isAttached(Object bean, Class<?> delegatingInterface)
    {
        JavaBeanHandler handler = getHandler(bean);

        if (null == handler)
        {
            throw new UnsupportedFeatureException(
                    "Cannot check a delegate on anything other than a Navel bean!");
        }

        return handler.delegateMapping.isAttached(delegatingInterface);
    }

    /**
     * Checks to see if there is a delegate for the specified property.
     * 
     * @param bean
     *            Proxy to check.
     * @param propertyName
     *            Property to check.
     * @return Whether there is a delegate for the specified property.
     */
    public static boolean isAttached(Object bean, String propertyName)
    {
        JavaBeanHandler handler = getHandler(bean);

        if (null == handler)
        {
            throw new UnsupportedFeatureException(
                    "Cannot check a delegate on anything other than a Navel bean!");
        }

        return handler.propertyValues.isAttached(propertyName);
    }

    /**
     * Utility method that exposes the runtime delegation attachment code.
     * 
     * @param bean
     *            Target to which the delegate will be attached, if applicable.
     * @param delegate
     *            Delegate to attach.
     */
    public static void attach(Object bean, InterfaceDelegate delegate)
    {
        JavaBeanHandler handler = getHandler(bean);

        if (null == handler)
        {
            throw new UnsupportedFeatureException(
                    "Cannot attach a delegate to anything other than a Navel bean!");
        }

        handler.delegateMapping.attach(delegate);
    }

    /**
     * Utility method that exposes the runtime delegation attachment code.
     * 
     * @param bean
     *            Target to which the delegate will be attached, if applicable.
     * @param propertyName
     *            Property this delegate should support.
     * @param delegate
     *            Delegate to attach.
     */
    public static void attach(Object bean, String propertyName,
            PropertyDelegate delegate)
    {
        JavaBeanHandler handler = getHandler(bean);

        if (null == handler)
        {
            throw new UnsupportedFeatureException(
                    "Cannot attach a delegate to anything other than a Navel bean!");
        }

        handler.propertyValues.attach(propertyName, delegate);
    }

    /**
     * Removes a delegate, if any, mapped to the specific interface. Useful, for
     * instance, to participate in a State or Strategy pattern where at
     * different times or under different conditions different delegates, or
     * none at all, might be desirable.
     * 
     * @param bean
     *            Bean from which the delegate should be detached.
     * @param delegatingInterface
     *            If there is a delegate for this interface, remove it.
     * @return Indicate whether a delegate was removed.
     */
    public static boolean detach(Object bean, Class<?> delegatingInterface)
    {
        JavaBeanHandler handler = getHandler(bean);

        if (null == handler)
        {
            throw new UnsupportedFeatureException(
                    "Cannot detach a delegate from anything other than a Navel bean!");
        }

        return handler.delegateMapping.detach(delegatingInterface);
    }

    /**
     * Removes a delegate, if any, mapped to the specific property. Useful, for
     * instance, to participate in a State or Strategy pattern where at
     * different times or under different conditions different delegates, or
     * none at all, might be desirable.
     * 
     * @param bean
     *            Bean from which the delegate should be detached.
     * @param propertyName
     *            If there is a delegate for this property, remove it.
     * @return Indicate whether a delegate was removed.
     */
    public static boolean detach(Object bean, String propertyName)
    {
        JavaBeanHandler handler = getHandler(bean);

        if (null == handler)
        {
            throw new UnsupportedFeatureException(
                    "Cannot detach a delegate from anything other than a Navel bean!");
        }

        return handler.propertyValues.detach(propertyName);
    }

    /**
     * A convenience method to get the underlying Navel bean handler, if the
     * passed in object is a Navel bean. The bean argument is tested and if any
     * of the tests to figure out if it is a Navel bean fail, null is returned.
     * 
     * @param bean
     *            Object to test for being a Navel bean.
     * @return Null if the bean argument is not a Navel bean, otherwise, the
     *         Navel handler for the bean proxy.
     */
    public static JavaBeanHandler getHandler(Object bean)
    {
        if (null == bean || !Proxy.isProxyClass(bean.getClass()))
        {
            return null;
        }

        Object handler = Proxy.getInvocationHandler(bean);

        if (!(handler instanceof JavaBeanHandler))
        {
            return null;
        }

        JavaBeanHandler beanHandler = (JavaBeanHandler) handler;

        return beanHandler;
    }
}
