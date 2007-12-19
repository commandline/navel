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

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Utility class for dealing with flattened property maps that use the bracket
 * ([]) operator with an index to place nested values at specific elements of a
 * List property.
 * 
 * @author cmdln
 * 
 */
class ListBuilder
{

    private static final Logger LOGGER = LogManager
            .getLogger(ListBuilder.class);

    private static final ListBuilder SINGLETON = new ListBuilder();

    private ListBuilder()
    {
        // enforce Singleton pattern
    }

    /**
     * Finds all of the properties assignable to the List interface and
     * deconstructs any keys in the original value map that use the bracket
     * operator, expanding the List property into a correctly populated list.
     * This method relies on there being two accessors for the List property,
     * one with no arguments that returns the List itself and another that
     * accepts an int or Integer argument and returns the type of the elements
     * within the List.
     */
    static void filter(Map<String, PropertyDescriptor> properties,
            Map<String, Object> values)
    {
        SINGLETON.filterLists(properties, values);
    }

    private void filterLists(Map<String, PropertyDescriptor> properties,
            Map<String, Object> values)
    {
        final Map<String, Object> original = Collections
                .unmodifiableMap(values);

        // create a copy that can be modified as the original is iterated
        Map<String, Object> copy = new HashMap<String, Object>(original);

        // find the element types for any List properties, does so by
        // looking for methods with the same name as the List accessor but
        // that also accept an int or Integer argument
        Map<String, Class<?>> elementTypes = ListPropertySupport
                .introspectListTypes(properties);

        Set<String> toExpand = new HashSet<String>();

        Set<String> toRemove = new HashSet<String>();

        for (Iterator<Entry<String, Object>> entryIter = original.entrySet()
                .iterator(); entryIter.hasNext();)
        {
            Entry<String, Object> entry = entryIter.next();

            Object value = entry.getValue();

            String originalExpression = entry.getKey();

            if (!(value instanceof List))
            {
                copy.remove(originalExpression);
            }

            if (originalExpression.indexOf('[') == -1
                    && originalExpression.indexOf("]") == -1)
            {
                continue;
            }

            ListElementDescriptor elementDescriptor = ListElementDescriptor
                    .describe(originalExpression);
            
            toRemove.add(originalExpression);

            // there is no further property munging required,
            if (!elementDescriptor.hasNestedProperty())
            {
                handleWholeNested(copy, elementDescriptor, elementTypes
                        .get(elementDescriptor.getParentProperty()), value);

                continue;
            }

            // add the value to a submap keyed on the nested property token
            handlePartialNested(copy, originalExpression, elementDescriptor,
                    value);

            toExpand.add(elementDescriptor.getParentProperty());
        }

        if (toRemove.isEmpty())
        {
            return;
        }

        values.keySet().removeAll(toRemove);

        expandFlattened(copy, elementTypes, toExpand);

        values.putAll(copy);
    }

    /**
     * After the initial filtering pass, the value copy will have populated List
     * instances for each of the List properties, some of the elements within
     * the lists may already be first class objects, but some may be value
     * sub-maps that need to be wrapped with Navel handlers and initialized
     * correctly, as well.
     */
    @SuppressWarnings("unchecked")
    private void expandFlattened(Map<String, Object> copy,
            Map<String, Class<?>> elementTypes, Set<String> flattened)
    {
        // iterate the flattened set
        for (String key : flattened)
        {
            // iterate the list for each entry
            List<Object> nestedList = (List<Object>) copy.get(key);

            if (null == nestedList)
            {
                LOGGER.warn(String.format(
                        "No nested list available for property, %1$s.", key));

                continue;
            }

            for (int i = 0; i < nestedList.size(); i++)
            {
                Object rawElement = nestedList.get(i);

                if (null == rawElement)
                {
                    continue;
                }

                Class<?> elementType = elementTypes.get(key);

                if (elementType.isAssignableFrom(rawElement.getClass()))
                {
                    continue;
                }

                if (elementType.isAssignableFrom(Map.class))
                {
                    throw new IllegalStateException(
                            String
                                    .format(
                                            "Element, %1$s, in constructed List is neither a Map or correctly assignable to the List generic type, %2$s; element is %3$s: %4$s",
                                            i, elementType.getName(),
                                            rawElement.getClass().getName(),
                                            rawElement));
                }

                Map<String, Object> rawValues = (Map<String, Object>) rawElement;

                Object expandedValued = instantiate(elementType, rawValues);

                nestedList.set(i, expandedValued);
            }
        }
    }

