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
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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

    private final String primaryClassName;

    // TODO restore during derserialization
    private transient final Map<String, PropertyDescriptor> propertyDescriptors;

    private final Map<String, Object> values;

    private final Set<String> filterToString;

    PropertyValues(String primaryClassName, Set<BeanInfo> proxiedBeanInfo,
            Map<String, Object> initialValues)
    {
        this.primaryClassName = primaryClassName;

        Map<String, PropertyDescriptor> tempProperties = new HashMap<String, PropertyDescriptor>();

        Map<String, Object> initialCopy = new HashMap<String, Object>(
                initialValues);

        Set<String> tempFilter = new HashSet<String>();

        for (BeanInfo beanInfo : proxiedBeanInfo)
        {
            tempProperties.putAll(mapProperties(beanInfo));

            IgnoreToString ignore = beanInfo.getBeanDescriptor().getBeanClass()
                    .getAnnotation(IgnoreToString.class);

            if (null != ignore)
            {
                tempFilter.addAll(Arrays.asList(ignore.value()));
            }
        }

        PropertyValueResolver.resolve(tempProperties, initialCopy);

        PropertyValidator.validateAll(tempProperties, initialCopy);

        this.propertyDescriptors = Collections.unmodifiableMap(tempProperties);

        this.filterToString = Collections.unmodifiableSet(tempFilter);

        this.values = initialCopy;
    }

    /**
     * Return a copy of the internal values, generally safe to manipulate. Does
     * not perform a deep copy, however, so be careful of de-referencing nested
     * values on elements of the copy.
     * 
     * @return A shallow copy of the internal values of this instance.
     */
    public Map<String, Object> copyValues()
    {
        return new HashMap<String, Object>(values);
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
        Map<String, Object> copy = new HashMap<String, Object>(newValues);

        PropertyValueResolver.resolve(propertyDescriptors, copy);

        PropertyValidator.validateAll(propertyDescriptors, copy);

        values.putAll(copy);
    }

    public Object get(String propertyName)
    {
        return values.get(propertyName);
    }

    public boolean containsKey(String property)
    {
        return values.containsKey(property);
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
        int dotIndex = propertyName.indexOf('.');

        String shallowProperty = -1 == dotIndex ? propertyName : propertyName
                .substring(0, dotIndex);

        for (PropertyDescriptor propertyDescriptor : propertyDescriptors
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
            Object nestedValue = values.get(shallowProperty);

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
                return nestedHandler.propertyValues.isPropertyOf(propertyName
                        .substring(dotIndex + 1));
            }
        }

        return false;
    }

    Object proxyToObject(final String message, final Method method,
            final Object[] args) throws UnsupportedFeatureException
    {
        String methodName = method.getName();

        int count = (null == args) ? 0 : args.length;

        Class[] argTypes = new Class[count];

        // the only method in Object that takes an argument is equals, and it
        // takes another Object as an argument
        for (int i = 0; i < count; i++)
        {
            argTypes[i] = Object.class;
        }

        String argString = parseArguments(argTypes);

        if (LOGGER.isDebugEnabled())
        {
            LOGGER
                    .debug(String
                            .format(
                                    "Proxying method, %1$s, with arguments (%2$s) to underlying Map.",
                                    methodName, argString));
        }

        if ("toString".equals(method.getName()) && argTypes.length == 0)
        {
            return filteredToString();
        }

        try
        {
            // use Object so anything else causes an exception--this is merely
            // a convenience so we don't have to implement the usual object
            // methods directly
            Object.class.getDeclaredMethod(method.getName(), argTypes);

            // need to handle equals a little differently, somewhere
            // between the proxy and the underlying map, based on experience
            if ("equals".equals(method.getName()))
            {
                return handleEquals(args[0]);
            }

            return method.invoke(values, args);
        }
        catch (NoSuchMethodException e)
        {
            throw new UnsupportedFeatureException(
                    String
                            .format(
                                    "Could not find a usable target for  method, %1$s, with arguments (%2$s).%3$s",
                                    methodName, argString, message));
        }
        catch (IllegalAccessException e)
        {
            LOGGER
                    .warn("Illegal access proxying Object methods to internal Map.");

            return null;
        }
        catch (InvocationTargetException e)
        {
            LOGGER
                    .warn("Error invoking while proxying Object methods to internal Map.");

            return null;
        }
    }

    static final Map<String, PropertyDescriptor> mapProperties(
            BeanInfo beanInfo)
    {
        Map<String, PropertyDescriptor> byNames = new HashMap<String, PropertyDescriptor>();

        for (PropertyDescriptor propertyDescriptor : beanInfo
                .getPropertyDescriptors())
        {
            byNames.put(propertyDescriptor.getName(), propertyDescriptor);
        }

        return byNames;
    }

    private Boolean handleEquals(Object value)
    {
        if (null == value)
        {
            return Boolean.FALSE;
        }

        // I don't think there is any sensical comparison, if the argument is
        // not a Proxy, too; maybe at some point iterating the defined fields
        // and comparing them individually? Too hard to second guess the bean
        // interface author, I think this is safer until something better occurs
        // to me
        if (!Proxy.isProxyClass(value.getClass()))
        {
            return Boolean.FALSE;
        }

        Object other = Proxy.getInvocationHandler(value);

        if (!(other instanceof JavaBeanHandler))
        {
            return Boolean.FALSE;
        }

        JavaBeanHandler otherHandler = (JavaBeanHandler) other;

        return Boolean.valueOf(values
                .equals(otherHandler.propertyValues.values));
    }

    private String filteredToString()
    {
        // create a shallow map to filter out ignored properties, as well as to
        // consistently sort by the property names
        Map<String, Object> toPrint = new TreeMap<String, Object>(values);

        if (filterToString.isEmpty())
        {
            return primaryClassName + ": " + toPrint.toString();
        }

        for (String ignoreName : filterToString)
        {
            toPrint.remove(ignoreName);
        }

        return primaryClassName + ": " + toPrint.toString();
    }

    private String parseArguments(Class[] argTypes)
    {
        if (null == argTypes)
        {
            return "";
        }

        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < argTypes.length; i++)
        {
            if (i != 0)
            {
                buffer.append(", ");
            }

            buffer.append(argTypes[i].getName());
        }

        return buffer.toString();
    }

    private void putValue(PropertyValues propertyValues,
            String[] propertyTokens, int tokenIndex, Object propertyValue)
    {
        String propertyName = propertyTokens[tokenIndex];

        PropertyDescriptor propertyDescriptor = propertyDescriptors
                .get(propertyName);

        if (null == propertyDescriptor)
        {
            throw new InvalidPropertyValueException(String.format(
                    "No property descriptor for property name, %1$s.",
                    propertyName));
        }

        if (1 == propertyTokens.length - tokenIndex)
        {
            PropertyValidator.validate(propertyDescriptors, propertyName,
                    propertyValue);

            propertyValues.values.put(propertyName, propertyValue);

            return;
        }

        Class propertyType = propertyDescriptor.getPropertyType();

        Object nestedBean = propertyValues.values.get(propertyName);

        if (null == nestedBean)
        {
            LOGGER.warn(String.format(
                    "Nested bean target was null for property name, %1$s.",
                    propertyName));

            if (!propertyType.isInterface())
            {
                LOGGER
                        .warn(String
                                .format(
                                        "Nested property, %1$s, must currently be an interface to allow automatic instantiation.  Was of type, %2$s.",
                                        propertyName, propertyType.getName()));

                return;
            }

            // TODO add hook for decorating, augmenting creation
            nestedBean = ProxyFactory.create(propertyType);
        }

        JavaBeanHandler nestedHandler = ProxyFactory.getHandler(nestedBean);

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
        putValue(nestedHandler.propertyValues, propertyTokens, tokenIndex + 1, propertyValue);
    }
}
