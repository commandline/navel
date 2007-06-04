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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import net.sf.navel.beans.validation.DelegateValidator;
import net.sf.navel.beans.validation.NestedValidator;
import net.sf.navel.beans.validation.PropertyValidator;

import org.apache.log4j.Logger;

/**
 * This handler adds the ability to define delegates for methods that do not
 * deal strictly with properties. It leverages PropertyBeanHandler to handle
 * properties, but can delegate other calls through to the provided classes.
 * 
 * @author cmdln
 * @version $Revision: 1.8 $, $Date: 2005/09/19 22:15:41 $
 */
public class DelegateBeanHandler<T> extends PropertyBeanHandler<T>
{

    private static final long serialVersionUID = 3778154657912211158L;

    private static final Logger LOGGER = Logger
            .getLogger(DelegateBeanHandler.class);

    /**
     * The default setting for resolution of nested values in the initial values
     * map.
     */
    public static final boolean DEFAULT_RESOLVE = false;

    /**
     * The default behavior for checking method delegation at initialization and
     * re-validation.
     */
    public static final boolean DEFAULT_INIT_CHECK = true;

    private DelegationTarget[] delegates;

    private final Map<Class, DelegationTarget> delegations = new HashMap<Class, DelegationTarget>();

    private final boolean resolveNested;

    private final boolean checkDelegatesOnInit;

    /**
     * Overload that assumes false for resolution of nested properties.
     * 
     * @param proxiedClass
     *            Class to support.
     * @param delegates
     *            Objects to delegate to when invoking methods other than
     *            property manipulators.
     * @throws UnsupportedFeatureException
     *             Thrown if the proxied class describes any events or any of
     *             it's non property-oriented methods do not have delegates
     *             provided.
     * @throws InvalidPropertyValueException
     *             Thrown if some value in the Map is inappropriate for the
     *             properties of the proxied class.
     */
    public DelegateBeanHandler(Class<T> proxiedClass,
            DelegationTarget... delegates) throws UnsupportedFeatureException,
            InvalidPropertyValueException
    {
        this(proxiedClass, DEFAULT_RESOLVE, DEFAULT_INIT_CHECK, delegates);
    }

    /**
     * Constructor used to indicate a proxied class to support and an array of
     * objects to delegate to where appropraite.
     * 
     * @param proxiedClass
     *            Class to support.
     * @param resolveNested
     *            Should this handler attempt to resolve nested properties, by
     *            name. The parent property shared by nested properties itself
     *            must be a Navel bean.
     * @param checkDelegatesOnInit
     *            Should delegates be checked at init and re-validate, to make
     *            sure all methods have backing? If not, will check at runtime.
     * @param delegates
     *            Objects to delegate to when invoking methods other than
     *            property manipulators.
     * @throws UnsupportedFeatureException
     *             Thrown if the proxied class describes any events or any of
     *             it's non property-oriented methods do not have delegates
     *             provided.
     * @throws InvalidPropertyValueException
     *             Thrown if some value in the Map is inappropriate for the
     *             properties of the proxied class.
     */
    public DelegateBeanHandler(Class<T> proxiedClass, boolean resolveNested,
            boolean checkDelegatesOnInit, DelegationTarget... delegates)
            throws UnsupportedFeatureException, InvalidPropertyValueException
    {
        this(proxiedClass, new HashMap<String, Object>(), resolveNested,
                checkDelegatesOnInit, delegates);
    }

    /**
     * Overload that assumes false for resolution of nested properties.
     * 
     * @param proxiedClass
     *            Class to support.
     * @param values
     *            Initial values for properties described by the proxied class.
     * @param delegates
     *            Objects to delegate to when invoking methods other than
     *            property manipulators.
     * @throws UnsupportedFeatureException
     *             Thrown if the proxied class describes any events or any of
     *             it's non property-oriented methods do not have delegates
     *             provided.
     * @throws InvalidPropertyValueException
     *             Thrown if some value in the Map is inappropriate for the
     *             properties of the proxied class.
     */
    public DelegateBeanHandler(Class<T> proxiedClass,
            Map<String, Object> values, DelegationTarget... delegates)
            throws UnsupportedFeatureException, InvalidPropertyValueException
    {
        this(proxiedClass, values, DEFAULT_RESOLVE, DEFAULT_INIT_CHECK,
                delegates);
    }

