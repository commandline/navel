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

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Shared logic that {@link BeanManipulator} and {@link PropertyValues} use to
 * flatten out {@link Proxy} instances backed by {@link JavaBeanHandler}.
 * 
 * @author cmdln
 * 
 */
class PropertyValuesExpander
{

    private static final PropertyValuesExpander SINGLETON = new PropertyValuesExpander();

    private PropertyValuesExpander()
    {
        // enforce Singleton pattern
    }

    static void expand(Map<String, PropertyValues> nestedProxies,
            Map<String, Object> values)
    {
        SINGLETON.expandNestedBeans(nestedProxies, values);
        
        SINGLETON.expandNestedLists(values);
        
        SINGLETON.expandNestedArrays(values);
    }

    static void resolve(Map<String, PropertyValues> nestedProxies,
            Map<String, Object> values)
    {
        SINGLETON.resolveNestedBeans(nestedProxies, values);
    }

    private void expandNestedBeans(Map<String, PropertyValues> nestedProxies,
            Map<String, Object> values)
    {
        // to allow modification of the original map
        for (Entry<String, PropertyValues> entry : nestedProxies.entrySet())
        {
            PropertyValues nestedProxy = entry.getValue();

            if (null == nestedProxy)
            {
                continue;
            }

            // depth first recursion, will resolved all descendants in the
            // original map, first
            Map<String, Object> nestedValues = nestedProxy.copyValues(true);

            // re-write the keys for the immediate properties in the map, the
            // previous line will have taken care of the more deeply nested
            // properties
            nestedValues = prefixKeys(entry.getKey(), nestedValues);

            values.putAll(nestedValues);

            values.remove(entry.getKey());
        }
    }
    
    @SuppressWarnings("unchecked")
    private void expandNestedLists(Map<String,Object> values)
    {
        Map<String,Object> copy = new HashMap<String, Object>(values);
        
        for (Entry<String,Object> entry : copy.entrySet())
        {
            if (!(entry.getValue() instanceof List))
            {
                continue;
            }
            
            List<Object> nestedList = (List<Object>) entry.getValue();
            
            for (int index = 0; index < nestedList.size(); index++)
            {
                Object nestedElement = nestedList.get(index);

                expandElement(entry.getKey(), index, values, nestedElement);
            }
        }
    }
    
    private void expandNestedArrays(Map<String,Object> values)
    {
        Map<String,Object> copy = new HashMap<String, Object>(values);
        
        for (Entry<String,Object> entry : copy.entrySet())
        {
            if (!(entry.getValue() instanceof Object[]))
            {
                continue;
            }
            
            Object[] nestedArray = (Object[]) entry.getValue();
            
            for (int index = 0; index < nestedArray.length; index++)
            {
                Object nestedElement = nestedArray[index];

                expandElement(entry.getKey(), index, values, nestedElement);
            }
        }
    }
    
    private void expandElement(String key, int index, Map<String,Object> values, Object nestedElement)
    {
        if (null == nestedElement)
        {
            return;
        }
        
        if (ProxyFactory.getHandler(nestedElement) == null)
        {
            return;
        }
        
        Map<String,Object> nestedElementValues = ProxyManipulator.copyAll(nestedElement, true);
        
        nestedElementValues = prefixKeys(String.format("%1$s[%2$d]", key, index), nestedElementValues);
        
        values.putAll(nestedElementValues);
        
        values.remove(key);
    }

    private void resolveNestedBeans(Map<String, PropertyValues> nestedProxies,
            Map<String, Object> values)
    {
        for (Entry<String, PropertyValues> entry : nestedProxies.entrySet())
        {
            PropertyValues nestedProxy = entry.getValue();

            if (null == nestedProxy)
            {
                continue;
            }

            Map<String, Object> nestedValues = nestedProxy.resolveDelegates(true);

            nestedValues = prefixKeys(entry.getKey(), nestedValues);

            values.putAll(nestedValues);
        }
    }

    private Map<String, Object> prefixKeys(String prefix,
            Map<String, Object> values)
    {
        Map<String, Object> prefixed = new HashMap<String, Object>();

        for (Entry<String, Object> entry : values.entrySet())
        {
            prefixed.put(prefix.concat(".").concat(entry.getKey()), entry
                    .getValue());
        }

        return prefixed;
    }
}