    /**
     * This method handles elements where the value instance is a whole object,
     * already.
     */
    @SuppressWarnings("unchecked")
    private void handleWholeNested(Map<String, Object> copy,
            final ListElementDescriptor elementDescriptor,
            final Class<?> valueType, final Object value)
    {
        List<Object> nestedList = initializeList(copy, elementDescriptor.getParentProperty());

        Object element = value;

        if (value instanceof Map)
        {
            if (null == valueType)
            {
                throw new IllegalArgumentException(
                        String
                                .format(
                                        "No type found for elements of List type property, %1$s.  Make sure the property name is correctly spelled in the raw values Map and add the CollectionType annotation if necessary.",
                                        elementDescriptor.getParentProperty()));
            }

            element = instantiate(valueType, (Map<String, Object>) value);
        }

        // add if the index is empty or unknown
        if (!elementDescriptor.hasIndex())
        {
            nestedList.add(element);

            return;
        }

        // set if the index is not empty
        pad(nestedList, elementDescriptor.getIndex());

        nestedList.set(elementDescriptor.getIndex(), element);
    }

    /**
     * This method handles elements where the value instance is a flattened key
     * into a value sub-map that needs to be assembled into the List element and
     * later wrapped with an appropriate Navel handler.
     */
    @SuppressWarnings("unchecked")
    private void handlePartialNested(Map<String, Object> copy, String flatKey,
            ListElementDescriptor elementDescriptor, Object value)
    {
        if (!elementDescriptor.hasIndex())
        {
            throw new IllegalArgumentException(
                    String
                            .format(
                                    "For a fully flattened entry, %1$s, an index value must be provided in the brackets ([]) or the related entries cannot be assembled correctly into appropriate elements of the list property!",
                                    elementDescriptor.getParentProperty()));
        }

        List<Object> nestedList = initializeList(copy, elementDescriptor
                .getParentProperty());

        pad(nestedList, elementDescriptor.getIndex());

        Map<String, Object> subMap = (Map<String, Object>) nestedList
                .get(elementDescriptor.getIndex());

        if (null == subMap)
        {
            subMap = new HashMap<String, Object>();

            nestedList.set(elementDescriptor.getIndex(), subMap);
        }

        // put the value on the submap with the nested property
        subMap.put(elementDescriptor.getNestedProperty(), value);
    }

    @SuppressWarnings("unchecked")
    private List<Object> initializeList(Map<String, Object> copy,
            final String propertyKey)
    {
        List<Object> nestedList = null;

        if (copy.containsKey(propertyKey))
        {
            nestedList = (List<Object>) copy.get(propertyKey);
        }
        else
        {
            if (copy.containsKey(propertyKey))
            {
                // be sure to re-use an existing, already resolved list in the
                // original bean contents, if available
                nestedList = new ArrayList<Object>((List<Object>) copy
                        .get(propertyKey));
            }
            else
            {
                nestedList = new ArrayList<Object>();
            }

            copy.put(propertyKey, nestedList);
        }

        return nestedList;
    }

    @SuppressWarnings("unchecked")
    private Object instantiate(Class<?> elementType,
            Map<String, Object> rawValues)
    {
        try
        {
            if (elementType.isInterface())
            {
                Object elementValue = ProxyFactory.create(rawValues, null, new Class<?>[] { elementType });
                
                PropertyValueResolver.resolveNested(elementValue, rawValues);

                return elementValue;
            }
            else
            {
                Object expandedValue = elementType.newInstance();

                BeanManipulator.populate(expandedValue, rawValues);

                return expandedValue;
            }
        }
        catch (InstantiationException e)
        {
            throw new IllegalStateException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private void pad(List<Object> list, int index)
    {
        if (list.size() > index)
        {
            return;
        }

        int limit = index - (list.size() - 1);

        for (int i = 0; i < limit; i++)
        {
            list.add(null);
        }
    }
}