    /**
     * Constructor used for the same purpose as the simpler version, with the
     * addition of setting the initial property values to the Map argument.
     * 
     * @param proxiedClass
     *            Class to support.
     * @param values
     *            Initial values for properties described by the proxied class.
     * @param resolveNested
     *            Should this handler attempt to resolve nested properties, by
     *            name. The parent property shared by nested properties itself
     *            must be a Navel bean.
     * @param checkDelegatesOnInit
     *            Should delegates be checked at init and re-validate, to make
     *            sure all methods have backing? If not, will check at runtime.
     * @param delegates
     *            Objects to delegate to when invoking methods other than
     *            property manipulators.
     * @throws UnsupportedFeatureException
     *             Thrown if the proxied class describes any events or any of
     *             it's non property-oriented methods do not have delegates
     *             provided.
     * @throws InvalidPropertyValueException
     *             Thrown if some value in the Map is inappropriate for the
     *             properties of the proxied class.
     */
    public DelegateBeanHandler(Class<T> proxiedClass,
            Map<String, Object> values, boolean resolveNested,
            boolean checkDelegatesOnInit, DelegationTarget... delegates)
            throws UnsupportedFeatureException, InvalidPropertyValueException
    {
        this.proxiedClass = proxiedClass;
        this.delegates = delegates;
        this.proxiedClass = proxiedClass;
        this.resolveNested = resolveNested;
        this.checkDelegatesOnInit = checkDelegatesOnInit;

        // shallow copy so that edits to the original map
        // don't affect the bean contents
        this.values = new HashMap<String, Object>(values);

        if (resolveNested)
        {
            validator = new DelegateValidator(this, new NestedValidator(this),
                    checkDelegatesOnInit);
        }
        else
        {
            validator = new DelegateValidator(this,
                    new PropertyValidator(this), checkDelegatesOnInit);
        }

        validator.validateAll();
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
    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Invoking " + method.getName());
        }

        Class proxiedInterface = method.getDeclaringClass();

        if (delegations.containsKey(proxiedInterface))
        {
            Object delegate = delegations.get(proxiedInterface);

            if (LOGGER.isTraceEnabled())
            {
                LOGGER
                        .trace("Found delegate for "
                                + proxiedInterface.getName());
            }

            return method.invoke(delegate, args);
        }

        String methodName = method.getName();

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Invoking property handling in super class.");
        }

        if (methodName.startsWith(WRITE) || methodName.startsWith(READ)
                || methodName.startsWith(BEING))
        {
            return super.invoke(proxy, method, args);
        }

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Delegating to java.lang.Object.");
        }

        return proxyToObject(
                "Either provide a DelegationTarget at construction time or attach one at runtime.",
                method, args);

    }

    /**
     * Accessor for array of objects supporting delegated interfaces.
     * 
     * @return DelegationTarget array.
     */
    public DelegationTarget[] getDelegates()
    {
        return delegates;
    }

    /**
     * Accessor for map associating interfaces with DelegationTarget instances
     * supporting those interfaces methods.
     * 
     * @return Map keyed by Class objects for declared interfaces on the proxied
     *         class and values from the array of DelegationTarget instances
     *         passed into the constructor.
     */
    public Map<Class, DelegationTarget> getDelegations()
    {
        return delegations;
    }

    /**
     * This method allows for attaching delegates at runtime.
     * 
     * @param toAttach
     *            New delegates to add to the handler.
     */
    public void attachDelegationTarget(DelegationTarget<T>... toAttach)
    {
        if (toAttach.length == 0)
        {
            return;
        }

        DelegationTarget[] combined = new DelegationTarget[delegates.length
                + toAttach.length];

        System.arraycopy(delegates, 0, combined, 0, delegates.length);
        System.arraycopy(toAttach, 0, combined, delegates.length,
                toAttach.length);

        delegates = combined;

        if (resolveNested)
        {
            validator = new DelegateValidator(this, new NestedValidator(this),
                    checkDelegatesOnInit);
        }
        else
        {
            validator = new DelegateValidator(this,
                    new PropertyValidator(this), checkDelegatesOnInit);
        }

        validator.validateAll();
    }
}
