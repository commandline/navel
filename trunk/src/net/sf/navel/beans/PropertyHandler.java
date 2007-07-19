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

import static net.sf.navel.beans.PrimitiveSupport.getElement;
import static net.sf.navel.beans.PrimitiveSupport.handleNull;
import static net.sf.navel.beans.PrimitiveSupport.isPrimitiveArray;
import static net.sf.navel.beans.PrimitiveSupport.setElement;

import java.beans.Introspector;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * All of the property support code and property value storage, encapsulated for
 * use by the {@see JavaBeanHandler}.
 * 
 * @author cmdln
 */
class PropertyHandler implements Serializable
{

    private static final long serialVersionUID = 8234362825076556906L;

    private static final Logger LOGGER = Logger
            .getLogger(PropertyHandler.class);

    // verb constants
    private static final String WRITE = "set";

    private static final String READ = "get";

    private static final String BEING = "is";

    private PropertyValues values;

    PropertyHandler(PropertyValues values)
    {
        this.values = values;
    }

    static boolean handles(Method method)
    {
        String methodName = method.getName();

        if (methodName.startsWith(WRITE))
        {
            return true;
        }

        if (methodName.startsWith(READ))
        {
            return true;
        }

        if (methodName.startsWith(BEING))
        {
            return true;
        }

        return false;
    }

    /**
     * The method required by the InvocationHandler interface. The
     * implementation here just supports manipulating the underlying map via the
     * bean property methods on the proxied interface.
     * 
     * @param proxy
     *            Class that was original called against.
     * @param method
     *            Method being invoked.
     * @param args
     *            Argument values.
     * @return Return value, must be null for void return types.
     */
    Object handle(Object proxy, Method method, Object[] args) throws Throwable
    {
        String methodName = method.getName();

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Invoking " + methodName);

            if (args != null)
            {
                LOGGER.debug("Arguments " + Arrays.asList(args));
            }
        }

        if (methodName.startsWith(WRITE))
        {
            handleWrite(methodName, args);

            return null;
        }
        else if (methodName.startsWith(READ))
        {
            return handleRead(method, args);
        }
        else if (methodName.startsWith(BEING))
        {
            return handleBeing(methodName);
        }
        else
        {
            throw new IllegalStateException(String.format(
                    "The method, %1$s, is not a property accessor or mutator!",
                    method.getName()));
        }
    }

    @SuppressWarnings("unchecked")
    private void handleWrite(String methodName, Object[] args)
    {
        if (null == args)
        {
            throw new IllegalArgumentException(
                    "PropertyBeanHandler needs a value to write to the indicated property.");
        }

        String propertyName = getPropertyName(methodName, WRITE);

        if (1 == args.length)
        {
            if (values.isAttached(propertyName))
            {
                PropertyDelegate delegate = values.propertyDelegates
                        .get(propertyName);

                if (LOGGER.isDebugEnabled())
                {
                    LOGGER
                            .debug(String
                                    .format(
                                            "Delegating write on property, %1$s, to delegate, %2$s",
                                            propertyName, delegate));
                }

                delegate.set(values, propertyName, args[0]);

                return;
            }

            values.put(propertyName, args[0]);
        }
        else if (2 == args.length)
        {
            handleIndexedWrite(propertyName, args);
        }
        else
        {
            throw new IllegalArgumentException(
                    "PropertyBeanHandler only supports writing simple and indexed properties.");
        }
    }

    @SuppressWarnings("unchecked")
    private void handleIndexedWrite(String propertyName, Object[] args)
    {
        int index = getIndex(args[0], true);

        Object value = values.get(propertyName);

        if (values.isAttached(propertyName))
        {
            // the PropertyValidator ensures at attachment time that this is a
            // safe cast
            IndexedPropertyDelegate delegate = (IndexedPropertyDelegate) values.propertyDelegates
                    .get(propertyName);

            if (LOGGER.isDebugEnabled())
            {
                LOGGER
                        .debug(String
                                .format(
                                        "Delegating write on property, %1$s, to delegate, %2$s",
                                        propertyName, delegate));
            }

            delegate.set(values, propertyName, index, args[1]);

            return;
        }

        if (isPrimitiveArray(value.getClass()))
        {
            setElement(value, index, args[1]);
        }
        else
        {
            Object[] indexed = (Object[]) value;

            indexed[index] = args[1];
        }
    }

    private Object handleRead(Method method, Object[] args)
    {
        String propertyName = getPropertyName(method.getName(), READ);

        if ((null == args) || (0 == args.length))
        {
            if (values.isAttached(propertyName))
            {
                PropertyDelegate<?> delegate = values.propertyDelegates
                        .get(propertyName);

                if (LOGGER.isDebugEnabled())
                {
                    LOGGER
                            .debug(String
                                    .format(
                                            "Delegating read on property, %1$s, to delegate, %2$s",
                                            propertyName, delegate));
                }

                return delegate.get(values, propertyName);
            }

            return handleNull(method.getReturnType(), values.get(propertyName));
        }
        else if (1 == args.length)
        {
            return handleIndexedRead(propertyName, args);
        }
        else
        {
            throw new IllegalArgumentException(
                    "PropertyBeanHandler only supports reading simple and indexed properties.");
        }
    }

    @SuppressWarnings("unchecked")
    private Object handleIndexedRead(String propertyName, Object[] args)
    {
        int index = getIndex(args[0], false);

        Object value = values.get(propertyName);

        if (null == value)
        {
            LOGGER.warn("Trying to read null array.");
            return null;
        }

        if (values.isAttached(propertyName))
        {
            // the PropertyValidator ensures this is a safe cast at the time the
            // delegate is attached
            IndexedPropertyDelegate delegate = (IndexedPropertyDelegate) values.propertyDelegates
                    .get(propertyName);

            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug(String.format(
                        "Delegating read on property, %1$s, to delegate, %2$s",
                        propertyName, delegate));
            }

            return delegate.get(values, propertyName, index);
        }

        if (isPrimitiveArray(value.getClass()))
        {
            return getElement(value, index);
        }

        if (List.class.isAssignableFrom(value.getClass()))
        {
            List indexed = (List) values.get(propertyName);

            Object element = indexed.get(index);

            return element;
        }

        Object[] indexed = (Object[]) values.get(propertyName);

        return indexed[index];
    }

    private Object handleBeing(String methodName)
    {
        String propertyName = getPropertyName(methodName, BEING);

        if (values.isAttached(propertyName))
        {
            PropertyDelegate<?> delegate = values.propertyDelegates
                    .get(propertyName);

            return delegate.get(values, propertyName);
        }

        return handleNull(Boolean.class, values.get(propertyName));
    }

    private String getPropertyName(String methodName, String prefix)
    {
        int firstChar = prefix.length();

        String propertyName = methodName.substring(firstChar);

        propertyName = Introspector.decapitalize(propertyName);

        return propertyName;
    }

    private int getIndex(Object raw, boolean forWrite)
    {
        if ((null == raw) || !(raw instanceof Integer))
        {
            if (forWrite)
            {
                throw new IllegalArgumentException(
                        "Index for write is invalid.");
            }
            else
            {
                throw new IllegalArgumentException("Index for read is invalid.");
            }
        }

        Integer indexWrapper = (Integer) raw;
        int index = indexWrapper.intValue();

        if (index < 0)
        {
            if (forWrite)
            {
                throw new IllegalArgumentException(
                        "Index for write must be positive.");
            }
            else
            {
                throw new IllegalArgumentException(
                        "Index for read must be positive.");
            }
        }

        return index;
    }
}