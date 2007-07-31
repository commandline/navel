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

import static net.sf.navel.beans.BeanManipulator.populate;

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

/**
 * This class contains extra intelligence to match nested property name prefixes
 * on the initial values and try to finesse them into the Map so they will work
 * properly.
 * 
 * @author cmdln
 * 
 */
class PropertyValueResolver
{

    private static final Logger LOGGER = Logger
            .getLogger(PropertyValueResolver.class);

    private static final PropertyValueResolver SINGLETON = new PropertyValueResolver();

    private PropertyValueResolver()
    {
        // enforce Singleton pattern
    }

    /**
     * Ensure that the map of initial values is valid per the properties
     * described by the proxied class.
     */
    static void resolve(Map<String, PropertyDescriptor> properties,
            Map<String, Object> values)
    {
        SINGLETON.resolveAll(properties, values);
    }

    private void resolveAll(Map<String, PropertyDescriptor> properties,
            Map<String, Object> values)
    {
        if (values.isEmpty())
        {
            return;
        }

        resolveNested(properties, values);

        ListBuilder.filter(properties, values);
    }

    private void resolveNested(Map<String, PropertyDescriptor> properties, Map<String, Object> values)
    {
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

        // this should only remove nested names, like foo.bar
        values.keySet().removeAll(toRemove);

        buildNestedBeans(properties, values, collapsed);

        values.putAll(collapsed);
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
            LOGGER.trace(String.format(
                    "Adding value, %1$s, to property, %2$s, for parent, %3$s.",
                    value, nestedName, parentName));
        }

        toRemove.add(name);
    }

    @SuppressWarnings("unchecked")
    private void buildNestedBeans(Map<String, PropertyDescriptor> properties, Map<String, Object> parentValues,
            Map<String, Object> collapsed) throws InvalidPropertyValueException
    {
        // copy so we can iterate the copy and use it to modify the original map
        Set<Entry<String, Object>> entries = new HashSet<Entry<String, Object>>(
                collapsed.entrySet());

        for (Iterator<Entry<String, Object>> entryIter = entries.iterator(); entryIter
                .hasNext();)
        {
            Entry<String, Object> entry = entryIter.next();

            String name = entry.getKey();

            PropertyDescriptor descriptor = properties.get(name);

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

            Class<?> propertyType = descriptor.getPropertyType();

            if (!propertyType.isInterface())
            {
                throw new InvalidPropertyValueException(
                        String
                                .format(
                                        "Nested property, %1$s, must currently be an interface to allow automatic instantiation.  Was of type, %2$s.",
                                        name, propertyType.getName()));
            }

            Map<String, Object> values = (Map<String, Object>) entry.getValue();

            try
            {
                // we don't need type, here
                Object nestedValue = buildOrReuse(parentValues, name,
                        propertyType, values);

                collapsed.put(name, nestedValue);
            }
            catch (UnsupportedFeatureException e)
            {
                throw new InvalidPropertyValueException(e);
            }
        }
    }

    private Object buildOrReuse(Map<String, Object> parentValues, String name,
            Class<?> propClass, Map<String, Object> values)
            throws InvalidPropertyValueException, UnsupportedFeatureException
    {
        // create a new Navel bean, the parent map doesn't have one, yet
        if (!parentValues.containsKey(name))
        {
            return NestedBeanFactory.create(name, propClass, values);
        }

        Object nestedValue = parentValues.get(name);

        JavaBeanHandler handler = ProxyFactory.getHandler(nestedValue);

        if (null == handler)
        {
            populate(nestedValue, values);
        }
        else
        {
            handler.propertyValues.putAll(values);
        }

        return nestedValue;
    }
}
