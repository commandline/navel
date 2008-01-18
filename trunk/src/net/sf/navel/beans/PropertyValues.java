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
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
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
            PropertyValuesExpander.expand(findNestedProxies(), copy);
        }

        return copy;
    }

    /**
     * Interrogate the registered {@link PropertyDelegate} instances and invoke
     * their {@link PropertyDelegate#get(PropertyValues, String)} method to
     * generate a {@link Map} of synthetic values.
     * 
     * @param flatten
     *            Whether the delegates on any nested proxies should also be
     *            resolved and flattened into the output {@link Map}.
     * @return A {@link Map} of just the returns of
     *         {@link PropertyDelegate#get(PropertyValues, String)} keyed by the
     *         properties to which the respective delegates are registered.
     */
    public Map<String, Object> resolveDelegates(boolean flatten)
    {
        Map<String, Object> resolved = new HashMap<String, Object>();

        for (Entry<String, PropertyDelegate<?>> delegateEntry : propertyDelegates
                .entrySet())
        {
            String propertyName = delegateEntry.getKey();

            PropertyDelegate<?> propertyDelegate = delegateEntry.getValue();

            Object delegatedValue = propertyDelegate.get(this, propertyName);

            resolved.put(propertyName, delegatedValue);
        }

        if (flatten)
        {
            PropertyValuesExpander.resolve(findNestedProxies(), resolved);
        }

        return resolved;
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

        SingleValueResolver.put(this, dotExpression, value);
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
        return SingleValueResolver.get(this, dotExpression);
    }

    public Object resolve(String dotExpression)
    {
        return SingleValueResolver.resolve(this, dotExpression);
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
        return SingleValueResolver.containsKey(this, dotExpression);
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
    public Object remove(String dotExpression)
    {
        checkImmutable();

        return SingleValueResolver.remove(this, dotExpression);
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
        return SingleValueResolver.isPropertyOf(this, propertyName);
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

    boolean isAttached(String dotExpression)
    {

        if (!isPropertyOf(dotExpression))
        {
            throw new InvalidExpressionException(
                    String
                            .format(
                                    "The property, %1$s, is not a valid property of the bean, %2$s.",
                                    dotExpression, proxyDescriptor));
        }

        PropertyExpression leafProperty = new DotNotationExpression(
                dotExpression).getLeaf();

        // if the expression has no nested properties, look in this instance
        if (leafProperty.isRoot())
        {
            return propertyDelegates.containsKey(dotExpression)
                    && propertyDelegates.get(dotExpression) != null;
        }

        JavaBeanHandler parentHandler = SingleValueResolver.getParentOf(this,
                leafProperty);

        if (null == parentHandler)
        {
            return false;
        }

        String leafPropertyName = leafProperty.getPropertyName();

        Map<String, PropertyDelegate<?>> parentPropertyDelegates = parentHandler.propertyValues.propertyDelegates;

        if (null == parentPropertyDelegates)
        {
            return false;
        }

        // then resolve the leaf property against its immediate parent
        return parentPropertyDelegates.containsKey(leafPropertyName)
                && parentPropertyDelegates.get(leafPropertyName) != null;
    }

    void attach(String dotExpression, PropertyDelegate<?> delegate)
    {
        checkImmutable();

        PropertyExpression leafProperty = new DotNotationExpression(
                dotExpression).getLeaf();

        // if the expression has no nested properties, look in this instance
        if (leafProperty.isRoot())
        {
            warnReattach(propertyDelegates, leafProperty.getPropertyName(),
                    proxyDescriptor);

            validateAttach(proxyDescriptor, leafProperty.getPropertyName(),
                    delegate);

            propertyDelegates.put(leafProperty.getPropertyName(), delegate);

            return;
        }

        JavaBeanHandler parentHandler = SingleValueResolver.getParentOf(this,
                leafProperty);

        if (null == parentHandler)
        {
            LOGGER.warn(String.format(
                    "Target of attachment, %1$s, is invalid.", leafProperty
                            .getParent().expressionToRoot()));

            return;
        }

        Map<String, PropertyDelegate<?>> parentDelegates = parentHandler.propertyValues.propertyDelegates;

        warnReattach(parentDelegates, leafProperty.getPropertyName(),
                parentHandler.propertyValues.proxyDescriptor);

        validateAttach(parentHandler.propertyValues.proxyDescriptor,
                leafProperty.getPropertyName(), delegate);

        parentDelegates.put(leafProperty.getPropertyName(), delegate);
    }

    boolean detach(String dotExpression)
    {
        checkImmutable();

        PropertyExpression leafProperty = new DotNotationExpression(
                dotExpression).getLeaf();

        // if the expression has no nested properties, look in this instance
        if (leafProperty.isRoot())
        {

            PropertyDelegate<?> delegate = propertyDelegates
                    .remove(leafProperty.getPropertyName());

            return delegate != null;
        }

        JavaBeanHandler parentHandler = SingleValueResolver.getParentOf(this,
                leafProperty);

        if (null == parentHandler)
        {
            LOGGER.warn(String.format(
                    "Target of attachment, %1$s, is invalid.", leafProperty
                            .getParent().expressionToRoot()));

            return false;
        }

        Map<String, PropertyDelegate<?>> parentDelegates = parentHandler.propertyValues.propertyDelegates;

        PropertyDelegate<?> delegate = parentDelegates.remove(leafProperty
                .getPropertyName());

        return delegate != null;
    }

    Object proxyToObject(String message, Method method, Object[] args)
    {
        return objectProxy.proxy(message, this, method, args);
    }

    boolean valuesEqual(PropertyValues propertyValues)
    {
        return this.values.equals(propertyValues.values);
    }

    void putInternal(String property, Object value)
    {
        values.put(property, value);
    }

    Object getInternal(String property)
    {
        return values.get(property);
    }

    Object resolveInternal(String propertyName)
    {
        PropertyDelegate<?> delegate = propertyDelegates.get(propertyName);

        if (null == delegate)
        {
            return null;
        }

        return delegate.get(this, propertyName);
    }

    Object removeInternal(String propertyName)
    {
        return values.remove(propertyName);
    }

    boolean containsKeyInternal(String property)
    {
        return values.containsKey(property);
    }

    private Map<String, PropertyValues> findNestedProxies()
    {
        Map<String, PropertyValues> nestedProxies = new HashMap<String, PropertyValues>();

        for (Entry<String, Object> entry : values.entrySet())
        {
            Object value = entry.getValue();

            if (null == value)
            {
                continue;
            }

            JavaBeanHandler handler = ProxyFactory.getHandler(value);

            if (null == handler)
            {
                continue;
            }

            nestedProxies.put(entry.getKey(), handler.propertyValues);
        }

        return nestedProxies;
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

    private static void validateAttach(ProxyDescriptor proxyDescriptor,
            String propertyName, PropertyDelegate<?> delegate)
    {
        PropertyDescriptor propertyDescriptor = proxyDescriptor.propertyDescriptors
                .get(propertyName);

        PropertyValidator.validate(propertyName, propertyDescriptor, delegate);
    }

    private static void warnReattach(
            Map<String, PropertyDelegate<?>> propertyDelegates,
            String propertyName, ProxyDescriptor proxyDescriptor)
    {

        if (propertyDelegates.get(propertyName) == null)
        {
            return;
        }

        LOGGER
                .warn(String
                        .format(
                                "PropertyDelegate already mapped for property, %1$s, on proxy, %2$s, overwriting!",
                                propertyName, proxyDescriptor));
    }
}
