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

import static net.sf.navel.beans.PrimitiveSupport.validate;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Provides validation on construction support for the DelegateBeanHandler,
 * ensuring that all the rules that must be true for the handler to work are.
 * This means that the proxied interface only defines JavaBean properties and
 * not any methods or events.
 * 
 * @author cmndln
 */
class PropertyValidator implements Serializable
{

    private static final long serialVersionUID = -6780317578317368699L;

    private static final Logger LOGGER = Logger
            .getLogger(PropertyValidator.class);

    PropertyValidator()
    {
    }

    /**
     * Ensure that the map of initial values is valid per the properties
     * described by the proxied class.
     * 
     * @param beanInfo
     *            Introspection data about proxied class.
     * @throws InvalidPropertyValueException
     *             Thrown in an initial value doesn't match up with the proxied
     *             class' properties, by name or type.
     */
    void resolve(BeanInfo beanInfo, Map<String, Object> values)
            throws InvalidPropertyValueException
    {
        if ((null == values) || values.isEmpty())
        {
            return;
        }

        List<PropertyDescriptor> properties = Arrays.asList(beanInfo
                .getPropertyDescriptors());

        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace("Found properties:");
            LOGGER.trace(formatProperties(properties));
        }

        eliminateMatches(values, properties);

        if (!values.isEmpty())
        {
            throw new InvalidPropertyValueException(
                    String
                            .format(
                                    "Extra values found in initial value map that do not match any known property for bean type %1$s : %2$s.",
                                    beanInfo.getBeanDescriptor().getBeanClass()
                                            .getName(), values.keySet()));
        }
    }

    /**
     * Eliminiate any values that exactly matches known properties.
     * 
     * @param values
     *            Property values copy to operate on.
     * @param properties
     *            Properties to eliminate against.
     * @throws InvalidPropertyValueException
     *             In case there is a coercion problem with an eliminated
     *             property.
     */
    private void eliminateMatches(Map<String, Object> values,
            List<PropertyDescriptor> properties)
            throws InvalidPropertyValueException
    {
        for (int i = 0; i < properties.size(); i++)
        {
            String propertyName = properties.get(i).getName();

            if (!values.containsKey(propertyName))
            {
                continue;
            }

            Object propertyValue = values.get(propertyName);

            checkValue(properties.get(i), propertyName, propertyValue);

            values.remove(propertyName);
        }
    }

    private String formatProperties(List<PropertyDescriptor> properties)
    {
        StringBuffer buffer = new StringBuffer("[");

        for (PropertyDescriptor property : properties)
        {
            if (buffer.length() != 1)
            {
                buffer.append(", ");
            }

            buffer.append(property.getName());
        }

        buffer.append(']');

        return buffer.toString();
    }

    private void checkValue(PropertyDescriptor propertyDescriptor,
            String propertyName, Object propertyValue)
    {
        Class<?> propertyType = propertyDescriptor.getPropertyType();

        if (propertyType.isPrimitive())
        {
            validate(propertyName, propertyType, propertyValue);

            return;
        }

        if (propertyValue != null && !propertyType.isInstance(propertyValue))
        {
            throw new InvalidPropertyValueException(propertyValue + " of type "
                    + propertyValue.getClass().getName()
                    + " is not a valid value for property " + propertyName
                    + " of type " + propertyType.getName());
        }
    }
}
