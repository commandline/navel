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

import java.util.Map;

/**
 * Public interface for doing reflection-like dynamic programming of the
 * underlying values of a Navel backed JavaBean.
 * 
 * @author cmdln
 * 
 */
public class PropertyManipulator
{

    private PropertyManipulator()
    {
        // enforce Singleton pattern
    }

    /**
     * Allows dynamic programming of the internal storage of a JavaBeanHandler,
     * useful for translation between Navel beans and other kinds of objects or
     * declarative interrogation of the state of the bean.
     * 
     * @param bean
     *            Object to query, must be a Navel bean.
     * @param propertyName
     *            Property to get, may be a nested property using the dot
     *            notation. Also may be an indexed property.
     * @return Whatever value is at the indicated property.
     */
    public static Object get(Object bean, String propertyName)
    {
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

        if (!handler.propertyValues.isPropertyOf(propertyName))
        {
            throw new IllegalArgumentException(
                    String
                            .format(
                                    "The property, %1$s, is not a valid one for this proxy, %2$s.",
                                    propertyName, handler));
        }

        return BeanManipulator.resolveValue(propertyName,
                handler.propertyValues.copyValues(false));
    }
    
    /**
     * Overload that assumes false for flattening of the internal values.
     * @return
     */
    public static Map<String, Object> copyAll(Object bean)
    {
        return copyAll(bean, false);
    }

    /**
     * Allows a copy of the bean's internal state to be retrieve by an arbitrary
     * caller.
     * 
     * @param bean
     *            Must be navel bean.
     * 
     * @return A shallow copy of the bean's internal state.
     */
    public static Map<String, Object> copyAll(Object bean, boolean flatten)
    {
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

        return handler.propertyValues.copyValues(flatten);
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
     *            Property to set, may be a nested property using the dot
     *            notation. Also may be an indexed property.
     * @param propertyValue
     *            Value to set at the specified name.
     */
    public static void put(Object bean, String propertyName,
            Object propertyValue)
    {
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

        // this will resolve, and validate the property value
        handler.propertyValues.put(propertyName, propertyValue);
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

        // this will copy, resolve, and validate the Map contents
        handler.propertyValues.putAll(values);
    }

    /**
     * Check to see if the specified property has an entry, even one with a null
     * value, in the storage map.
     * 
     * @param bean
     *            Object to check, must be a Navel bean.
     * @param propertyName
     *            Property to check, must be valid for the Navel bean.
     * @return Whether the internal storage has an entry for this property.
     */
    public static boolean isSet(Object bean, String propertyName)
    {
        if (null == bean)
        {
            throw new IllegalArgumentException("Bean argument cannot be null!");
        }

        JavaBeanHandler handler = ProxyFactory.getHandler(bean);

        if (null == handler)
        {
            throw new UnsupportedFeatureException("Bean must be a Navel bean!");
        }

        if (!handler.propertyValues.isPropertyOf(propertyName))
        {
            throw new IllegalArgumentException(
                    String
                            .format(
                                    "The property, %1$s, is not a valid one for this proxy, %2$s.",
                                    propertyName, handler));
        }

        return handler.propertyValues.containsKey(propertyName);
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
        if (null == bean)
        {
            throw new IllegalArgumentException("Bean argument cannot be null!");
        }

        JavaBeanHandler handler = ProxyFactory.getHandler(bean);

        if (null == handler)
        {
            throw new UnsupportedFeatureException("Bean must be a Navel bean!");
        }

        if (!handler.propertyValues.isPropertyOf(propertyName))
        {
            throw new IllegalArgumentException(
                    String
                            .format(
                                    "The property, %1$s, is not a valid one for this proxy, %2$s.",
                                    propertyName, handler));
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
    public static void clear(Object bean)
    {
        JavaBeanHandler handler = ProxyFactory.getHandler(bean);

        if (null == handler)
        {
            throw new IllegalArgumentException("Bean must be a Navel bean!");
        }

        handler.propertyValues.clear();
    }
}
