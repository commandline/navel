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
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to aid introspecting List typed properties, used exclusively by
 * {@link InitialValuesIndexedBuilder}.
 * 
 * @author cmdln
 * 
 */
class InitialValuesIndexedSupport
{

    private static final InitialValuesIndexedSupport SINGLETON = new InitialValuesIndexedSupport();

    private InitialValuesIndexedSupport()
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
            // add annotated types
            addFromAnnotation(elementTypes, descriptor);
            // add indexed types, either array or the element type
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
        // whether the descriptor is indexed is determined solely by presence of
        // either of both of the get(:int), set(:int,:?) methods
        if (!(descriptor instanceof IndexedPropertyDescriptor))
        {
            return;
        }

        IndexedPropertyDescriptor indexedDescriptor = (IndexedPropertyDescriptor) descriptor;

        // if this is not null, then it is an array type
        if (indexedDescriptor.getPropertyType() != null)
        {
            assert indexedDescriptor.getPropertyType().isArray() : "An indexed descriptor can only have a non-null return if it is an array type";

            elementTypes.put(descriptor.getName(), indexedDescriptor
                    .getPropertyType());
            
            return;
        }
        
        // if there is no plain return type, then the indexed type indicates the element type
        elementTypes.put(descriptor.getName(), indexedDescriptor.getIndexedPropertyType());
    }
}
