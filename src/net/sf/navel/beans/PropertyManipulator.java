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
import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Public interface for doing reflection-like dynamic programming of the
 * underlying values of a Navel backed JavaBean.
 * 
 * @author cmdln
 * 
 */
public class PropertyManipulator
{

    private static final Logger LOGGER = LogManager
            .getLogger(PropertyManipulator.class);

    private static final PropertyManipulator SINGLETON = new PropertyManipulator();

    private PropertyManipulator()
    {
        // enforce Singleton pattern
    }

    /**
     * Allows for dynamic programming of a JavaBean's properties. If the bean
     * argument is a Navel bean, it will use the standard validation to ensure
     * correct names and types. If it is a Navel bean, it will also use the same
     * logic as the ProxyFactory to initialize uninitialized nested properties
     * of those are interfaces so can be properly proxied.
     * 
     * @param bean
     *            Target whose property, simply or nested, to set.
     * @param propertyName
     *            Property to set, may be a nested property using the do
     *            notation. Also may be an indexed property.
     * @param propertyValue
     *            Value to set at the specified name.
     */
    public static void put(Object bean, String propertyName,
            Object propertyValue)
    {
        SINGLETON.putValue(bean, propertyName, propertyValue);
    }

    /**
     * This allows direct access to the underlying Map storage to put multiple
     * values all at once. Unline {@see BeanManipulator.populate()}, this
     * method does not call the actually mutators but it does strictly validate
     * the supplied Map to ensure it is consistent with the JavaBean's
     * interfaces.
     * 
     * @param bean
     *            Target bean to apply the Map argument, must be a Navel bean.
     *            To apply a Map to a plain, old JavaBean, use BeanManipulator.
     * @param values
     *            Values to insert into the Navel bean's internal storage, will
     *            be validated with the same rules used by the ProxyFactory
     *            during construction.
     */
    public static void putAll(Object bean, Map<String, Object> values)
    {
        Map<String, Object> copy = new HashMap<String, Object>(values);

        if (null == bean)
        {
            throw new IllegalArgumentException("Bean argument cannot be null!");
        }

        JavaBeanHandler handler = ProxyFactory.getHandler(bean);

        if (null == handler)
        {
            throw new UnsupportedFeatureException(
                    "The bean argument must be a Navel bean, use the BeanManipulator to apply a Map to a plain, old JavaBean.");
        }

        // resolve all nested properties, constructing daughter Navel beans
        // along the way, if needed
        for (BeanInfo beanInfo : handler.proxiedBeanInfo)
        {
            PropertyValueResolver.resolve(beanInfo, copy);
        }

        // this will also validate the Map contents
        handler.propertyValues.putAll(copy);
    }

    /**
     * Useful for removing values from the underlying Map, such as when trying
     * to ignore certain property values like for persistence applications.
     * 
     * @param bean
     *            Bean to clear.
     * @param propertyName
     *            Name of the property to clear.
     * @return Whether the state of the bean was affected.
     */
    public static boolean clear(Object bean, String propertyName)
    {
        JavaBeanHandler handler = ProxyFactory.getHandler(bean);

        if (null == handler)
        {
            throw new IllegalArgumentException("Bean must be a Navel bean!");
        }

        if (!handler.propertyValues.isPropertyOf(propertyName))
        {
            throw new IllegalArgumentException(
                    String
                            .format(
                                    "The property, %1$s, is not a valid one for the bean that implements the types, %2$s.",
                                    propertyName, handler.proxiedInterfaces));
        }

        if (!handler.propertyValues.containsKey(propertyName))
        {
            return true;
        }

        Object value = handler.propertyValues.remove(propertyName);

        return (value != null);
    }

    /**
     * Allows clear of a Navel bean's internal storage.
     * 
     * @param bean
     *            Must be a Navel bean.
     */
    public void clear(Object bean)
    {
        JavaBeanHandler handler = ProxyFactory.getHandler(bean);

        if (null == handler)
        {
            throw new IllegalArgumentException("Bean must be a Navel bean!");
        }

        handler.propertyValues.clear();
    }

    private void putValue(Object bean, String propertyName, Object propertyValue)
    {
        String[] propertyTokens = propertyName.split("\\.");

        if (0 == propertyTokens.length)
        {
            LOGGER.warn("Empty property name.");

            return;
        }

        putValue(propertyTokens, 0, bean, propertyValue);
    }

    private void putValue(String[] propertyTokens, int tokenIndex, Object bean,
            Object propertyValue)
    {
        String propertyName = propertyTokens[tokenIndex];

        PropertyDescriptor propertyDescriptor = AbstractPropertyManipulator
                .findProperty(bean.getClass(), propertyName);

        if (null == propertyDescriptor)
        {
            throw new InvalidPropertyValueException(String.format(
                    "No property descriptor for property name, %1$s.",
                    propertyName));
        }

        AbstractPropertyManipulator manipulator = AbstractPropertyManipulator
                .getPropertyManipulator(propertyDescriptor.getClass());

        if (1 == propertyTokens.length - tokenIndex)
        {
            JavaBeanHandler handler = ProxyFactory.getHandler(bean);

            if (null == handler)
            {
                manipulator.handleWrite(propertyDescriptor, propertyName, bean,
                        propertyValue);
            }
            else
            {
                handler.propertyValues.put(propertyName, propertyValue);
            }

            return;
        }

        Object nestedBean = manipulator.handleRead(propertyDescriptor,
                propertyName, bean);

        if (null == nestedBean)
        {
            LOGGER.warn(String.format(
                    "Nested bean target was null for property name, %1$s.",
                    propertyName));

            Class propClass = propertyDescriptor.getPropertyType();

            if (!propClass.isInterface())
            {
                LOGGER
                        .warn(String
                                .format(
                                        "Nested property, %1$s, must currently be an interface to allow automatic instantiation.  Was of type, %2$s.",
                                        propertyName, propClass.getName()));

                return;
            }

            nestedBean = ProxyFactory.create(propClass);
        }

        // recurse on the nested property
        putValue(propertyTokens, tokenIndex + 1, nestedBean, propertyValue);
    }
}
