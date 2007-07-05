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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
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
public class PropertyValues
{

    private static final Logger LOGGER = LogManager
            .getLogger(PropertyValues.class);

    private final Map<String, Object> values;

    PropertyValues()
    {
        this.values = new HashMap<String, Object>();
    }

    PropertyValues(Map<String, Object> initialValues)
    {
        // JavaBeanHandler already copies the initialValues since it must call
        // PropertyValueResolver
        this.values = initialValues;
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
        // TODO validate propertyName
        // TODO valid value type
        values.put(propertyName, value);
    }

    public void putAll(Map<String, Object> values)
    {
        // TODO validate propertyNames
        // TODO valid value types
        values.putAll(values);
    }

    public Object get(String propertyName)
    {
        return values.get(propertyName);
    }

    Object proxyToObject(final String message, final Method method,
            final Object[] args) throws UnsupportedFeatureException
    {
        String methodName = method.getName();

        int count = (null == args) ? 0 : args.length;

        Class[] argTypes = new Class[count];

        String argString = parseArguments(argTypes);

        // the only method in Object that takes an argument is equals, and it
        // takes another Object as an argument
        for (int i = 0; i < count; i++)
        {
            argTypes[i] = Object.class;
        }

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

    boolean containsKey(String property)
    {
        return values.containsKey(property);
    }

    Object remove(String property)
    {
        return values.remove(property);
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

        // TODO need another mechanism
        // IgnoreToString ignore = proxiedClass
        // .getAnnotation(IgnoreToString.class);
        IgnoreToString ignore = null;

        if (null == ignore)
        {
            return toPrint.toString();
        }

        for (String ignoreName : ignore.value())
        {
            toPrint.remove(ignoreName);
        }

        return toPrint.toString();
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
}
