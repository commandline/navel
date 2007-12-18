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

import java.util.ArrayList;

/**
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

    static Object copy(Object source, boolean deep, boolean immutableValues)
    {
        return SINGLETON.copyObject(source, deep, immutableValues);
    }
    
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

        JavaBeanHandler sourceHandler = getHandler(source);

        ProxyDescriptor sourceDescriptor = sourceHandler.propertyValues.getProxyDescriptor();

        JavaBeanHandler newHandler = new JavaBeanHandler(sourceHandler, deep,
                immutableValues);

        Class<?>[] copyTypes = new ArrayList<Class<?>>(sourceDescriptor
                .getProxiedInterfaces()).toArray(new Class<?>[sourceDescriptor
                .getProxiedInterfaces().size()]);

        return ProxyCreator.create(newHandler, null, null, copyTypes,
                new InterfaceDelegate[0]);
    }

    private Object copySubset(Object source, boolean deep, Class<?>[] subTypes)
    {
        if (null == source)
        {
            return null;
        }
        
        JavaBeanHandler sourceHandler = getHandler(source);

        return null;
    }
    
    private JavaBeanHandler getHandler(Object bean)
    {
        JavaBeanHandler handler = ProxyFactory.getHandler(bean);

        if (null == handler)
        {
            throw new UnsupportedFeatureException(
                    "Cannot copy anything other than a Navel bean!");
        }
        
        return handler;
    }
}
