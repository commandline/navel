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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * This handler adds the ability to define delegates for methods that do not
 * deal strictly with properties. It leverages PropertyBeanHandler to handle
 * properties, but can delegate other calls through to the provided classes.
 * 
 * @author cmdln
 */
public class MethodHandler implements Serializable
{

    private static final long serialVersionUID = 3778154657912211158L;

    private static final Logger LOGGER = Logger.getLogger(MethodHandler.class);

    /**
     * The default behavior for checking method delegation at initialization and
     * re-validation.
     */
    public static final boolean DEFAULT_INIT_CHECK = true;

    private final Map<Class<?>, DelegationTarget> delegations = new HashMap<Class<?>, DelegationTarget>();

    /**
     * Constructor used for the same purpose as the simpler version, with the
     * addition of setting the initial property values to the Map argument.
     * 
     * @param proxiedInterfaces
     *            Interfaces to support.
     * @param delegates
     *            Objects to delegate to when invoking methods other than
     *            property manipulators.
     * @throws UnsupportedFeatureException
     *             Thrown if the proxied class describes any events or any of
     *             it's non property-oriented methods do not have delegates
     *             provided.
     */
    MethodHandler(Set<Class<?>> proxiedInterfaces, DelegationTarget[] delegates)
    {
        delegations.keySet().addAll(
                Collections.unmodifiableSet(proxiedInterfaces));
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

        Class proxiedInterface = method.getDeclaringClass();

        Object delegate = delegations.get(proxiedInterface);

        if (null == delegate)
        {
            throw new IllegalStateException(String.format(
                    "No delegate found for interface, %1$s!", proxiedInterface
                            .getName()));
        }

        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace(String.format("Found delegate for %1$s.",
                    proxiedInterface.getName()));
        }

        return method.invoke(delegate, args);
    }

    boolean isAttached(Class<?> interfaceType)
    {
        if (!delegations.containsKey(interfaceType))
        {
            return false;
        }

        return delegations.get(interfaceType) != null;
    }

    /**
     * This method allows for attaching delegates at runtime.
     * 
     * @param toAttach
     *            New delegates to add to the handler.
     */
    void attach(Class<?> interfaceType, DelegationTarget toAttach)
    {
        if (null == toAttach)
        {
            throw new IllegalArgumentException("Cannot attach null delegate!");
        }

        delegations.put(interfaceType, toAttach);
    }
}
