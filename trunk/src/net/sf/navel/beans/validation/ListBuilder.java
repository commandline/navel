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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.navel.beans.BeanManipulator;
import net.sf.navel.beans.DelegateBeanHandler;
import net.sf.navel.beans.PropertyBeanHandler;

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
class ListBuilder implements Serializable
{

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager
            .getLogger(ListBuilder.class);

    final PropertyBeanHandler<?> handler;

    ListBuilder(PropertyBeanHandler<?> handler)
    {
        // prevent instantiation outside this package

        this.handler = handler;
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
    void filter()
    {
        final Map<String, Object> original = Collections
                .unmodifiableMap(handler.getValues());

        // create a copy that can be modified as the original is iterated
        Map<String, Object> copy = new HashMap<String, Object>(original);

        // find the element types for any List properties, does so by
        // looking for methods with the same name as the List accessor but
        // that also accept an int or Integer argument
        Map<String, Class<?>> elementTypes = ListPropertySupport
                .introspectListTypes(handler.getProxiedClass());

        Set<String> flattened = new HashSet<String>();

        Set<String> toRemove = new HashSet<String>();

        for (Iterator<Entry<String, Object>> entryIter = original.entrySet()
                .iterator(); entryIter.hasNext();)
        {
            Entry<String, Object> entry = entryIter.next();

            String flatKey = entry.getKey();

            copy.remove(flatKey);

            if (flatKey.indexOf('[') == -1 && flatKey.indexOf("]") == -1)
            {
                continue;
            }

            String[] tokens = parseFlat(flatKey);

            String propertyKey = tokens[0];

            toRemove.add(flatKey);

            String index = "";
            String nestedKey = null;

            if (tokens.length == 2)
            {
                index = tokens[1];
            }
            else
            {
                index = tokens[1];
                nestedKey = stripLeadingDot(tokens[2]);
            }

            Object value = entry.getValue();

            // there is no further property munging required,
            if (null == nestedKey || nestedKey.length() == 0)
            {
                handleWholeNested(copy, propertyKey, index, elementTypes
                        .get(propertyKey), value);

                continue;
            }

            // add the value to a submap keyed on the nested property token
            handlePartialNested(copy, flatKey, propertyKey, index, nestedKey,
                    value);

            flattened.add(propertyKey);
        }

        if (toRemove.isEmpty())
        {
            return;
        }

        handler.removeAll(toRemove);

        expandFlattened(copy, elementTypes, flattened);

        handler.putAll(copy);
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
                LOGGER.warn("No nested list available for property, " + key
                        + ".");

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
                            "Element, "
                                    + i
                                    + " in constructed List is neither a Map or correctly assignable to the List generic type, "
                                    + elementType.getName() + "; element is "
                                    + rawElement.getClass().getName() + ": "
                                    + rawElement);
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
            final String propertyKey, final String index,
            final Class<?> valueType, final Object value)
    {
        List<Object> nestedList = initializeList(copy, propertyKey);

        Object element = value;

        if (value instanceof Map)
        {
            if (null == valueType)
            {
                throw new IllegalArgumentException(
                        "Not type found for elements of List type property, "
                                + propertyKey
                                + ".  Make sure the property name is correctly spelled in the raw values Map.");
            }

            element = instantiate(valueType, (Map<String, Object>) value);
        }

        // add if the index is empty or unknown
        if ("".equals(index) || "?".equals(index))
        {
            nestedList.add(element);

            return;
        }

        // set if the index is not empty
        int parsedIndex = parseIndex(propertyKey, index);

        pad(nestedList, parsedIndex);

        nestedList.set(parsedIndex, element);
    }

    /**
     * This method handles elements where the value instance is a flattened key
     * into a value sub-map that needs to be assembled into the List element and
     * later wrapped with an appropriate Navel handler.
     */
    @SuppressWarnings("unchecked")
    private void handlePartialNested(Map<String, Object> copy, String flatKey,
            String propertyKey, String index, String nestedKey, Object value)
    {
        if (index.trim().length() == 0)
        {
            throw new IllegalArgumentException(
                    "For a fully flattened entry, "
                            + propertyKey
                            + ", an index value must be provided in the brackets ([]) "
                            + "or the related entries cannot be assembled correctly into appropriate elements of the list property!");
        }

        List<Object> nestedList = initializeList(copy, propertyKey);

        int parsedIndex = parseIndex(propertyKey, index);

        pad(nestedList, parsedIndex);

        Map<String, Object> subMap = (Map<String, Object>) nestedList
                .get(parsedIndex);

        if (null == subMap)
        {
            subMap = new HashMap<String, Object>();

            nestedList.set(parsedIndex, subMap);
        }

        // put the value on the submap with the nested property
        subMap.put(nestedKey, value);
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
            if (handler.getValues().containsKey(propertyKey))
            {
                // be sure to re-use an existing, already resolved list in the
                // original bean contents, if available
                nestedList = new ArrayList<Object>((List<Object>) handler
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
                PropertyBeanHandler<?> handler = new DelegateBeanHandler(
                        elementType, rawValues, true, false);

                return handler.getProxy();
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

    private String[] parseFlat(final String flatKey)
    {
        Pattern pattern = Pattern.compile("\\[.*\\]");

        Matcher matcher = pattern.matcher(flatKey);

        boolean found = matcher.find();

        if (!found)
        {
            return new String[]
            { flatKey };
        }

        String property = flatKey.substring(0, matcher.start());
        String index = matcher.group();
        index = index.substring(1, index.length() - 1);

        if (matcher.end() < flatKey.length())
        {
            return new String[]
            { property, index, flatKey.substring(matcher.end() + 1) };
        }
        else
        {
            return new String[]
            { property, index };
        }
    }

    private int parseIndex(String propertyKey, String index)
    {
        try
        {
            return Integer.parseInt(index);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException(
                    "Index value, "
                            + index
                            + ", could not be parsed as a number for use with List property, "
                            + propertyKey + ".", e);
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

    private String stripLeadingDot(String toTrim)
    {
        if (!toTrim.startsWith("."))
        {
            return toTrim;
        }

        return toTrim.substring(1);
    }
}
