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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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

    static void expand(Map<String, Object> values)
    {
        SINGLETON.expandNestedBeans(values);
    }

    private void expandNestedBeans(Map<String, Object> values)
    {
        // to allow modification of the original map
        Set<Entry<String, Object>> entries = new HashSet<Entry<String, Object>>(
                values.entrySet());

        for (Iterator<Entry<String, Object>> entryIter = entries.iterator(); entryIter
                .hasNext();)
        {
            Entry<String, Object> entry = entryIter.next();

            JavaBeanHandler handler = ProxyFactory.getHandler(entry.getValue());

            if (null == handler)
            {
                continue;
            }

            // depth first recursion, will resolved all descendants in the
            // original map, first
            Map<String, Object> nestedValues = handler.propertyValues
                    .copyValues(true);

            // re-write the keys for the immediate properties in the map, the
            // previous line will have taken care of the more deeply nested
            // properties
            expandNestedBean(values, entry.getKey(), nestedValues);
        }
    }

    private void expandNestedBean(Map<String, Object> values, String key,
            Map<String, Object> nested)
    {
        for (Iterator<Entry<String, Object>> entryIter = nested.entrySet()
                .iterator(); entryIter.hasNext();)
        {
            Entry<String, Object> entry = entryIter.next();

            values.put(key + "." + entry.getKey(), entry.getValue());
        }

        values.remove(key);
    }
}
