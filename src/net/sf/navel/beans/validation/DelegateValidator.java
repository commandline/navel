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
package net.sf.navel.beans.validation;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.navel.beans.MethodHandler;
import net.sf.navel.beans.DelegationTarget;
import net.sf.navel.beans.InvalidPropertyValueException;
import net.sf.navel.beans.PropertyValidator;
import net.sf.navel.beans.UnsupportedFeatureException;

import org.apache.log4j.Logger;

/**
 * Provides validation on construction support for the DelegateBeanHandler,
 * ensuring that all the rules that must be true for the handler to work are.
 * 
 * @author cmndln
 * @version $Revision: 1.5 $, $Date: 2005/09/15 22:08:06 $
 */
public class DelegateValidator implements Validator, Serializable
{

    private static final long serialVersionUID = 5652414840166443606L;

    private static final Logger LOGGER = Logger
            .getLogger(DelegateValidator.class);

    private final PropertyValidator decorated;

    private final Map<Class, DelegationTarget> delegations;

    private final DelegationTarget[] delegates;

    private final MethodHandler<?> handler;

    private final boolean checkOnInit;

    public DelegateValidator(MethodHandler<?> handler,
            PropertyValidator decorated, boolean checkOnInit)
    {
        this.decorated = decorated;
        this.handler = handler;
        this.checkOnInit = checkOnInit;
        delegates = handler.getDelegates();
        delegations = handler.getDelegations();
    }

    /**
     * This method checks that there are delegates for the purely functional
     * interfaces of the class and that anything left over describes properties
     * only.
     * 
     * @param beanInfo
     *            Introspection data about the proxied class.
     * @throws UnsupportedFeatureException
     *             Thrown if the interfaces that do not have delegates do not
     *             describe pure property beans.
     */
    public final void validateMethods(BeanInfo beanInfo)
            throws UnsupportedFeatureException
    {
        Class[] interfaces = decorated.proxiedClass.getInterfaces();

        if ((null == interfaces) || (0 == interfaces.length))
        {
            throw new UnsupportedFeatureException(
                    "Proxied class must implement at least one interface, either describing proxies or to be delegated.");
        }

        // builing a hashed set makes containment checking easy
        Set<Class> delegatedInterfaces = new HashSet<Class>(interfaces.length);

        for (int i = 0; i < interfaces.length; i++)
        {
            delegatedInterfaces.add(interfaces[i]);
        }

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Delegated interfaces:");
            LOGGER.debug(delegatedInterfaces);
        }

        // map the delegates we have to implemented interfaces
        for (int i = 0; i < delegates.length; i++)
        {
            mapDelegate(delegates[i].getClass(), delegates[i],
                    delegatedInterfaces);
        }

        // make sure any left over interfaces are property beans or have no
        // methods, e.g. Serializable
        Iterator propertyBeans = delegatedInterfaces.iterator();

        while (propertyBeans.hasNext())
        {
            Class propertyBean = (Class) propertyBeans.next();

            validateProperties(propertyBean, checkOnInit);
        }
    }

    public void validateAll() throws UnsupportedFeatureException,
            InvalidPropertyValueException
    {
        try
        {
            BeanInfo beanInfo = Introspector
                    .getBeanInfo(decorated.proxiedClass);

            validateEvents(beanInfo);
            validateMethods(beanInfo);
            validateData(beanInfo);
        }
        catch (IntrospectionException e)
        {
            throw new UnsupportedFeatureException(
                    "Unable to introspect proxied class.");
        }
    }

    public void validateEvents(BeanInfo beanInfo)
            throws UnsupportedFeatureException
    {
        decorated.forbidEvents(beanInfo);
    }

    public void validateData(BeanInfo beanInfo)
            throws InvalidPropertyValueException
    {
        decorated.validateData(beanInfo);
    }

    public void revalidateData() throws InvalidPropertyValueException
    {
        decorated.revalidateData();
    }

    @SuppressWarnings("unchecked")
    private void mapDelegate(Class delegateClass, DelegationTarget delegate,
            Set<Class> delegatedInterfaces)
    {
        if (delegateClass.isInterface())
        {
            if (delegatedInterfaces.contains(delegateClass))
            {
                if (LOGGER.isTraceEnabled())
                {
                    LOGGER.trace("Mapping " + delegate.getClass().getName()
                            + " to " + delegateClass.getName() + ".");
                }

                delegatedInterfaces.remove(delegateClass);
                delegations.put(delegateClass, delegate);
                delegate.setDelegationSource(handler);
            }
            else
            {
                if (LOGGER.isTraceEnabled())
                {
                    LOGGER.trace("Found interface, " + delegateClass.getName()
                            + ", but no delegation target.");
                }
            }
        }
        else
        {
            Class[] implementedClasses = findAllInterfaces(delegateClass);

            if (LOGGER.isTraceEnabled())
            {
                LOGGER.trace("Recursing on " + delegateClass.getName() + ".");
                LOGGER.trace("Found " + implementedClasses.length
                        + " implemented interfaces.");
            }

            for (int i = 0; i < implementedClasses.length; i++)
            {
                mapDelegate(implementedClasses[i], delegate,
                        delegatedInterfaces);
            }
        }
    }

    private Class[] findAllInterfaces(Class delegateClass)
    {
        List<Class> interfaces = new ArrayList<Class>();

        interfaces.addAll(Arrays.asList(delegateClass.getInterfaces()));

        for (Class parent = delegateClass.getSuperclass(); !parent
                .equals(Object.class); parent = parent.getSuperclass())
        {
            interfaces.addAll(Arrays.asList(parent.getInterfaces()));
        }

        return interfaces.toArray(new Class[interfaces.size()]);
    }

    private void validateProperties(Class implemented, boolean exceptionOnFail)
            throws UnsupportedFeatureException
    {
        if (Serializable.class.equals(implemented))
        {
            return;
        }

        BeanInfo beanInfo = null;

        try
        {
            beanInfo = Introspector.getBeanInfo(implemented);
        }
        catch (IntrospectionException e)
        {
            throw new UnsupportedFeatureException(
                    "Error introspecting non-delegated interface");
        }

        validateEvents(beanInfo);

        try
        {
            decorated.validateMethods(beanInfo);
        }
        catch (UnsupportedFeatureException e)
        {
            if (exceptionOnFail)
            {
                throw new UnsupportedFeatureException("Implemented interface, "
                        + implemented.getName()
                        + ", needs to be a simple bean description "
                        + "or have an appropriate delegate.");
            }
            else
            {
                if (LOGGER.isDebugEnabled())
                {
                    LOGGER.debug("Implemented interface, "
                            + implemented.getName()
                            + ", needs to be a simple bean description "
                            + "or have an appropriate delegate.");
                }
            }
        }
    }
}