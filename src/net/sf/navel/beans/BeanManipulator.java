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
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

/**
 * This class allows for generically manipulating the values encapsulated in
 * JavaBeans and Navel beans. Values can be extracted, via Introspection, into a
 * Map and populated, via Introspection, back into any given JavaBean instance
 * from a Map.
 * 
 * Navel and JavaBeans are treated equally with no special handling of unset
 * properties or entries in the Map that do not match any available properties.
 * For more strict treatment of just Navel beans, use the PropertyManipulator.
 * 
 * @author cmdln
 */
public class BeanManipulator
{

    /*----------------------------------------*
     * constants
     *----------------------------------------*/
    private static final Logger LOGGER = Logger
            .getLogger(BeanManipulator.class);

    private static final BeanManipulator SINGLETON = new BeanManipulator();

    private static final Map<String, Object> EMPTY = Collections
            .unmodifiableMap(new HashMap<String, Object>());

    /*----------------------------------------*
     * constructors
     *----------------------------------------*/
    /**
     * Enforces the singleton pattern.
     */
    private BeanManipulator()
    {
        // enforce Singleton pattern
    }

    /*----------------------------------------*
     * class methods
     *----------------------------------------*/
    /**
     * Overload that assumes false for the resolve nested argument.
     */
    public static Map<String, Object> describe(Object bean)
    {
        return SINGLETON.describeBean(bean, false);
    }

    /**
     * Extracts the values from the bean argument into a Map keyed by property
     * names. If using PropertyBeanHandler, you can safely skip this method,
     * using getValues() instead.
     * 
     * @param bean
     *            JavaBean to extract from.
     * @param flattenNested
     *            If any of the properties are themselves Navel beans, should we
     *            flatten their properties into the key set of the containing
     *            bean?
     * @return Map of extracted values, never null but may be empty.
     */
    public static Map<String, Object> describe(Object bean,
            boolean flattenNested)
    {
        return SINGLETON.describeBean(bean, flattenNested);
    }

    /**
     * Populates the values from the Map argument into the bean argument. If a
     * property in the Map argument isn't supported in the bean, it is logged
     * but functionally ignored. This method supports nested properties using
     * "dot notation" in the Map argument, meaning "foo.bar" causes a read of
     * the foo property and a write on the resulting object's bar property with
     * the value in the Map for the "foo.bar" key.
     * 
     * @param bean
     *            JavaBean instance to populate.
     * @param values
     *            Values to populate into the bean.
     */
    public static void populate(Object bean, Map<String, Object> values)
    {
        SINGLETON.populateBean(bean, values);
    }

    /**
     * Utility method to recurse into a description map explicitly for a nested
     * property name and dig out the ultimate value.
     * 
     * @param propertyName
     *            Property name, may use dot notation.
     * @param values
     *            Map of possible values, initially the immediate properties of
     *            some bean, but with each recursive call, it will represent a
     *            new layer of bean properties.
     * @return Null or the resolved value.
     */
    public static Object resolveValue(final String propertyName,
            final Map<String, Object> values)
    {
        int dotIndex = propertyName.indexOf(".");

        if (dotIndex == -1)
        {
            return SINGLETON.getNestedBean(propertyName, values);
        }

        String shallowProperty = propertyName.substring(0, dotIndex);

        Object nestedBean = SINGLETON.getNestedBean(shallowProperty, values);

        if (null == nestedBean)
        {
            return null;
        }

        String subName = propertyName.substring(dotIndex + 1, propertyName
                .length());
        Map<String, Object> subValues = SINGLETON.describeBean(nestedBean,
                false);

        return resolveValue(subName, subValues);
    }

    /**
     * Support method to help in dealing with introspection, reflection of
     * JavaBeans.
     * 
     * @param beanType
     *            Target bean type, be careful not to pass the <em>proxy</em>
     *            type from a dynamic proxy backing a bean.
     * @param propertyName
     *            Name of a property to check via introspection.
     * @return Whether the named property belongs to the bean class.
     */
    public static boolean isPropertyOf(Object bean, String propertyName)
    {
        if (null == bean)
        {
            throw new IllegalArgumentException(
                    "Cannot check against a null reference!");
        }

        JavaBeanHandler handler = ProxyFactory.getHandler(bean);

        if (null == handler)
        {
            Class<?> interfaceType = bean.getClass();

            if (isPropertyOf(interfaceType, propertyName))
            {
                return true;
            }

            return false;
        }

        return handler.propertyValues.isPropertyOf(propertyName);
    }

    public static boolean isPropertyOf(Class<?> interfaceType,
            String propertyName)
    {
        int dotIndex = propertyName.indexOf('.');

        String shallowProperty = -1 == dotIndex ? propertyName : propertyName
                .substring(0, dotIndex);

        BeanInfo beanInfo = JavaBeanHandler.introspect(interfaceType);

        for (PropertyDescriptor propertyDescriptor : beanInfo
                .getPropertyDescriptors())
        {
            // keep going if this is not the property we are looking for or
            // a parent property
            if (!propertyDescriptor.getName().equals(shallowProperty))
            {
                continue;
            }

            // if this is a leafy property, we're done
            if (-1 == dotIndex)
            {
                return true;
            }

            // otherwise, recurse on the nested property
            return isPropertyOf(propertyDescriptor.getPropertyType(),
                    propertyName.substring(dotIndex + 1));
        }

        return false;
    }

