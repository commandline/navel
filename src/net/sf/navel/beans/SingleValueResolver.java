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
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * @author cmdln
 * 
 */
class SingleValueResolver
{

    private static final Logger LOGGER = LogManager
            .getLogger(SingleValueResolver.class);

    private static final SingleValueResolver SINGLETON = new SingleValueResolver();

    private SingleValueResolver()
    {
        // enforce Singleton pattern
    }

    static boolean put(PropertyValues propertyValues,
            String propertyExpression, Object propertyValue)
    {
        return SINGLETON.putValue(propertyValues, new DotNotationExpression(
                propertyExpression).getRoot(), propertyValue);
    }

    static Object get(PropertyValues propertyValues, String propertyExpression)
    {
        return SINGLETON.getValue(propertyValues, new DotNotationExpression(
                propertyExpression).getRoot());
    }

    static boolean containsKey(PropertyValues propertyValues,
            String propertyExpression)
    {
        return SINGLETON.valuesContainsKey(propertyValues,
                new DotNotationExpression(propertyExpression).getRoot());
    }

    static boolean isPropertyOf(PropertyValues propertyValues,
            String propertyExpression)
    {
        return SINGLETON.isPropertyOfValues(propertyValues.getProxyDescriptor(), 
                new DotNotationExpression(propertyExpression).getRoot());
    }

    private boolean putValue(PropertyValues propertyValues,
            PropertyExpression expression, Object propertyValue)
    {
        String propertyName = expression.getPropertyName();

        ProxyDescriptor proxyDescriptor = propertyValues.getProxyDescriptor();

        PropertyDescriptor propertyDescriptor = proxyDescriptor.propertyDescriptors
                .get(propertyName);

        if (null == propertyDescriptor)
        {
            throw new InvalidPropertyValueException(
                    String
                            .format(
                                    "Cannot validate put, no property descriptor for property name, %1$s, on proxy, %2$s.",
                                    propertyName, proxyDescriptor));
        }

        if (expression.isLeaf())
        {
            if (expression.isIndexed())
            {
                putValueWithBracket(propertyValues.get(propertyName),
                        expression, propertyValue);
            }
            else
            {
                PropertyValidator.validate(proxyDescriptor, propertyName,
                        propertyValue);

                propertyValues.putInternal(propertyName, propertyValue);
            }

            return true;
        }

        Class<?> propertyType = propertyDescriptor.getPropertyType();

        Object interimValue = propertyValues.getInternal(propertyName);

        if (expression.isIndexed())
        {
            Object elementValue = getValueWithBracket(expression, interimValue);

            if (null == elementValue)
            {
                Class<?> elementType = getAppropriateBracketType(propertyDescriptor);

                expression.validateInstantiable(elementType);

                elementValue = ProxyFactory.create(elementType);

                putValueWithBracket(interimValue, expression, elementValue);
            }

            interimValue = elementValue;
        }

        if (null == interimValue)
        {
            expression.validateInstantiable(propertyType);

            interimValue = ProxyFactory.create(propertyType);
        }

        JavaBeanHandler nestedHandler = ProxyFactory.getHandler(interimValue);

        if (null == nestedHandler)
        {
            LOGGER
                    .warn(String
                            .format(
                                    "Nested property, %1$s, must currently be an interface to allow full de-reference.  Was of type, %2$s.",
                                    propertyName, propertyType.getName()));
            return false;
        }

        // recurse on the nested property
        return putValue(nestedHandler.propertyValues, expression.getChild(),
                propertyValue);
    }

    @SuppressWarnings("unchecked")
    private void putValueWithBracket(Object value,
            PropertyExpression expression, Object propertyValue)
    {
        if (null == value)
        {
            throw new InvalidExpressionException(
                    String
                            .format(
                                    "The expression, %1$s, requires either a valid List or a valid array for the bracket operator.",
                                    expression.expressionToRoot()));
        }

        expression.validateIndex();

        int index = expression.getIndex();

        if (value instanceof List)
        {
            List<Object> listValue = (List<Object>) value;

            expression.validateListBounds(listValue.size());

            listValue.set(index, propertyValue);

            return;
        }

        if (PrimitiveSupport.isPrimitiveArray(value.getClass()))
        {
            PrimitiveSupport.setElement(value, index, propertyValue);

            return;
        }

        expression.validateArray(value);

        Object[] arrayValue = (Object[]) value;

        expression.validateArrayBounds(arrayValue.length);

        arrayValue[index] = propertyValue;
    }

