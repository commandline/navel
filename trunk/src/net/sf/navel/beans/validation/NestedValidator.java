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
package net.sf.navel.beans.validation;

import static net.sf.navel.beans.BeanManipulator.getNavelHandler;
import static net.sf.navel.beans.BeanManipulator.populate;

import java.beans.BeanInfo;
import java.beans.IndexedPropertyDescriptor;
import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.sf.navel.beans.DelegateBeanHandler;
import net.sf.navel.beans.InvalidPropertyValueException;
import net.sf.navel.beans.PropertyBeanHandler;
import net.sf.navel.beans.UnsupportedFeatureException;

import org.apache.log4j.Logger;

/**
 * This class contains extra intelligence to match nested property name prefixes
 * on the initial values and try to finesse them into the Map so they will work
 * properly.
 * 
 * @author lyon
 * 
 */
public class NestedValidator extends PropertyValidator
{

    private static final long serialVersionUID = 394546846709604632L;

    private static final Logger LOGGER = Logger
            .getLogger(NestedValidator.class);

    private final ListBuilder listBuilder;

    public NestedValidator(PropertyBeanHandler<?> handler)
    {
        super(handler);

        listBuilder = new ListBuilder(handler);
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
    @Override
    public void validateData(BeanInfo beanInfo)
            throws InvalidPropertyValueException
    {
        List<PropertyDescriptor> properties = getAllProperties(beanInfo);

        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace("Found properties:");
            LOGGER.trace(properties);
        }

        eliminateMatches(properties);

        if (values.isEmpty())
        {
            return;
        }

        Map<String, PropertyDescriptor> nameReference = mapNames(properties);

        resolveNested(nameReference);

        listBuilder.filter();

        if (!values.isEmpty())
        {
            throw new InvalidPropertyValueException(
                    "Extra values found in initial value map that do not match any known property for bean type "
                            + proxiedClass.getName() + ": " + values.keySet());
        }
    }

    /**
     * @see net.sf.navel.beans.validation.PropertyValidator#filterMethods(java.util.Set,
     *      java.beans.PropertyDescriptor)
     */
    @Override
    protected void filterMethods(Set<String> methodNames,
            PropertyDescriptor property)
    {
        super.filterMethods(methodNames, property);

        if (!(property instanceof IndexedPropertyDescriptor))
        {
            return;
        }

        IndexedPropertyDescriptor indexedProperty = (IndexedPropertyDescriptor) property;

        checkMethod(methodNames, indexedProperty.getIndexedReadMethod());
        checkMethod(methodNames, indexedProperty.getIndexedWriteMethod());
    }

    /**
     * @see net.sf.navel.beans.validation.PropertyValidator#checkValue(PropertyDescriptor,
     *      java.lang.String, java.lang.Object)
     */
    @Override
    protected void checkValue(PropertyDescriptor propertyDescriptor,
            String propertyName, Object propertyValue)
    {
        if (!(propertyDescriptor instanceof IndexedPropertyDescriptor))
        {
            super.checkValue(propertyDescriptor, propertyName, propertyValue);

            return;
        }

        if (!List.class.isAssignableFrom(propertyValue.getClass()))
        {
            throw new InvalidPropertyValueException(propertyValue + " of type "
                    + propertyValue.getClass().getName()
                    + " is not a valid value for use with indexed property "
                    + propertyName + ".");
        }
    }

    private Map<String, PropertyDescriptor> mapNames(
            List<PropertyDescriptor> properties)
    {
        Map<String, PropertyDescriptor> byNames = new HashMap<String, PropertyDescriptor>(
                properties.size());

        for (int i = 0; i < properties.size(); i++)
        {
            byNames.put(properties.get(i).getName(), properties.get(i));
        }

        return byNames;
    }

    private void resolveNested(Map<String, PropertyDescriptor> nameReference)
            throws InvalidPropertyValueException
    {
        // IMPROVE introduce a parameter object similar to a variant for Map or
        // Bean
        Map<String, Object> collapsed = new HashMap<String, Object>();
        Set<String> toRemove = new HashSet<String>();

        // to allow the instance copy of values to be modified
        Set<Entry<String, Object>> entries = new HashSet<Entry<String, Object>>(
                values.entrySet());

        for (Iterator<Entry<String, Object>> entryIter = entries.iterator(); entryIter
                .hasNext();)
        {
            Entry<String, Object> entry = entryIter.next();

            String name = entry.getKey();

            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("Working on " + name + ".");
            }

            if (name.indexOf('.') < 0)
            {
                if (LOGGER.isTraceEnabled())
                {
                    LOGGER.trace("Flat property, " + name + ", continuing.");
                }

                continue;
            }

            resolveSingleNested(name, entry.getValue(), collapsed, toRemove);
        }

