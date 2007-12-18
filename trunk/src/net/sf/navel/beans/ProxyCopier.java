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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Support code for all of the copy operations, including generating views based
 * on a subset of a proxy's types as a copy.
 * 
 * @author cmdln
 * 
 */
class ProxyCopier
{

    private static final ProxyCopier SINGLETON = new ProxyCopier();

    private ProxyCopier()
    {
        // enforce Singleton pattern
    }

    /**
     * A fully type compatible copy, copies the {@link PropertyDelegate}
     * instances, too, so that the public properties as seen from the accessors,
     * mutators is consistent with the source.
     * 
     * @param source
     *            Object to copy.
     * @param deep
     *            Recurse and copy any nested Navel beans or only copy
     *            references.
     * @param immutableValues
     *            Whether the values portion of the copy, including the
     *            {@link PropertyDelegate} instances, is fixed so that trying to
     *            alter any portion of it throws an unchecked exception.
     * @return A copy that is consistent, value-wise, with the source.
     */
    static Object copy(Object source, boolean deep, boolean immutableValues)
    {
        return SINGLETON.copyObject(source, deep, immutableValues);
    }

    /**
     * A copy that implements a subset of the types implemented by the source
     * and whose value portion is filtered down to just those values that are
     * compatible with the new interface set. Will also copy references to valid
     * property delegates.
     * 
     * @param source
     *            Object to copy.
     * @param deep
     *            Recurse and copy any nested Navel beans or only copy
     *            references.
     * @param immutableValues
     *            Whether the values portion of the copy, including the
     *            {@link PropertyDelegate} instances, is fixed so that trying to
     *            alter any portion of it throws an unchecked exception.
     * @return A copy that is consistent, value-wise, with the source.
     */
    static Object subset(Object source, boolean deep, Class<?>... subTypes)
    {
        return SINGLETON.copySubset(source, deep, subTypes);
    }

    private Object copyObject(Object source, boolean deep,
            boolean immutableValues)
    {
        if (null == source)
        {
            return null;
        }

        JavaBeanHandler sourceHandler = ProxyFactory.getRequiredHandler(source,
                "Cannot copy anything other than a Navel bean!");

        ProxyDescriptor sourceDescriptor = sourceHandler.propertyValues
                .getProxyDescriptor();

        JavaBeanHandler newHandler = new JavaBeanHandler(sourceHandler, deep,
                immutableValues);

        Class<?>[] copyTypes = new ArrayList<Class<?>>(sourceDescriptor
                .getProxiedInterfaces()).toArray(new Class<?>[sourceDescriptor
                .getProxiedInterfaces().size()]);

        return ProxyCreator.create(newHandler, null, null, copyTypes);
    }

    private Object copySubset(Object source, boolean deep, Class<?>[] subTypes)
    {
        if (null == source)
        {
            return null;
        }

        JavaBeanHandler sourceHandler = ProxyFactory.getRequiredHandler(source,
                "Cannot copy anything other than a Navel bean!");

        ProxyDescriptor sourceDescriptor = sourceHandler.propertyValues
                .getProxyDescriptor();

        Collection<Class<?>> subTypesSet = new ArrayList<Class<?>>(Arrays
                .asList(subTypes));

        Set<Class<?>> sourceTypes = sourceDescriptor.getProxiedInterfaces();

        if (!sourceTypes.containsAll(subTypesSet))
        {
            throw new IllegalArgumentException(
                    String
                            .format(
                                    "The requested sub-types, %1$s, are not a property subset of those supported by the source, %1$s.",
                                    Arrays.deepToString(subTypes), source));
        }
        
        ProxyDescriptor proxyDescriptor = new ProxyDescriptor(subTypes);

        Map<String, Object> subValues = null;
        
        if (subTypesSet.containsAll(sourceTypes))
        {
            sourceHandler.propertyValues.copyValues(false);
        }
        else
        {
            subValues = filterValues(
                    proxyDescriptor, sourceHandler.propertyValues.copyValues(false));
        }
        
        PropertyValues propertyValues = new PropertyValues(proxyDescriptor, subValues);

        JavaBeanHandler newHandler = new JavaBeanHandler(propertyValues);

        return ProxyCreator.create(newHandler, null, null, subTypes);
    }

    private Map<String, Object> filterValues(ProxyDescriptor proxyDescriptor,
            Map<String, Object> sourceValues)
    {
        Map<String, Object> newValues = new HashMap<String, Object>();

        Map<String, PropertyDescriptor> propertyDescriptors = proxyDescriptor
                .getPropertyDescriptors();

        for (Entry<String, Object> sourceEntry : sourceValues.entrySet())
        {
            String sourceKey = sourceEntry.getKey();

            if (!propertyDescriptors.containsKey(sourceKey))
            {
                continue;
            }

            newValues.put(sourceKey, sourceEntry.getValue());
        }

        return newValues;
    }
}
