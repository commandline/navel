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
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to aid introspecting List typed properties.
 * 
 * @author cmdln
 * 
 */
class ListPropertySupport
{

    private static final ListPropertySupport SINGLETON = new ListPropertySupport();

    private ListPropertySupport()
    {
        // enforce Singleton pattern
    }

    /**
     * Identify the properties that are of type List that Navel can support,
     * associating the type of the elements within the List in the resulting
     * Map.
     */
    static Map<String, Class<?>> introspectListTypes(
            Map<String, PropertyDescriptor> properties)
    {
        return SINGLETON.getElementTypes(properties);
    }

    /**
     * For all the list properties on the bean class, find the specific types
     * meant to be contained within the list by locating alternate accessors who
     * take a single int or Integer argument that is the index for a get call
     * into the List itself.
     */
    private Map<String, Class<?>> getElementTypes(
            Map<String, PropertyDescriptor> properties)
    {
        Map<String, Class<?>> elementTypes = new HashMap<String, Class<?>>();

        for (PropertyDescriptor descriptor : properties.values())
        {
            addFromAnnotation(elementTypes, descriptor);
            addFromAlternateAccessor(elementTypes, descriptor);
        }

        return elementTypes;
    }

    private void addFromAnnotation(Map<String, Class<?>> elementTypes,
            PropertyDescriptor descriptor)
    {
        if (descriptor.getReadMethod() == null)
        {
            return;
        }

        CollectionType collectionType = descriptor.getReadMethod()
                .getAnnotation(CollectionType.class);

        if (null == collectionType)
        {
            return;
        }

        elementTypes.put(descriptor.getName(), collectionType.value());
    }

    private void addFromAlternateAccessor(Map<String, Class<?>> elementTypes,
            PropertyDescriptor descriptor)
    {
        Class<?> propertyType = descriptor.getPropertyType();

        if (propertyType != null && !List.class.isAssignableFrom(propertyType))
        {
            return;
        }

        Method readMethod = findRead(descriptor, true);

        if (null == readMethod)
        {
            return;
        }

        elementTypes.put(descriptor.getName(), readMethod.getReturnType());
    }

    private Method findRead(PropertyDescriptor descriptor,
            boolean primitiveArgument)
    {
        Class<?> beanClass = findBeanClass(descriptor);

        String name = descriptor.getName();

        String methodName = "get";

        methodName = methodName.concat(name.substring(0, 1).toUpperCase());
        methodName = methodName.concat(name.substring(1));

        try
        {
            if (primitiveArgument)
            {
                return beanClass.getMethod(methodName, int.class);
            }
            else
            {
                return beanClass.getMethod(methodName, Integer.class);
            }
        }
        catch (SecurityException e)
        {
            throw new IllegalArgumentException(e);
        }
        catch (NoSuchMethodException e)
        {
            if (primitiveArgument)
            {
                return findRead(descriptor, false);
            }
            else
            {
                return null;
            }
        }
    }

    private Class<?> findBeanClass(PropertyDescriptor descriptor)
    {
        Method method = descriptor.getWriteMethod();

        // may be read only
        if (null == method)
        {
            method = descriptor.getReadMethod();
        }

        if (method != null)
        {
            return method.getDeclaringClass();
        }

        if (!(descriptor instanceof IndexedPropertyDescriptor))
        {
            return null;
        }

        IndexedPropertyDescriptor indexedDescriptor = (IndexedPropertyDescriptor) descriptor;

        // may be indexed
        method = indexedDescriptor.getIndexedWriteMethod();

        // may be indexed read only
        if (null == method)
        {
            method = indexedDescriptor.getIndexedReadMethod();
        }

        Class<?> parent = method.getDeclaringClass();

        if (null == parent)
        {

            parent = indexedDescriptor.getReadMethod().getDeclaringClass();
        }

        return parent;
    }
}
