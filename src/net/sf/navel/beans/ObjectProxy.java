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
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Consolidate all the logic to proxy calls to Methods on the Object class
 * through to the internal storage Map of the PropertyValues instance.
 * 
 * @author thomas
 * 
 */
class ObjectProxy implements Serializable
{

    private static final long serialVersionUID = 7779547801041690485L;

    private static final Logger LOGGER = LogManager
            .getLogger(ObjectProxy.class);

    private static final String DEFAULT_TO_STRING_TEMPLATE = "Navelbean: %1$s, values = %2$s";

    private static String toStringTemplate = DEFAULT_TO_STRING_TEMPLATE;

    private final ProxyDescriptor proxyDescriptor;

    private final Set<String> filterToString;

    ObjectProxy(ProxyDescriptor proxyDescriptor)
    {
        Set<String> tempFilter = new HashSet<String>();

        for (BeanInfo beanInfo : proxyDescriptor.proxiedBeanInfo)
        {

            IgnoreToString ignore = beanInfo.getBeanDescriptor().getBeanClass()
                    .getAnnotation(IgnoreToString.class);

            if (null != ignore)
            {
                tempFilter.addAll(Arrays.asList(ignore.value()));
            }
        }

        this.filterToString = Collections.unmodifiableSet(tempFilter);
        this.proxyDescriptor = proxyDescriptor;
    }

    ObjectProxy(ObjectProxy source)
    {
        this.filterToString = Collections
                .unmodifiableSet(source.filterToString);
        this.proxyDescriptor = source.proxyDescriptor;
    }

    /**
     * Allows callers to specify their own template for use with
     * {@link String#format(String, Object...)} which excepts two arguments.
     * 
     * <ol>
     * <li><code>ProxyDescriptor.toString()</code> result</li>
     * <li><code>PropertyValues.toString()</code> result</li>
     * </ol>
     * 
     * @param toStringTemplate
     *            Custom format template.
     */
    public static void setToStringTemplate(String toStringTemplate)
    {
        ObjectProxy.toStringTemplate = toStringTemplate;
    }

    /**
     * Restore the default format template for {@link #toString()}.
     */
    public static void resetToStringTemplate()
    {
        ObjectProxy.toStringTemplate = DEFAULT_TO_STRING_TEMPLATE;
    }

    Object proxy(final String message, final PropertyValues values,
            final Method method, final Object[] args)
            throws UnsupportedFeatureException
    {
        String methodName = method.getName();

        int count = (null == args) ? 0 : args.length;

        Class<?>[] argTypes = new Class<?>[count];

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
                                    "Proxying method, %1$s, with arguments (%2$s) for proxy, %3$s, to underlying Map.",
                                    methodName, argString, proxyDescriptor));
        }

        if ("toString".equals(method.getName()) && argTypes.length == 0)
        {
            return String.format(toStringTemplate, proxyDescriptor, values
                    .filteredToString(filterToString));
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
                return handleEquals(values, args[0]);
            }

            return method.invoke(values, args);
        }
        catch (NoSuchMethodException e)
        {
            throw new UnsupportedFeatureException(
                    String
                            .format(
                                    "Could not find a usable target for method, %1$s, with arguments (%2$s) on proxy, %4$s.%3$s",
                                    methodName, argString, message,
                                    proxyDescriptor));
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException(
                    String
                            .format(
                                    "Illegal access proxying Object method, %1$s, with arguments (%2$s) to internal Map for proxy, %3$s.",
                                    methodName, argString, proxyDescriptor), e);
        }
        catch (InvocationTargetException e)
        {
            throw new IllegalStateException(
                    String
                            .format(
                                    "Exception, %1$s, occured in reflection invocation while proxying Object method, %2$s, with arguments (%3$s) to internal Map for proxy, %4$s.",
                                    e.getMessage(), methodName, argString,
                                    proxyDescriptor), e);
        }
    }

    /**
     * The map of the comparison target has to be dug out, otherwise equivalence
     * won't track with the direct proxy of hashCode to the underlying storage
     * map.
     */
    private Boolean handleEquals(PropertyValues values, Object value)
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

        return Boolean.valueOf(values.equals(otherHandler.propertyValues));
    }

    private String parseArguments(Class<?>[] argTypes)
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
