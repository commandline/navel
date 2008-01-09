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
import java.beans.IndexedPropertyDescriptor;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

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

    private static final String DEFAULT_TO_STRING_TEMPLATE = "propertyValues = %1$s";

    private static String toStringTemplate = DEFAULT_TO_STRING_TEMPLATE;

    private final Map<String, PropertyDelegate<?>> propertyDelegates;

    private final Map<String, Object> values;

    private final ProxyDescriptor proxyDescriptor;

    final ObjectProxy objectProxy;

    private final boolean immutable;

    PropertyValues(ProxyDescriptor proxyDescriptor,
            Map<String, Object> initialValues)
    {
        this.proxyDescriptor = proxyDescriptor;

        Map<String, Object> initialCopy = null == initialValues ? new HashMap<String, Object>()
                : new HashMap<String, Object>(initialValues);

        // this cleans up nested values, if it hits a Navel bean as a nested
        // property it will try to re-use or instantiate as appropriate,
        // including validating the nested bean
        InitialValuesResolver.resolve(proxyDescriptor.propertyDescriptors,
                initialCopy);

        // only validates the direct properties of this bean, but the step above
        // takes care of ensuring nested properties are valid
        PropertyValidator.validateAll(proxyDescriptor, initialCopy);

        this.objectProxy = new ObjectProxy(proxyDescriptor);

        this.values = initialCopy;
        this.propertyDelegates = new HashMap<String, PropertyDelegate<?>>();
        this.immutable = false;
    }

    PropertyValues(PropertyValues source, final boolean deep,
            final boolean immutable)
    {
        // if immutable is true, it requires a deep copy
        boolean qualifiedDeep = deep || immutable;

        // NavelDescriptor is immutable so safe to share like this
        this.proxyDescriptor = source.proxyDescriptor;

        this.objectProxy = new ObjectProxy(source.objectProxy);

        this.immutable = immutable;

        // shallow copy
        Map<String, Object> valuesCopy = new HashMap<String, Object>(
                source.values);

        // deep copy, if requested either directly or as a consequence of
        // requesting an immutable copy
        if (qualifiedDeep)
        {
            // iterate the source since so that the copy can be safely modified
            for (Entry<String, Object> entry : source.values.entrySet())
            {
                Object nestedValue = entry.getValue();

                if (ProxyFactory.getHandler(nestedValue) == null)
                {
                    continue;
                }

                String nestedProperty = entry.getKey();

                // deep and unmodifiable carry all the way throughout the copied
                // graph
                Object nestedCopy = immutable ? ProxyFactory
                        .unmodifiableObject(nestedValue) : ProxyFactory.copy(
                        nestedValue, true);

                valuesCopy.put(nestedProperty, nestedCopy);
            }
        }

        this.values = immutable ? Collections.unmodifiableMap(valuesCopy)
                : valuesCopy;
        // implementers of PropertyDelegate must be stateless to avoid problems
        // with this shallow copy
        this.propertyDelegates = immutable ? Collections
                .unmodifiableMap(source.propertyDelegates)
                : new HashMap<String, PropertyDelegate<?>>(
                        source.propertyDelegates);
    }

    /**
     * Allows callers to specify their own template for use with
     * {@link String#format(String, Object...)} which excepts one argument.
     * 
     * <ol>
     * <li><code>Map.toString()</code> result</li>
     * </ol>
     * 
     * @param toStringTemplate
     *            Custom format template.
     */
    public static void setToStringTemplate(String toStringTemplate)
    {
        PropertyValues.toStringTemplate = toStringTemplate;
    }

    /**
     * Restore the default format template for {@link #toString()}.
     */
    public static void resetToStringTemplate()
    {
        PropertyValues.toStringTemplate = DEFAULT_TO_STRING_TEMPLATE;
    }

    public ProxyDescriptor getProxyDescriptor()
    {
        return proxyDescriptor;
    }

    /**
     * Return a copy of the internal values, generally safe to manipulate. Does
     * not perform a deep copy, however, so be careful of de-referencing nested
     * values on elements of the copy.
     * 
     * @param flatten
     *            If true, the nested beans will be flattened, calculating new
     *            keys in the flat return map that are valid dot-notation
     *            expressions representing where the original values where in
     *            the normalized storage graph.
     * @return A shallow copy of the internal values of this instance.
     */
    public Map<String, Object> copyValues(boolean flatten)
    {
        Map<String, Object> copy = new HashMap<String, Object>(values);

        if (flatten)
        {
            BeanManipulator.expandNestedBeans(copy);
        }

        return copy;
    }

    /**
     * Checks that the supplied expression is valid for the JavaBean's compile
     * time property set, that the type matches or can be coerced, and adds the
     * supplied value to internal storage.
     * 
     * @param dotExpression
     *            A property in the set of properties introspected when the
     *            associated Proxy and JavaBeanHandler were created by the
     *            ProxyFactory.
     * @param value
     *            Checked for type safety against the appropriate property.
     */
    public void put(String dotExpression, Object value)
    {
        checkImmutable();

        DotNotationExpression fullExpression = new DotNotationExpression(
                dotExpression);

        putValue(fullExpression.getRoot(), value);
    }

    /**
     * Forwards to the internal map, after resolving nested and list properties
     * and validating the new map. Will overwrite values at the same keys in the
     * internal storage.
     */
    public void putAll(Map<String, Object> newValues)
    {
        checkImmutable();

        // resolution depends on combining existing and new values, specifically
        // for lists and setting unset values on existing beans
        Map<String, Object> combined = new HashMap<String, Object>(values);
        combined.putAll(newValues);

        InitialValuesResolver.resolve(proxyDescriptor.propertyDescriptors,
                combined);

        // depends on resolution taking care of lists, not presently possible to
        // validate just the new values
        PropertyValidator.validateAll(proxyDescriptor, combined);

        // clear out the existing values since the new Map will contain old and
        // new correctly resolved, validated and combined
        values.clear();
        values.putAll(combined);
    }

    /**
     * Evaluates the supplied expression and returns the corresponding value, if
     * there is one.
     * 
     * @param dotExpression
     *            Property names chained with the dot (.) character, may also
     *            use the bracket characters ([]) with an index value to
     *            de-reference lists and arrays.
     * @return Null if there is no corresponding value, otherwise the value
     *         indicated by the expression.
     * @throws InvalidExpressionException
     *             If the dot expression doesn't parse with the given bean
     *             interfaces.
     */
    public Object get(String dotExpression)
    {
        DotNotationExpression fullExpression = new DotNotationExpression(
                dotExpression);

        return getValue(this, fullExpression.getRoot());
    }

    /**
     * Figure out if the supplied expression refers to a valid entry in the
     * underlying storage.
     * 
     * @param dotExpression
     *            Property names chained with the dot (.) character, may also
     *            use the bracket characters ([]) with an index value to
     *            de-reference lists and arrays.
     * @return True if an entry exists, even if its value is null, false if
     *         there is no entry at all.
     * @throws InvalidExpressionException
     *             If the dot expression doesn't parse with the given bean
     *             interfaces.
     */
    public boolean containsKey(String dotExpression)
    {
        DotNotationExpression fullExpression = new DotNotationExpression(
                dotExpression);

        return containsKey(fullExpression.getRoot());
    }

    /**
     * Evaluates the supplied expression and removes the corresponding value, if
     * there is one.
     * 
     * @param dotExpression
     *            Property names chained with the dot (.) character, may also
     *            use the bracket characters ([]) with an index value to
     *            de-reference lists and arrays.
     * @return Null if nothing was removed, otherwise the value indicated by the
     *         expression.
     * @throws InvalidExpressionException
     *             If the dot expression doesn't parse with the given bean
     *             interfaces.
     */
    public Object remove(String property)
    {
        checkImmutable();

        return values.remove(property);
    }

    /**
     * Clears all of the internal storage entries.
     */
    public void clear()
    {
        checkImmutable();

        values.clear();
    }

    /**
     * @return Whether calls that would affect the state of this instance will
     *         throw unchecked exceptions.
     */
    public boolean isImmutable()
    {
        return immutable;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (null == obj)
        {
            return false;
        }

        if (!(obj instanceof PropertyValues))
        {
            return false;
        }

        PropertyValues other = (PropertyValues) obj;

        return proxyDescriptor.equals(other.proxyDescriptor)
                && values.equals(other.values);
    }

    /**
     * @see java.lang.Object#hashCode()
     */

    @Override
    public int hashCode()
    {
        int result = 17;

        result = 37 * result + proxyDescriptor.hashCode();

        result = 37 * result + values.hashCode();

        return result;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @SuppressWarnings("unchecked")
    @Override
    public String toString()
    {
        return filteredToString(Collections.EMPTY_SET);
    }

    static final Map<String, PropertyDescriptor> mapProperties(BeanInfo beanInfo)
    {
        Map<String, PropertyDescriptor> byNames = new HashMap<String, PropertyDescriptor>();

        for (PropertyDescriptor propertyDescriptor : beanInfo
                .getPropertyDescriptors())
        {
            byNames.put(propertyDescriptor.getName(), propertyDescriptor);
        }

        return byNames;
    }

    PropertyDelegate<?> getPropertyDelegate(String propertyName)
    {
        return propertyDelegates.get(propertyName);
    }

    boolean isPropertyOf(String propertyName)
    {
        return isPropertyOf(this, propertyName);
    }

    String filteredToString(Set<String> filterToString)
    {
        // create a shallow map to filter out ignored properties, as well as to
        // consistently sort by the property names
        Map<String, Object> toPrint = new TreeMap<String, Object>(values);

        for (String ignoreName : filterToString)
        {
            toPrint.remove(ignoreName);
        }

        return String.format(toStringTemplate, toPrint);
    }

    boolean isAttached(String propertyName)
    {
        return propertyDelegates.containsKey(propertyName)
                && propertyDelegates.get(propertyName) != null;
    }

    void attach(String propertyName, PropertyDelegate<?> delegate)
    {
        checkImmutable();

        if (propertyDelegates.get(propertyName) != null)
        {
            LOGGER
                    .warn(String
                            .format(
                                    "PropertyDelegate already mapped for property, %1$s, on proxy, %2$s, overwriting!",
                                    propertyName, proxyDescriptor));
        }

        PropertyDescriptor propertyDescriptor = proxyDescriptor.propertyDescriptors
                .get(propertyName);

        PropertyValidator.validate(propertyName, propertyDescriptor, delegate);

        propertyDelegates.put(propertyName, delegate);
    }

    boolean detach(String propertyName)
    {
        return propertyDelegates.remove(propertyName) != null;
    }

    Object proxyToObject(String message, Method method, Object[] args)
    {
        return objectProxy.proxy(message, this, method, args);
    }

    boolean valuesEqual(PropertyValues propertyValues)
    {
        return this.values.equals(propertyValues.values);
    }

    private void checkImmutable()
    {
        if (!immutable)
        {
            return;
        }

        throw new UnsupportedOperationException(
                "This bean is immutable.  It was created with ProxyFactory.unmodifiableObject(), use ProxyFactory.copy() to create a copy safe to modify.");
    }

    private boolean putValue(PropertyExpression expression, Object propertyValue)
    {
        String propertyName = expression.getPropertyName();

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
                putValueWithBracket(get(propertyName), expression,
                        propertyValue);
            }
            else
            {
                PropertyValidator.validate(proxyDescriptor, propertyName,
                        propertyValue);

                values.put(propertyName, propertyValue);
            }

            return true;
        }

        Class<?> propertyType = propertyDescriptor.getPropertyType();

        Object interimValue = values.get(propertyName);

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
        return nestedHandler.propertyValues.putValue(expression.getChild(),
                propertyValue);
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

    private boolean isPropertyOf(PropertyValues propertyValues,
            final String propertyName)
    {
        int dotIndex = propertyName.indexOf('.');

        String shallowProperty = -1 == dotIndex ? propertyName : propertyName
                .substring(0, dotIndex);

        boolean indexedProperty = false;

        if (shallowProperty.endsWith("]") && shallowProperty.indexOf('[') != -1)
        {
            shallowProperty = propertyName.substring(0, propertyName
                    .indexOf('['));

            indexedProperty = true;
        }

        for (PropertyDescriptor propertyDescriptor : propertyValues.proxyDescriptor.propertyDescriptors
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
            if (indexedProperty)
            {
                if (!(propertyDescriptor instanceof IndexedPropertyDescriptor))
                {
                    return false;
                }

                IndexedPropertyDescriptor indexedDescriptor = (IndexedPropertyDescriptor) propertyDescriptor;

                return BeanManipulator.isPropertyOf(indexedDescriptor
                        .getIndexedPropertyType(), propertyName
                        .substring(dotIndex + 1));
            }

            Object nestedValue = propertyValues.values.get(shallowProperty);

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
                return isPropertyOf(nestedHandler.propertyValues, propertyName
                        .substring(dotIndex + 1));
            }
        }

        return false;
    }

    private boolean containsKey(PropertyExpression expression)
    {
        // if this is a leafy property, we're done
        if (expression.isLeaf() && !expression.isIndexed())
        {
            return values.containsKey(expression.getPropertyName());
        }

        Object interimValue = values.get(expression.getPropertyName());

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
            return nestedHandler.propertyValues.containsKey(expression
                    .getChild());
        }
    }

    private Object getValue(PropertyValues propertyValues,
            PropertyExpression currentExpression)
    {
        Object interimValue = propertyValues.values.get(currentExpression
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
            return PrimitiveSupport.getElement(values, index);
        }

        expression.validateArray(value);

        Object[] arrayValue = (Object[]) value;

        expression.validateArrayBounds(arrayValue.length);

        return arrayValue[index];
    }
}
