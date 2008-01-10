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

import java.beans.IndexedPropertyDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;

/**
 * This PropertyManipulator daughter class provides support for indexed
 * properties, as defined in the JavaBeans spec.
 * 
 * @author cmdln
 */
class ReflectionIndexedManipulator extends ReflectionSimpleManipulator
{

    private static final Logger LOGGER = Logger
            .getLogger(ReflectionIndexedManipulator.class);

    /**
     * This implementation knows how to access the indexed write property on
     * IndexedPropertyDescriptor. Since the factory method on
     * PropertyManipulator is keyed on PropertyDescriptor sub-types, the cast
     * should be perfectly safe.
     * 
     * @param property
     *            Descriptor for the target property.
     * @param propertyName
     *            Name of the property, may be an expression of some sort.
     * @param bean
     *            The bean to write to.
     * @param value
     *            The value to write.
     * @returns Whether the propert was written.
     */
    @Override
    boolean handleWrite(PropertyDescriptor property,
            String propertyName, Object bean, Object value, boolean suppressExceptions)
    {
        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace("Inside IndexedPropertyManipulator.handleWrite().");
        }

        if (!(property instanceof IndexedPropertyDescriptor))
        {
            throw new IllegalArgumentException(
                    String
                            .format(
                                    "Cannot perform indexed write on a non-indexed property, %1$s, of bean, %2$s!",
                                    propertyName, bean));
        }

        Integer index = DotNotationExpression.getIndex(propertyName);

        if (null == index)
        {
            return false;
        }

        IndexedPropertyDescriptor indexedProperty = (IndexedPropertyDescriptor) property;

        Method writeMethod = indexedProperty.getIndexedWriteMethod();
        return invokeWriteMethod(writeMethod, bean, new Object[]
        { Integer.valueOf(index), value }, suppressExceptions);
    }

    /**
     * This implementation knows how to access the indexed read property on
     * IndexedPropertyDescriptor. Since the factory method on
     * PropertyManipulator is keyed on PropertyDescriptor sub-types, the cast
     * should be perfectly safe.
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
    Object handleRead(PropertyDescriptor property, String propertyName,
            Object bean, boolean suppressExceptions)
    {
        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace("Inside IndexedPropertyManipulator.handleRead().");
        }

        if (!(property instanceof IndexedPropertyDescriptor))
        {
            throw new IllegalArgumentException(
                    String
                            .format(
                                    "Cannot perform indexed read on a non-indexed property, %1$s, of bean, %2$s!",
                                    propertyName, bean));
        }

        Integer index = DotNotationExpression.getIndex(propertyName);

        if (null == index)
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug(String.format(
                        "No index found for indexed read of property, %1$s.",
                        propertyName));
            }

            return null;
        }

        IndexedPropertyDescriptor indexedProperty = (IndexedPropertyDescriptor) property;

        Method readMethod = indexedProperty.getIndexedReadMethod();
        return invokeReadMethod(readMethod, bean, new Object[]
        { Integer.valueOf(index) }, suppressExceptions);
    }
}
