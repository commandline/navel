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
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * This PropertyManipulator daughter class provides support for indexed
 * properties, as defined in the JavaBeans spec.
 * 
 * @author cmdln
 */
class IndexedPropertyManipulator extends SimplePropertyManipulator
{

    private static final Logger LOGGER = Logger
            .getLogger(IndexedPropertyManipulator.class);

    private static final String OPEN_BRACKET = "[";

    private static final String CLOSE_BRACKET = "]";

    private static final int MISSING_INDEX = -1;

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
     */
    @Override
    public void handleWrite(PropertyDescriptor property, String propertyName,
            Object bean, Object value)
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Inside IndexedPropertyManipulator.handleWrite().");
        }

        if (!(property instanceof IndexedPropertyDescriptor))
        {
            throw new IllegalArgumentException(
                    String
                            .format(
                                    "Cannot perform indexed write on a non-indexed property, %1$s, of bean, %2$s!",
                                    propertyName, bean));
        }

        int index = getIndex(propertyName);

        if (MISSING_INDEX == index)
        {
            return;
        }

        IndexedPropertyDescriptor indexedProperty = (IndexedPropertyDescriptor) property;

        Method writeMethod = indexedProperty.getIndexedWriteMethod();
        invokeWriteMethod(writeMethod, bean, new Object[]
        { Integer.valueOf(index), value });
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
    public Object handleRead(PropertyDescriptor property, String propertyName,
            Object bean)
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Inside IndexedPropertyManipulator.handleRead().");
        }

        if (!(property instanceof IndexedPropertyDescriptor))
        {
            throw new IllegalArgumentException(
                    String
                            .format(
                                    "Cannot perform indexed read on a non-indexed property, %1$s, of bean, %2$s!",
                                    propertyName, bean));
        }

        int index = getIndex(propertyName);

        if (MISSING_INDEX == index)
        {
            LOGGER.warn("No index found for indexed read.");
            return null;
        }

        IndexedPropertyDescriptor indexedProperty = (IndexedPropertyDescriptor) property;

        Method readMethod = indexedProperty.getIndexedReadMethod();
        return invokeReadMethod(readMethod, bean, new Object[]
        { Integer.valueOf(index) });
    }

    static int getIndex(String propertyName)
    {
        int braceStart = propertyName.indexOf(OPEN_BRACKET);
        int braceEnd = propertyName.indexOf(CLOSE_BRACKET);

        if ((-1 == braceStart) || (-1 == braceEnd) || (braceEnd <= braceStart))
        {
            LOGGER.warn("One or both braces missing or invalid positioning.");
            return -1;
        }

        String indexString = propertyName.substring(braceStart + 1, braceEnd);

        try
        {
            Integer index = Integer.valueOf(indexString);

            // compare the parsed to the original in case there are any
            // trailing characters on the original
            if (index.toString().equals(indexString))
            {
                return index.intValue();
            }
            else
            {
                return -1;
            }
        }
        catch (NumberFormatException e)
        {
            LOGGER.warn(indexString + " cannot be parsed as an int.");

            return -1;
        }
    }

    static void putIndexed(Map<String, Object> values, String nameWithIndex,
            String propertyName, PropertyDescriptor propertyDescriptor,
            Object propertyValue)
    {
        Object array = values.get(propertyName);

        // use the argument since the local will have had the index
        // operator and value stripped
        int arrayIndex = IndexedPropertyManipulator.getIndex(nameWithIndex);

        if (PrimitiveSupport.isPrimitiveArray(propertyDescriptor
                .getPropertyType()))
        {
            PrimitiveSupport.setElement(array, arrayIndex, propertyValue);
        }
        else
        {
            Object[] indexed = (Object[]) array;

            indexed[arrayIndex] = propertyValue;
        }
    }

    static Object getIndexed(Map<String, Object> values, String nameWithIndex,
            String propertyName, PropertyDescriptor propertyDescriptor)
    {
        Object array = values.get(propertyName);

        // use the argument since the local will have had the index
        // operator and value stripped
        int arrayIndex = IndexedPropertyManipulator.getIndex(nameWithIndex);

        if (PrimitiveSupport.isPrimitiveArray(propertyDescriptor
                .getPropertyType()))
        {
            return PrimitiveSupport.getElement(array, arrayIndex);
        }
        else
        {
            Object[] indexed = (Object[]) array;

            Object nestedValue = indexed[arrayIndex];

            if (null == nestedValue)
            {
                nestedValue = NestedBeanFactory
                        .create(nameWithIndex, propertyDescriptor
                                .getPropertyType().getComponentType());

                indexed[arrayIndex] = nestedValue;
            }

            return nestedValue;
        }
    }
}
