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
package net.sf.navel.example;

import java.util.Collection;
import java.util.Map;

import net.sf.navel.beans.ConstructionDelegate;
import net.sf.navel.beans.ProxyFactory;

/**
 * @author thomas
 * 
 */
public class NestedConstructor implements ConstructionDelegate
{

    /**
     * @see net.sf.navel.beans.ConstructionDelegate#additionalTypes(int,
     *      java.lang.Class, java.lang.Class, java.lang.Class<?>[],
     *      java.util.Map)
     */

    public Collection<Class<?>> additionalTypes(int nestingDepth,
            Class<?> thisType, Class<?> primaryType, Class<?>[] allTypes,
            Map<String, Object> initialValues)
    {
        // do nothing
        return null;
    }

    /**
     * @see net.sf.navel.beans.ConstructionDelegate#initValues(int, Class,
     *      Object)
     */

    public void initValues(int nestingDepth, Class<?> thisType, Object bean)
    {
        if (nestingDepth > 1)
        {
            return;
        }

        // only initialize one level deep
        NestedBean nestedBean = (NestedBean) bean;

        nestedBean.setNested(ProxyFactory.createAs(TypesBean.class));
    }

    /**
     * @see net.sf.navel.beans.ConstructionDelegate#initBehaviors(int,
     *      java.lang.Class, java.lang.Object)
     */

    public void initBehaviors(int nestingDepth, Class<?> thisType, Object bean)
    {
        // do nothing
    }
}
