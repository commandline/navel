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

import static net.sf.navel.beans.PrimitiveSupport.isValid;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;

/**
 * This daughter class knows how to interact with properties represented by the
 * stock PropertyDescriptor.
 * 
 * @author cmdln
 */
public class SimplePropertyManipulator extends AbstractPropertyManipulator
{

    private static final Logger LOGGER = Logger
            .getLogger(SimplePropertyManipulator.class);

    /**
     * This write method is coerces the value, if appropriate, then uses simple
     * reflection to set the property.
     * 
     * @param property
     *            Descriptor for the target property.
     * @param propertyName
     *            Name of the property, may be an expression of some sort.
     * @param bean
     *            The bean to write to.
     * @param value
     *            The value to write.
     * @returns Whether the target property was written.
     */
    @Override
    public boolean handleWrite(PropertyDescriptor property,
            String propertyName, Object bean, Object value)
    {
        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace("Inside SimplePropertyManipulator.handleWrite() for "
                    + propertyName + "/" + property.getName()
                    + " for bean of type " + bean.getClass().getName() + ".");
        }

        Method writeMethod = property.getWriteMethod();

        if (null == writeMethod)
        {
            if (LOGGER.isTraceEnabled())
            {
                LOGGER.trace("No write method available for " + propertyName
                        + "/" + property.getName() + " for bean of type "
                        + bean.getClass().getName());
            }

            return false;
        }

        Object convertedValue = convertPropertyValue(property, value);

        return invokeWriteMethod(writeMethod, bean, new Object[]
        { convertedValue });
    }

    /**
     * This property uses simple reflection to read the property.
     * 
     * @param property
     *            Descriptor for the target property.
     * @param propertyName
     *            Name of the property, may be an expression of some sort.
     * @param bean
     *            The bean to write to.
     * @return The value read from the bean argument.
     */
    @Override
    public Object handleRead(PropertyDescriptor property, String propertyName,
            Object bean)
    {
        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace("Inside SimplePropertyManipulator.handleRead().");
        }

        Method readMethod = property.getReadMethod();

        return invokeReadMethod(readMethod, bean, new Object[0]);
    }

    /**
     * Somewhat generic write implementation that will work with either indexed
     * or simple properties.
     * 
     * @param method
     *            Write method to invoke, can be null.
     * @param bean
     *            Invocation target.
     * @param args
     *            Arguments.
     * @returns Whether the target property was written.
     */
    protected final boolean invokeWriteMethod(Method method, Object bean,
            Object[] args)
    {
        if (null == method)
        {
            LOGGER.debug("No write method available.");

            return false;
        }

        Class<?>[] argTypes = method.getParameterTypes();

        if (argTypes.length != args.length)
        {
            LOGGER
                    .debug("PropertyManipulator only supports writing simple and indexed properties.");

            return false;
        }

        int valueIndex = args.length - 1;
        Class<?> propertyType = argTypes[valueIndex];
        Object value = args[valueIndex];

        if (propertyType.isPrimitive())
        {
            if (!isValid(propertyType, value))
            {
                if (LOGGER.isTraceEnabled())
                {
                    LOGGER
                            .trace("Argument type (primitive) mismatch trying to invoke write, "
                                    + method.getName());
                    LOGGER.trace(propertyType.getName()
                            + ": "
                            + ((null == value) ? "null" : value.getClass()
                                    .getName()) + ".");
                }

                return false;
            }
        }
        else
        {
            if (value != null && !propertyType.isInstance(value))
            {
                if (LOGGER.isTraceEnabled())
                {
                    LOGGER
                            .trace("Argument type mismatch trying to invoke write, "
                                    + method.getName());
                    LOGGER.trace(propertyType.getName()
                            + ": "
                            + ((null == value.getClass()) ? "null" : value
                                    .getClass().getName()) + ".");
                }

                return false;
            }
        }

        try
        {
            method.invoke(bean, args);
            
            return true;
        }
        catch (IllegalAccessException e)
        {
            LOGGER.warn("Illegal access invoking write method, "
                    + method.getName());
            
            return false;
        }
        catch (InvocationTargetException e)
        {
            LOGGER.warn("Bad invocation of write method, " + method.getName());
            
            return false;
        }
    }

    /**
     * Somewhat generic read implementation that will work with either indexed
     * or simple properties.
     * 
     * @param method
     *            Read method to invoke, can be null.
     * @param bean
     *            Invocation target.
     * @param args
     *            Arguments.
     * @return Property value.
     */
    protected final Object invokeReadMethod(Method method, Object bean,
            Object[] args)
    {
        if (null == method)
        {
            LOGGER.debug("No read method available.");

            return null;
        }

        Class<?>[] argTypes = method.getParameterTypes();

        if (argTypes.length > args.length)
        {
            LOGGER
                    .debug("PropertyManipulator only supports reading simple and indexed properties.");

            return null;
        }

        try
        {
            return method.invoke(bean, args);
        }
        catch (IllegalAccessException e)
        {
            LOGGER.warn("Illegal access invoking read method, "
                    + method.getName());

            return null;
        }
        catch (InvocationTargetException e)
        {
            LOGGER.warn("Bad invocation of read method, " + method.getName());

            return null;
        }
    }
}
