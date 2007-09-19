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

import java.io.Serializable;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;

/**
 * This class wires in the support for methods as found through JavaBeans
 * introspection.
 * 
 * @author cmdln
 */
public class MethodHandler implements Serializable
{

    private static final long serialVersionUID = 3778154657912211158L;

    private static final Logger LOGGER = Logger.getLogger(MethodHandler.class);

    private final InterfaceDelegateMapping mapping;

    MethodHandler(InterfaceDelegateMapping mapping)
    {
        this.mapping = mapping;
    }

    public boolean handles(Method method)
    {
        return mapping.proxyDescriptor.handles(method);
    }

    /**
     * The method required by the InvocationHandler interface. The
     * implementation here just supports manipulating the underlying map via the
     * bean property methods on the proxied interface.
     * 
     * @param proxy
     *            Class that was original called against.
     * @param method
     *            Method being invoked.
     * @param args
     *            Argument values.
     * @return Return value, must be null for void return types.
     */
    Object handle(Object proxy, Method method, Object[] args) throws Throwable
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(String.format("Invoking %1$s.", method.getName()));
        }

        Class<?> proxiedInterface = method.getDeclaringClass();

        Object delegate = mapping.delegations.get(proxiedInterface);

        if (null == delegate)
        {
            throw new IllegalStateException(String.format(
                    "No InterfaceDelegate instance found for interface, %1$s!", proxiedInterface
                            .getName()));
        }

        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace(String.format("Found delegate for %1$s.",
                    proxiedInterface.getName()));
        }
        
        Method delegateMethod = delegate.getClass().getMethod(method.getName(), method.getParameterTypes());

        return delegateMethod.invoke(delegate, args);
    }
}
