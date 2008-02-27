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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Maps introspected methods that the proxied JavaBean must support to a
 * changeable set of delegates. Provides methods for interrogating and altering
 * the delegates backing particular sets of methods, grouped by the JavaBean's
 * interfaces.
 * 
 * @author cmdln
 * 
 */
class InterfaceDelegateMapping implements Serializable
{

    private static final long serialVersionUID = -4311211381597816297L;

    private static final Logger LOGGER = LogManager
            .getLogger(InterfaceDelegateMapping.class);

    final Map<Class<?>, InterfaceDelegate> delegations = new HashMap<Class<?>, InterfaceDelegate>();

    final ProxyDescriptor proxyDescriptor;

    private final PropertyValues values;

    /**
     * Internal constructor that builds the internal Map whose key set is based
     * on the total set of interfaces introspected from the construction of the
     * Proxy that JavaBeanHandler supports.
     * 
     * @param proxyDescriptor
     *            Wraps all the non-Serializable introspection data into an
     *            instance that will manually re-build itself on
     *            de-serialziation.
     * @param values
     *            Necessary to set onto each delegate so it can safely
     *            manipulate the internal state of the JavaBeanHandler.
     */
    InterfaceDelegateMapping(ProxyDescriptor proxyDescriptor,
            PropertyValues values)
    {
        this.values = values;
        this.proxyDescriptor = proxyDescriptor;

        for (Class<?> delegatingInterface : proxyDescriptor.withDelegatableMethods)
        {
            if (LOGGER.isTraceEnabled())
            {
                LOGGER.trace(String.format(
                        "Adding interface, %1$s, as delegatable.",
                        delegatingInterface.getName()));
            }

            // initialize to null since this is just setting up the fixed key
            // set
            delegations.put(delegatingInterface, null);
        }

        if (LOGGER.isTraceEnabled())
        {
            Set<Class<?>> nonDelegatable = proxyDescriptor
                    .getProxiedInterfaces();

            nonDelegatable.removeAll(proxyDescriptor.withDelegatableMethods);

            LOGGER
                    .trace(String
                            .format(
                                    "The following interfaces were skipped because they lacked delegatable methods, %1$s.",
                                    nonDelegatable));
        }
    }

    final void attach(InterfaceDelegate delegate)
    {
        InterfaceDelegateValidator.validate(delegate);

        Class<?> delegatingInterface = delegate.getDelegatingInterface();

        validateInterfaceArgument(delegatingInterface);

        if (LOGGER.isDebugEnabled())
        {
            if (delegations.get(delegatingInterface) != null)
            {
                LOGGER
                        .debug(String
                                .format(
                                        "InterfaceDelegate already mapped for interface, %1$s, on proxy, %2$s, overwriting!",
                                        delegatingInterface, proxyDescriptor));
            }
        }

        delegate.attach(values);
        delegations.put(delegatingInterface, delegate);
    }

    boolean isAttached(Class<?> interfaceType)
    {
        if (!proxyDescriptor.getProxiedInterfaces().contains(interfaceType))
        {
            throw new InvalidDelegateException(
                    String
                            .format(
                                    "The interface, %1$s, is not one supported by this proxy, %2$s!",
                                    interfaceType.getName(), proxyDescriptor));
        }

        validateInterfaceArgument(interfaceType);

        return delegations.get(interfaceType) != null;
    }

    boolean detach(Class<?> delegatingInterface)
    {
        validateInterfaceArgument(delegatingInterface);

        return delegations.remove(delegatingInterface) != null;
    }

    private void validateInterfaceArgument(Class<?> delegatingInterface)
    {
        if (delegations.containsKey(delegatingInterface))
        {
            return;
        }

        throw new IllegalArgumentException(
                String
                        .format(
                                "The interface, %1$s, is not a valid delegating interface supported by the proxy!  Supported delegating interfaces, %2$s.  If it is in the total set of interfaces supported by the proxy it may lack at least one behavioral method: %3$s.",
                                delegatingInterface.getName(),
                                printClasses(delegations.keySet()),
                                proxyDescriptor));

    }

    private String printClasses(Set<Class<?>> delegatingInterfaces)
    {
        Set<String> sortedInterfaces = new TreeSet<String>();

        for (Class<?> additionalInterface : delegatingInterfaces)
        {
            sortedInterfaces.add(additionalInterface.getName());
        }

        return sortedInterfaces.toString();
    }
}