    static void expandNestedBeans(Map<String, Object> values)
    {
        // to allow modification of the original map
        Set<Entry<String, Object>> entries = new HashSet<Entry<String, Object>>(
                values.entrySet());

        for (Iterator<Entry<String, Object>> entryIter = entries.iterator(); entryIter
                .hasNext();)
        {
            Entry<String, Object> entry = entryIter.next();

            JavaBeanHandler handler = ProxyFactory.getHandler(entry.getValue());

            if (null == handler)
            {
                continue;
            }

            SINGLETON.expandNestedBean(values, entry.getKey(),
                    handler.propertyValues.copyValues(false));
        }
    }

    private Map<String, Object> describeBean(Object bean, boolean flattenNested)
    {
        PropertyDescriptor[] properties = AbstractPropertyManipulator
                .getProperties(bean.getClass());

        if (0 == properties.length)
        {
            LOGGER.warn("No properties found for describe.");

            return EMPTY;
        }

        Map<String, Object> values = new HashMap<String, Object>(
                properties.length);

        for (int i = 0; i < properties.length; i++)
        {
            readProperty(properties[i], bean, values);
        }

        if (flattenNested)
        {
            expandNestedBeans(values);
        }

        return values;
    }

    private void populateBean(Object bean, Map<String, Object> values)
    {
        for (Iterator<Entry<String, Object>> entryIter = values.entrySet()
                .iterator(); entryIter.hasNext();)
        {
            Entry<String, Object> value = entryIter.next();

            String sourceName = value.getKey();

            Object sourceValue = value.getValue();

            writeProperty(bean, sourceName, sourceValue);
        }
    }

    private Object getNestedBean(String propertyName, Map<String, Object> values)
    {
        String shallowProperty = propertyName;

        boolean indexedProperty = false;

        if (shallowProperty.endsWith("]") && shallowProperty.indexOf('[') != -1)
        {
            shallowProperty = propertyName.substring(0, propertyName
                    .indexOf('['));

            indexedProperty = true;
        }

        if (!indexedProperty)
        {
            return values.get(shallowProperty);
        }

        Object array = values.get(shallowProperty);

        if (null == array)
        {
            return null;
        }

        int arrayIndex = IndexedPropertyManipulator.getIndex(propertyName);

        if (PrimitiveSupport.isPrimitiveArray(array.getClass()))
        {
            return PrimitiveSupport.getElement(array, arrayIndex);
        }
        else
        {
            Object[] indexed = (Object[]) array;

            return indexed[arrayIndex];
        }
    }

    private void writeProperty(Object bean, String propertyName,
            Object propertyValue)
    {
        String[] propertyTokens = propertyName.split("\\.");

        if (0 == propertyTokens.length)
        {
            LOGGER.warn("Empty property name.");

            return;
        }

        writeProperty(propertyTokens, 0, bean, propertyValue);
    }

    private void writeProperty(String[] propertyTokens, int tokenIndex,
            Object bean, Object value)
    {
        String propertyName = propertyTokens[tokenIndex];

        PropertyDescriptor property = AbstractPropertyManipulator.findProperty(
                bean.getClass(), propertyName);

        if (null == property)
        {
            LOGGER.warn("No property for " + propertyName + ".");

            return;
        }

        AbstractPropertyManipulator manipulator = AbstractPropertyManipulator
                .getPropertyManipulator(property.getClass());

        if (1 == propertyTokens.length - tokenIndex)
        {
            manipulator.handleWrite(property, propertyName, bean, value);
        }
        else
        {
            Object nestedBean = manipulator.handleRead(property, propertyName,
                    bean);

            if (null == nestedBean)
            {
                LOGGER.warn("Nested bean target was null, " + propertyName);

                return;
            }

            // recurse on the nested property
            writeProperty(propertyTokens, tokenIndex + 1, nestedBean, value);
        }
    }

    private void readProperty(PropertyDescriptor property, Object bean,
            Map<String, Object> values)
    {
        // getClass, implemented in Object, always shows up as a property
        // with the generated BeanInfo, but should be ignored since it is
        // always read only
        if ("class".equals(property.getName()))
        {
            return;
        }

        Method readMethod = property.getReadMethod();

        if (null == readMethod)
        {
            return;
        }

        // just want to get at some shared code, rather than duplicating it
        AbstractPropertyManipulator manipulator = AbstractPropertyManipulator
                .getPropertyManipulator(PropertyDescriptor.class);

        Object value = manipulator.handleRead(property, property.getName(),
                bean);

        if (null != value)
        {
            values.put(property.getName(), value);
        }
    }

    private void expandNestedBean(Map<String, Object> values, String key,
            Map<String, Object> nested)
    {
        for (Iterator<Entry<String, Object>> entryIter = nested.entrySet()
                .iterator(); entryIter.hasNext();)
        {
            Entry<String, Object> entry = entryIter.next();

            values.put(key + "." + entry.getKey(), entry.getValue());
        }

        values.remove(key);
    }
}