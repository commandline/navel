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

import java.beans.BeanInfo;
import java.beans.MethodDescriptor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
class DelegateMapping
{

    private static final Logger LOGGER = LogManager
            .getLogger(DelegateMapping.class);

    final Map<Class<?>, DelegationTarget> delegations = new HashMap<Class<?>, DelegationTarget>();

    final Set<Method> methods;

    private final PropertyValues values;

    /**
     * Internal constructor that builds the internal Map whose key set is based
     * on the total set of interfaces introspected from the construction of the
     * Proxy that JavaBeanHandler supports.
     * 
     * @param proxiedBeanInfo
     *            All of the JavaBean interfaces the handler supports,
     *            established the total and immutable set for validating new
     *            delegates as they are attached.
     * @param delegates
     *            Optional set of delegates to support each of the bean
     *            interfaces.
     * @param values
     *            Necessary to set onto each delegate so it can safely
     *            manipulate the internal state of the JavaBeanHandler.
     */
    DelegateMapping(Set<BeanInfo> proxiedBeanInfo,
            DelegationTarget[] delegates, PropertyValues values)
    {
        this.values = values;

        Set<Method> tempMethods = new HashSet<Method>();

        for (BeanInfo beanInfo : proxiedBeanInfo)
        {
            MethodDescriptor[] methodDescriptors = beanInfo
                    .getMethodDescriptors();

            // don't bother with interfaces that only specify properties and
            // events
            if (methodDescriptors.length == 0)
            {
                continue;
            }

            for (MethodDescriptor methodDescriptor : methodDescriptors)
            {
                tempMethods.add(methodDescriptor.getMethod());
            }

            // initialize to null since this is just setting up the fixed key
            // set
            delegations.put(beanInfo.getBeanDescriptor().getBeanClass(), null);
        }

        methods = Collections.unmodifiableSet(tempMethods);

        if (delegates == null)
        {
            return;
        }

        for (DelegationTarget delegate : delegates)
        {
            attach(delegate);
        }
    }

    final void attach(DelegationTarget delegate)
    {
        MethodValidator.validate(delegate);

        Class<?> delegatingInterface = delegate.getDelegatingInterface();

        if (!delegations.containsKey(delegate.getDelegatingInterface()))
        {
            throw new IllegalArgumentException(String.format(
                    "The proxy does not implement the interface, %1$s.",
                    delegatingInterface.getName()));
        }

        if (delegations.get(delegatingInterface) != null)
        {
            LOGGER
                    .warn(String
                            .format(
                                    "DelegationTarget already mapped for interface, %1$s, overwriting!",
                                    delegatingInterface));
        }

        delegate.setPropertyValues(values);
        delegations.put(delegatingInterface, delegate);
    }

    boolean isAttached(Class<?> interfaceType)
    {
        if (!delegations.containsKey(interfaceType))
        {
            throw new InvalidDelegateException(
                    String
                            .format(
                                    "The interface, %1$s, is not one supported by this object!",
                                    interfaceType.getName()));
        }

        return delegations.get(interfaceType) != null;
    }
}
