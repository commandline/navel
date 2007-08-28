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
import java.beans.IndexedPropertyDescriptor;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * The storage class that supports the data bucket behavior for all simple
 * properties of a JavaBean. Expose an interface that allows for programmatic
 * manipulation of the bean contents, safely. That is putting new values by
 * property names are checked to ensure the do not violate the properties the
 * bean exposes through its compile time interfaces.
 * 
 * @author cmdln
 * 
 */
public class PropertyValues implements Serializable
{

    private static final long serialVersionUID = -7835666700165867556L;

    private static final Logger LOGGER = LogManager
            .getLogger(PropertyValues.class);

    final Map<String, PropertyDelegate<?>> propertyDelegates = new HashMap<String, PropertyDelegate<?>>();

    private final Map<String, Object> values;

    private final ProxyDescriptor proxyDescriptor;

    final ObjectProxy objectProxy;

    PropertyValues(ProxyDescriptor proxyDescriptor,
            Map<String, Object> initialValues)
    {
        this.proxyDescriptor = proxyDescriptor;

        Map<String, Object> initialCopy = null == initialValues ? new HashMap<String, Object>()
                : new HashMap<String, Object>(initialValues);

        // this cleans up nested values, if it hits a Navel bean as a nested
        // property it will try to re-use or instantiate as appropriate,
        // including validating the nested bean
        PropertyValueResolver.resolve(proxyDescriptor.propertyDescriptors,
                initialCopy);

        // only validates the direct properties of this bean, but the step above
        // takes care of ensuring nested properties are valid
        PropertyValidator.validateAll(proxyDescriptor.propertyDescriptors,
                initialCopy);

        this.objectProxy = new ObjectProxy(proxyDescriptor);

        this.values = initialCopy;
    }

    PropertyValues(PropertyValues source)
    {
        // NavelDescriptor is immutable so safe to share like this
        this.proxyDescriptor = source.proxyDescriptor;

        this.objectProxy = new ObjectProxy(source.objectProxy);

        this.values = new HashMap<String, Object>(source.values);
    }

    public ProxyDescriptor getProxyDescriptor()
    {
        return proxyDescriptor;
    }

    /**
     * Return a copy of the internal values, generally safe to manipulate. Does
     * not perform a deep copy, however, so be careful of de-referencing nested
     * values on elements of the copy.
     * 
     * @return A shallow copy of the internal values of this instance.
     */
    public Map<String, Object> copyValues(boolean flatten)
    {
        Map<String, Object> copy = new HashMap<String, Object>(values);

        if (flatten)
        {
            BeanManipulator.expandNestedBeans(copy);
        }

        return copy;
    }

    /**
     * Checks that the supplied property name is valid for the JavaBean's
     * compile time property set, that the type matches or can be coerced, and
     * adds the supplied value to internal storage.
     * 
     * @param propertyName
     *            A property in the set of properties introspected when the
     *            associated Proxy and JavaBeanHandler were created by the
     *            ProxyFactory.
     * @param value
     *            Checked for type safety against the appropriate property.
     */
    public void put(String propertyName, Object value)
    {
        String[] propertyTokens = propertyName.split("\\.");

        if (0 == propertyTokens.length)
        {
            LOGGER.warn("Empty property name.");

            return;
        }

        putValue(this, propertyTokens, 0, value);
    }

    /**
     * Forewards to the internal map, after resolving nested and list properties
     * and validating the new map. Will overwrite values at the same keys in the
     * internal storage.
     */
    public void putAll(Map<String, Object> newValues)
    {
        // resolution depends on combining existing and new values, specifically
        // for lists and setting unset values on existing beans
        Map<String, Object> combined = new HashMap<String, Object>(values);
        combined.putAll(newValues);

        PropertyValueResolver.resolve(proxyDescriptor.propertyDescriptors,
                combined);

        // depends on resolution taking care of lists, not presently possible to
        // validate just the new values
        PropertyValidator.validateAll(proxyDescriptor.propertyDescriptors,
                combined);

        // clear out the existing values since the new Map will contain old and
        // new correctly resolved, validated and combined
        values.clear();
        values.putAll(combined);
    }

