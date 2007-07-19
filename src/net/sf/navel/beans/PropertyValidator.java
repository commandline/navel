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
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Provides validation on construction support for the DelegateBeanHandler,
 * ensuring that all the rules that must be true for the handler to work are.
 * This means that the proxied interface only defines JavaBean properties and
 * not any methods or events.
 * 
 * @author cmndln
 */
class PropertyValidator
{

    private static final PropertyValidator SINGLETON = new PropertyValidator();

    private PropertyValidator()
    {
        // enforce Singleton pattern
    }

    static void validateAll(Map<String, PropertyDescriptor> properties,
            Map<String, Object> values)
    {
        for (Entry<String, Object> entry : values.entrySet())
        {
            validate(properties, entry.getKey(), entry.getValue());
        }
    }

    static void validate(Map<String, PropertyDescriptor> properties,
            String propertyName, Object propertyValue)
    {
        SINGLETON.validateProperty(properties, propertyName, propertyValue);
    }

    static void validate(String propertyName,
            PropertyDescriptor propertyDescriptor, PropertyDelegate<?> delegate)
    {
        if (delegate instanceof IndexedPropertyDelegate)
        {
            IndexedPropertyDelegate<?, ?> indexedDelegate = (IndexedPropertyDelegate<?, ?>) delegate;

            Class<?> arrayType = delegate.propertyType();

            if (!arrayType.isArray())
            {
                throw new InvalidDelegateException(
                        String
                                .format(
                                        "Invalid type, %1$s, for IndexedPropertyDelegate.  Property, %2$s, must be of an array type.",
                                        delegate.propertyType(), propertyName));
            }

            Class<?> componentType = indexedDelegate.componentType();

            if (arrayType.getComponentType().isAssignableFrom(componentType))
            {
                throw new InvalidDelegateException(
                        String
                                .format(
                                        "Component of the delegate's array type, %1$s, must match the component type, %2$s, of the delegate.",
                                        arrayType.getName(), componentType
                                                .getName()));
            }
        }

        if (propertyDescriptor instanceof IndexedPropertyDescriptor
                && !(delegate instanceof IndexedPropertyDelegate))
        {
            throw new InvalidDelegateException(
                    String
                            .format(
                                    "Property, %2$s, requires an IndexedPropertyDelegate instance.",
                                    delegate.propertyType(), propertyName));
        }

        if (!propertyDescriptor.getPropertyType().equals(
                delegate.propertyType()))
        {
            throw new InvalidDelegateException(
                    String
                            .format(
                                    "Invalid type, %1$s, for PropertyDelegate.  Property, %2$s, requires type, %3$s.",
                                    delegate.propertyType(), propertyName,
                                    propertyDescriptor.getPropertyType()));
        }
    }

    private void validateProperty(Map<String, PropertyDescriptor> properties,
            String propertyName, Object propertyValue)
    {
        if (!properties.containsKey(propertyName))
        {
            throw new InvalidPropertyValueException(String.format(
                    "This JavaBean does not have a property, %1$s.",
                    propertyName));
        }

        PropertyDescriptor propertyDescriptor = properties.get(propertyName);

        Class<?> propertyType = propertyDescriptor.getPropertyType();

        // according to the JavaDocs for PropertyDescriptor, this indicates a
        // particular variation of an indexed property
        if (null == propertyType)
        {
            return;
        }

        // also give List properties a pass as the ListBuilder will deal with
        // these
        if (propertyType.isAssignableFrom(List.class))
        {
            return;
        }

        if (propertyType.isPrimitive())
        {
            PrimitiveSupport
                    .validate(propertyName, propertyType, propertyValue);

            return;
        }

        if (null == propertyValue)
        {
            return;
        }

        Class<?> valueType = propertyValue.getClass();

        if (Proxy.isProxyClass(valueType))
        {
            JavaBeanHandler handler = ProxyFactory.getHandler(propertyValue);

            if (null == handler)
            {
                throw new UnsupportedOperationException(
                        "Cannot validated nested properties that use an InvocationHandler other than JavaBeanHandler!");
            }

            if (handler.proxiesFor(propertyType))
            {
                return;
            }

            throw new InvalidPropertyValueException(
                    String
                            .format(
                                    "Navel bean value, %1$s, cannot be assigned to property type, %2$s, for property, %3$s.",
                                    propertyValue, propertyType, propertyName));
        }

        if (!propertyType.isAssignableFrom(valueType))
        {
            throw new InvalidPropertyValueException(
                    String
                            .format(
                                    "Value, %1$s, of type, %2$s, is not a valid value for property, %3$s, of type, %4$s.",
                                    propertyValue, valueType.getName(),
                                    propertyName, propertyType.getName()));
        }
    }
}
