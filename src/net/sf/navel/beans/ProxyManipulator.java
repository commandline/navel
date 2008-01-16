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
public class ProxyManipulator
{

    private static final ProxyManipulator SINGLETON = new ProxyManipulator();

    private ProxyManipulator()
    {
        // enforce Singleton pattern
    }

    /**
     * Allows dynamic programming of the internal storage of a JavaBeanHandler,
     * useful for translation between Navel beans and other kinds of objects or
     * declarative interrogation of the state of the bean.
     * 
     * <em>WARNING:</em> This will bypass any attached
     * {@link PropertyDelegate} instances!
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
        SINGLETON.assertValidBean(bean);

        JavaBeanHandler handler = SINGLETON.getRequiredHandler(bean);

        PropertyValues propertyValues = handler.propertyValues;

        if (!propertyValues.isPropertyOf(propertyName))
        {
            throw new IllegalArgumentException(
                    String
                            .format(
                                    "The property, %1$s, is not a valid one for this proxy, %2$s.",
                                    propertyName, handler));
        }

        return propertyValues.get(propertyName);
    }

    /**
     * Overload that assumes false for flattening of the internal values.
     * 
     * <em>WARNING:</em> This will bypass any attached
     * {@link PropertyDelegate} instances!
     * 
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
     * <em>WARNING:</em> This will bypass any attached
     * {@link PropertyDelegate} instances!
     * 
     * @param bean
     *            Must be navel bean.
     * @param flatten
     *            If true, then the nested proxies will be flattened into the
     *            output.
     * 
     * @return A shallow copy of the bean's internal state.
     */
    public static Map<String, Object> copyAll(Object bean, boolean flatten)
    {
        SINGLETON.assertValidBean(bean);

        JavaBeanHandler handler = SINGLETON.getRequiredHandler(bean);

        return handler.propertyValues.copyValues(flatten);
    }

    /**
     * Allows for dynamic programming of a JavaBean's properties. If the bean
     * argument is a Navel bean, it will use the standard validation to ensure
     * correct names and types. If it is a Navel bean, it will also use the same
     * logic as the ProxyFactory to initialize uninitialized nested properties
     * of those are interfaces so can be properly proxied.
     * 
     * <em>WARNING:</em> This will bypass any attached
     * {@link PropertyDelegate} instances!
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
        SINGLETON.assertValidBean(bean);

        JavaBeanHandler handler = SINGLETON.getRequiredHandler(bean);

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
     * <em>WARNING:</em> This will bypass any attached
     * {@link PropertyDelegate} instances!
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
        SINGLETON.assertValidBean(bean);

        JavaBeanHandler handler = SINGLETON.getRequiredHandler(bean);

        // this will copy, resolve, and validate the Map contents
        handler.propertyValues.putAll(values);
    }

    /**
     * Check to see if the specified property has an entry, even one with a null
     * value, in the storage map.
     * 
     * <em>WARNING:</em> This will bypass any attached
     * {@link PropertyDelegate} instances!
     * 
     * @param bean
     *            Object to check, must be a Navel bean.
     * @param propertyName
     *            Property to check, must be valid for the Navel bean.
     * @return Whether the internal storage has an entry for this property.
     */
    public static boolean isSet(Object bean, String propertyName)
    {
        SINGLETON.assertValidBean(bean);

        JavaBeanHandler handler = SINGLETON.getRequiredHandler(bean);

        SINGLETON.assertPropertyOf(handler, propertyName);

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
        SINGLETON.assertValidBean(bean);

        JavaBeanHandler handler = SINGLETON.getRequiredHandler(bean);

        SINGLETON.assertPropertyOf(handler, propertyName);

        if (!handler.propertyValues.containsKey(propertyName))
        {
            return false;
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
        SINGLETON.assertValidBean(bean);

        JavaBeanHandler handler = SINGLETON.getRequiredHandler(bean);

        handler.propertyValues.clear();
    }
    
    public static Object resolve(Object bean, String propertyExpression)
    {
        SINGLETON.assertValidBean(bean);

        JavaBeanHandler handler = SINGLETON.getRequiredHandler(bean);

        return handler.propertyValues.resolve(propertyExpression);
    }

    /**
     * Overload that assumes false for flattening out nested proxies.
     * 
     * @param bean
     *            Proxy to resolve.
     * @return A {@link Map} containing the results of calling
     *         {@link PropertyDelegate#get(PropertyValues, String)} for all the
     *         registered instances, empty if no delegates are registered.
     */
    public static Map<String, Object> resolveAll(Object bean)
    {
        return ProxyManipulator.resolveAll(bean, false);
    }

    /**
     * Utility to resolve the values for delegates properties since
     * {@link #copyAll(Object)} ignores any attached {@link PropertyDelegate}
     * instances. This methid does not currently resolve nested beans.
     * 
     * @param bean
     *            Proxy to resolve.
     * @param resolveNeste
     *            If true, the nested proxies will also be resolved and
     *            flattened into the output.
     * @return A {@link Map} containing the results of calling
     *         {@link PropertyDelegate#get(PropertyValues, String)} for all the
     *         registered instances, empty if no delegates are registered.
     */
    public static Map<String, Object> resolveAll(Object bean,
            boolean resolveNested)
    {
        SINGLETON.assertValidBean(bean);

        JavaBeanHandler handler = SINGLETON.getRequiredHandler(bean);

        return handler.propertyValues.resolveDelegates(resolveNested);
    }

    /**
     * Compares just the internal storage of two Navel beans for equivalence.
     * This is similar to the original equals semantic in Navel2.
     * 
     * @param oneBean
     *            One bean to compare.
     * @param anotherBean
     *            The other bean to compare.
     * @return Whether the storage of the two beans is equivalent.
     */
    public static boolean valuesEqual(Object oneBean, Object anotherBean)
    {
        JavaBeanHandler oneHandler = ProxyFactory.getHandler(oneBean);
        JavaBeanHandler anotherHandler = ProxyFactory.getHandler(anotherBean);

        if (null == oneHandler || null == anotherHandler)
        {
            throw new IllegalArgumentException(
                    "Both beans must be a Navel beans!");
        }

        return oneHandler.propertyValues
                .valuesEqual(anotherHandler.propertyValues);
    }
    
    /**
     * Find any lists populated with Navel proxies and expand them.
     * 
     * @param values A {@link Map} that may have lists in its entries.
     */
    public void expandLists(Map<String,Object> values)
    {
        PropertyValuesExpander.expandLists(values);
    }
    
    /**
     * Find any arrays populated with Navel proxies and expand them.
     * 
     * @param values A {@link Map} that may have arrays in its entries.
     */
    public void expandArrays(Map<String,Object> values)
    {
        PropertyValuesExpander.expandArrays(values);
    }

    private JavaBeanHandler getRequiredHandler(Object bean)
    {
        JavaBeanHandler handler = ProxyFactory.getHandler(bean);

        if (null == handler)
        {
            throw new IllegalArgumentException("Bean must be a Navel bean!");
        }

        return handler;
    }

    private void assertValidBean(Object bean)
    {
        if (null != bean)
        {
            return;
        }

        throw new IllegalArgumentException("Bean argument cannot be null!");
    }

    private void assertPropertyOf(JavaBeanHandler handler, String propertyName)
    {
        if (handler.propertyValues.isPropertyOf(propertyName))
        {
            return;
        }

        throw new InvalidExpressionException(
                String
                        .format(
                                "The property expression, %1$s, is not a valid one for this proxy, %2$s.",
                                propertyName, handler));
    }
}