    public Object get(String propertyName)
    {
        return values.get(propertyName);
    }

    public boolean containsKey(String property)
    {
        return containsKey(this, property);
    }

    public Object remove(String property)
    {
        return values.remove(property);
    }

    public void clear()
    {
        values.clear();
    }

    boolean isPropertyOf(String propertyName)
    {
        return isPropertyOf(this, propertyName);
    }

    static final Map<String, PropertyDescriptor> mapProperties(BeanInfo beanInfo)
    {
        Map<String, PropertyDescriptor> byNames = new HashMap<String, PropertyDescriptor>();

        for (PropertyDescriptor propertyDescriptor : beanInfo
                .getPropertyDescriptors())
        {
            byNames.put(propertyDescriptor.getName(), propertyDescriptor);
        }

        return byNames;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */

    @Override
    public boolean equals(Object obj)
    {
        if (null == obj)
        {
            return false;
        }

        if (!(obj instanceof PropertyValues))
        {
            return false;
        }

        PropertyValues other = (PropertyValues) obj;

        return proxyDescriptor.equals(other.proxyDescriptor)
                && values.equals(other.values);
    }

    /**
     * @see java.lang.Object#hashCode()
     */

    @Override
    public int hashCode()
    {
        int result = 17;

        result = 37 * result + proxyDescriptor.hashCode();

        result = 37 * result + values.hashCode();

        return result;
    }

    boolean isAttached(String propertyName)
    {
        return propertyDelegates.containsKey(propertyName)
                && propertyDelegates.get(propertyName) != null;
    }

    void attach(String propertyName, PropertyDelegate<?> delegate)
    {
        if (propertyDelegates.get(propertyName) != null)
        {
            LOGGER
                    .warn(String
                            .format(
                                    "PropertyDelegate already mapped for property, %1$s, overwriting!",
                                    propertyName));
        }

        PropertyDescriptor propertyDescriptor = proxyDescriptor.propertyDescriptors
                .get(propertyName);

        PropertyValidator.validate(propertyName, propertyDescriptor, delegate);

        propertyDelegates.put(propertyName, delegate);
    }

    boolean detach(String propertyName)
    {
        return propertyDelegates.remove(propertyName) != null;
    }

    Object proxyToObject(String message, Method method, Object[] args)
    {
        return objectProxy.proxy(message, this, method, args);
    }

    private void putValue(PropertyValues propertyValues,
            String[] propertyTokens, int tokenIndex, Object propertyValue)
    {
        String propertyName = propertyTokens[tokenIndex];
        String originalName = propertyName;

        boolean indexedProperty = false;

        if (propertyName.endsWith("]") && propertyName.indexOf('[') != -1)
        {
            propertyName = propertyName.substring(0, propertyName.indexOf('['));

            indexedProperty = true;
        }

        PropertyDescriptor propertyDescriptor = propertyValues.proxyDescriptor.propertyDescriptors
                .get(propertyName);

        if (null == propertyDescriptor)
        {
            throw new InvalidPropertyValueException(String.format(
                    "No property descriptor for property name, %1$s.",
                    propertyName));
        }

        if (1 == propertyTokens.length - tokenIndex)
        {
            if (indexedProperty)
            {
                IndexedPropertyManipulator.putIndexed(propertyValues.values,
                        originalName, propertyName, propertyDescriptor,
                        propertyValue);
            }
            else
            {
                PropertyValidator.validate(
                        propertyValues.proxyDescriptor.propertyDescriptors,
                        propertyName, propertyValue);

                propertyValues.values.put(propertyName, propertyValue);
            }

            return;
        }

        Class<?> propertyType = propertyDescriptor.getPropertyType();

        Object nestedBean = null;

        if (indexedProperty)
        {
            nestedBean = IndexedPropertyManipulator.getIndexed(
                    propertyValues.values, originalName, propertyName,
                    propertyDescriptor, true);
        }
        else
        {
            nestedBean = propertyValues.values.get(propertyName);
        }

        if (null == nestedBean)
        {
            nestedBean = NestedBeanFactory.create(originalName, propertyType);

            if (null == nestedBean)
            {
                return;
            }
        }

        JavaBeanHandler nestedHandler = ProxyFactory.getHandler(nestedBean);

        // IMPROVE consider using BeanManipulator to attempt population
        if (null == nestedHandler)
        {
            LOGGER
                    .warn(String
                            .format(
                                    "Nested property, %1$s, must currently be an interface to allow full de-reference.  Was of type, %2$s.",
                                    propertyName, propertyType.getName()));
            return;
        }

        // recurse on the nested property
        putValue(nestedHandler.propertyValues, propertyTokens, tokenIndex + 1,
                propertyValue);
    }

    private boolean isPropertyOf(PropertyValues propertyValues,
            final String propertyName)
    {
        int dotIndex = propertyName.indexOf('.');

        String shallowProperty = -1 == dotIndex ? propertyName : propertyName
                .substring(0, dotIndex);

        boolean indexedProperty = false;

        if (shallowProperty.endsWith("]") && shallowProperty.indexOf('[') != -1)
        {
            shallowProperty = propertyName.substring(0, propertyName
                    .indexOf('['));

            indexedProperty = true;
        }

        for (PropertyDescriptor propertyDescriptor : propertyValues.proxyDescriptor.propertyDescriptors
                .values())
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
            if (indexedProperty)
            {
                if (!(propertyDescriptor instanceof IndexedPropertyDescriptor))
                {
                    return false;
                }

                IndexedPropertyDescriptor indexedDescriptor = (IndexedPropertyDescriptor) propertyDescriptor;

                return BeanManipulator.isPropertyOf(indexedDescriptor
                        .getIndexedPropertyType(), propertyName
                        .substring(dotIndex + 1));
            }

            Object nestedValue = propertyValues.values.get(shallowProperty);

            JavaBeanHandler nestedHandler = ProxyFactory
                    .getHandler(nestedValue);

            if (null == nestedHandler)
            {
                return BeanManipulator.isPropertyOf(propertyDescriptor
                        .getPropertyType(), propertyName
                        .substring(dotIndex + 1));
            }
            else
            {
                return isPropertyOf(nestedHandler.propertyValues, propertyName
                        .substring(dotIndex + 1));
            }
        }

        return false;
    }