    private Object getValue(PropertyValues propertyValues,
            PropertyExpression currentExpression)
    {
        Object interimValue = propertyValues.getInternal(currentExpression
                .getPropertyName());

        currentExpression.validateInterimForIndexed(interimValue);

        if (currentExpression.isLeaf())
        {
            if (currentExpression.isIndexed())
            {
                return getValueWithBracket(currentExpression, interimValue);
            }
            else
            {
                return interimValue;
            }
        }

        if (currentExpression.isIndexed())
        {
            interimValue = getValueWithBracket(currentExpression, interimValue);
        }

        JavaBeanHandler interimHandler = ProxyFactory.getHandler(interimValue);

        if (null == interimHandler)
        {
            throw new InvalidExpressionException(String.format(
                    "Cannot de-reference a non-Navel bean property, %1$s.",
                    currentExpression.expressionToRoot()));
        }
        else
        {
            return getValue(interimHandler.propertyValues, currentExpression
                    .getChild());
        }
    }

    private Object getValueWithBracket(PropertyExpression expression,
            Object value)
    {
        int index = expression.getIndex();

        expression.validateIndex();

        if (value instanceof List)
        {
            List<?> listValue = (List<?>) value;

            expression.validateListBounds(listValue.size());

            return listValue.get(index);
        }

        if (PrimitiveSupport.isPrimitiveArray(value.getClass()))
        {
            return PrimitiveSupport.getElement(value, index);
        }

        expression.validateArray(value);

        Object[] arrayValue = (Object[]) value;

        expression.validateArrayBounds(arrayValue.length);

        return arrayValue[index];
    }
    
    private boolean isPropertyOfValues(ProxyDescriptor proxyDescriptor, PropertyExpression expression)
    {
        String shallowProperty = expression.getPropertyName();

        boolean indexedProperty = expression.isIndexed();

        for (PropertyDescriptor propertyDescriptor : proxyDescriptor.propertyDescriptors
                .values())
        {
            // keep going if this is not the property we are looking for or
            // a parent property
            if (!propertyDescriptor.getName().equals(shallowProperty))
            {
                continue;
            }

            // if this is a leafy property, we're done
            if (expression.isLeaf())
            {
                return true;
            }
            
            Class<?> nextType = propertyDescriptor.getPropertyType();

            // otherwise, dig out the more specific type for a list or array
            if (indexedProperty)
            {
                nextType = getAppropriateBracketType(propertyDescriptor);
                
                if (null == nextType)
                {
                    return false;
                }
            }
            
            return isPropertyOfValues(new ProxyDescriptor(new Class<?>[] { nextType }), expression.getChild());
        }

        return false;
    }

    private boolean valuesContainsKey(PropertyValues propertyValues,
            PropertyExpression expression)
    {
        // if this is a leafy property, we're done
        if (expression.isLeaf() && !expression.isIndexed())
        {
            return propertyValues.containsKeyInternal(expression
                    .getPropertyName());
        }

        Object interimValue = propertyValues.getInternal(expression
                .getPropertyName());

        if (expression.isIndexed())
        {
            if (null == interimValue)
            {
                return false;
            }

            interimValue = getValueWithBracket(expression, interimValue);

            if (expression.isLeaf())
            {
                return interimValue != null;
            }
        }

        JavaBeanHandler nestedHandler = ProxyFactory.getHandler(interimValue);

        if (null == nestedHandler)
        {
            return false;
        }
        else
        {
            return valuesContainsKey(nestedHandler.propertyValues, expression
                    .getChild());
        }
    }

    private Class<?> getAppropriateBracketType(
            PropertyDescriptor propertyDescriptor)
    {
        if (propertyDescriptor instanceof IndexedPropertyDescriptor)
        {
            IndexedPropertyDescriptor indexedPropertyDescriptor = (IndexedPropertyDescriptor) propertyDescriptor;

            return indexedPropertyDescriptor.getIndexedPropertyType();
        }

        CollectionType collectionType = propertyDescriptor.getReadMethod()
                .getAnnotation(CollectionType.class);

        if (null == collectionType)
        {
            return null;
        }

        return collectionType.value();
    }
}