        // avoid the next bit, allow the caller to deal with this error
        // condition
        if (!values.isEmpty())
        {
            return;
        }

        // this should only remove nested names, like foo.bar
        handler.removeAll(toRemove);

        buildNestedBeans(handler.getValues(), nameReference, collapsed);

        handler.putAll(collapsed);
    }

    @SuppressWarnings("unchecked")
    private void resolveSingleNested(String name, Object value,
            Map<String, Object> collapsed, Set<String> toRemove)
            throws InvalidPropertyValueException
    {

        String[] split = name.split("\\.");

        if (split.length > 2)
        {
            throw new InvalidPropertyValueException(
                    "Currently unable to recursively deal with nested properties, "
                            + name);
        }

        String parentName = split[0];

        String nestedName = split[1];

        Map<String, Object> nestedValues = null;

        if (collapsed.containsKey(parentName))
        {
            // IMRPOVE see not where collapsed is initialized
            nestedValues = (Map<String, Object>) collapsed.get(parentName);
        }
        else
        {
            nestedValues = new HashMap<String, Object>();

            collapsed.put(parentName, nestedValues);
        }

        nestedValues.put(nestedName, value);

        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace("Adding " + value + " to " + nestedName
                    + " for parent " + parentName + ".");
        }

        toRemove.add(name);

        values.remove(name);
    }

    @SuppressWarnings("unchecked")
    private void buildNestedBeans(Map<String, Object> parentValues,
            Map<String, PropertyDescriptor> nameReference,
            Map<String, Object> collapsed) throws InvalidPropertyValueException
    {
        // IMPROVE see not where collapsed is initialized
        // copy so we can iterate the copy and use it to modify the original map
        Set<Entry<String, Object>> entries = new HashSet<Entry<String, Object>>(
                collapsed.entrySet());

        for (Iterator<Entry<String, Object>> entryIter = entries.iterator(); entryIter
                .hasNext();)
        {
            Entry<String, Object> entry = entryIter.next();

            String name = entry.getKey();

            PropertyDescriptor descriptor = nameReference.get(name);

            if (null == descriptor)
            {
                if (LOGGER.isDebugEnabled())
                {
                    LOGGER
                            .debug("Could not find descriptor for property named "
                                    + name);
                }

                continue;
            }

            Class propClass = descriptor.getPropertyType();

            if (!propClass.isInterface())
            {
                throw new InvalidPropertyValueException(
                        "Nested property, "
                                + name
                                + ", must currently be a Navel bean to allow automatic instantiation.");
            }

            // IMPROVE see note where collapsed is initialized
            Map<String, Object> values = (Map<String, Object>) entry.getValue();

            try
            {
                // we don't need type, here
                Object nestedValue = buildOrReuse(parentValues, name,
                        propClass, values);

                collapsed.put(name, nestedValue);
            }
            catch (UnsupportedFeatureException e)
            {
                throw new InvalidPropertyValueException(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T buildOrReuse(Map<String, Object> parentValues, String name,
            Class<T> propClass, Map<String, Object> values)
            throws InvalidPropertyValueException, UnsupportedFeatureException
    {
        if (!parentValues.containsKey(name))
        {
            // following the parent type is a reasonable guess, especially since
            // the delegation checking can be done leniently, now
            if (handler instanceof DelegateBeanHandler)
            {
                return new DelegateBeanHandler<T>(propClass, values, false,
                        false).getProxy();
            }
            else
            {
                return new PropertyBeanHandler<T>(propClass, values, false)
                        .getProxy();
            }
        }

        // if the value is not T, then we are in trouble
        T nestedValue = (T) parentValues.get(name);

        // again, if the handler does not expect the matched type, T, we are
        // in serious trouble
        PropertyBeanHandler<T> handler = (PropertyBeanHandler<T>) getNavelHandler(nestedValue);

        if (null == handler)
        {
            populate(nestedValue, values);
        }
        else
        {
            handler.putAll(values);
        }

        return nestedValue;
    }
}