    private boolean containsKey(PropertyValues propertyValues,
            final String propertyName)
    {
        int dotIndex = propertyName.indexOf('.');

        String shallowProperty = -1 == dotIndex ? propertyName : propertyName
                .substring(0, dotIndex);

        boolean indexedProperty = false;

        if (shallowProperty.endsWith("]") && shallowProperty.indexOf('[') != -1)
        {
            shallowProperty = propertyName.substring(0, propertyName
                    .indexOf('['));

            indexedProperty = true;
        }

        // if this is a leafy property, we're done
        if (-1 == dotIndex && !indexedProperty)
        {
            return values.containsKey(shallowProperty);
        }

        Object nestedBean = null;

        if (indexedProperty)
        {
            nestedBean = IndexedPropertyManipulator.getIndexed(
                    propertyValues.values, propertyName, shallowProperty,
                    propertyValues.proxyDescriptor.propertyDescriptors
                            .get(shallowProperty), false);

            if (-1 == dotIndex)
            {
                return nestedBean != null;
            }
        }
        else
        {
            nestedBean = propertyValues.values.get(shallowProperty);
        }

        JavaBeanHandler nestedHandler = ProxyFactory.getHandler(nestedBean);

        if (null == nestedHandler)
        {
            return false;
        }
        else
        {
            return nestedHandler.propertyValues.containsKey(propertyName
                    .substring(dotIndex + 1));
        }
    }

    boolean valuesEqual(PropertyValues propertyValues)
    {
        return this.values.equals(propertyValues.values);
    }
}
