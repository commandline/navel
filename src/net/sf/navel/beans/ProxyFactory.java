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
import java.util.Map;

/**
 * This is the starting point for working with Navel. It encapsulates the
 * creation of dynamic proxies, constructing them with the supplied delegation
 * pieces and the common data backing code.
 * 
 * @author thomas
 * 
 */
public class ProxyFactory
{

    private ProxyFactory()
    {
        // enforce Singleton pattern
    }

    /**
     * Register custom construction logic, to give Navel beans the ability to
     * behave more closely to concrete Java Beans that can have whatever
     * constructions scheme is required.
     * 
     * @param forType
     *            Interface for which the custom logic will be registered.
     * @param delegate
     *            Implementation that provides the custom logic.
     */
    public static void register(Class<?> forType, ConstructionDelegate delegate)
    {
        if (null == forType)
        {
            throw new IllegalArgumentException(
                    "Cannot register a ConstructorDelegate for a null type.");
        }

        if (null == delegate)
        {
            throw new IllegalArgumentException(String.format(
                    "Cannot register a null delegate for type, $1%s.", forType
                            .getName()));
        }

        ProxyCreator.register(forType, delegate);
    }

    /**
     * Register a single constructor to be called on all proxies,
     * unconditionally. The {@link ConstructionDelegate} interface still
     * provides enough information in the formal arguments that an arbitrary
     * implementation can do conditional work on its own.
     * 
     * @param delegate
     *            Delegate to be called for every proxy, in case an application
     *            has rules to be executed everywhere.
     */
    public static void registerDefault(ConstructionDelegate delegate)
    {
        if (null == delegate)
        {
            throw new IllegalArgumentException(
                    "Cannot register a null delegate for the default constructor.");
        }

        ProxyCreator.registerDefault(delegate);
    }

    /**
     * Register an alternate strategy for handling the mass setting of values on
     * any beans instantiated during the course of evaluating dot notations.
     * 
     * @param resolver
     *            Implementation that should handle the derived maps for a new
     *            or existing bean to pass property validation.
     */
    public static void registerResolver(NestedResolver resolver)
    {
        if (null == resolver)
        {
            throw new IllegalArgumentException(
                    "Cannot register a null NestedResolver.");
        }

        PropertyValueResolver.register(resolver);
    }

    /**
     * Useful for testing but should not be necessary, otherwise.
     * 
     * @param forType
     *            Type for which a delegate, if it exists, will be removed.
     * @return The delegate instance, if there is one, or null.
     */
    public static ConstructionDelegate unregister(Class<?> forType)
    {
        return ProxyCreator.unregister(forType);
    }

    /**
     * Overload that narrows the new bean down to the primary type of interest
     * and set the initial values.
     * 
     * @param <B>
     *            The preferred return type.
     * @param primaryType
     *            Class argument to fulfill the return type generic parameter.
     * @param additionalTypes
     *            Optional, additional types this object will implement.
     * @return An instance of the primary type that also extends all of the
     *         optionalTypes.
     */
    @SuppressWarnings("unchecked")
    public static <B> B createAs(Class<B> primaryType,
            Class<?>... additionalTypes)
    {
        return ProxyFactory.createAs(primaryType, null, additionalTypes);
    }

    /**
     * Overload that narrows the new bean down to the primary type of interest
     * and does not set any initial values.
     * 
     * @param <B>
     *            The preferred return type.
     * @param primaryType
     *            Class argument to fulfill the return type generic parameter.
     * @param initialValues
     *            Initial property values the proxy will have, checked to see if
     *            they are valid.
     * @param additionalTypes
     *            Optional, additional types this object will implement.
     * @return An instance of the primary type that also extends all of the
     *         optionalTypes.
     */
    @SuppressWarnings("unchecked")
    public static <B> B createAs(Class<B> primaryType,
            Map<String, Object> initialValues, Class<?>... additionalTypes)
    {
        return ProxyFactory.createAs(primaryType, null, initialValues,
                additionalTypes);
    }

    /**
     * Overload that narrows the new bean down to the primary type of interest
     * and does not set any initial values.
     * 
     * @param <B>
     *            The preferred return type.
     * @param primaryType
     *            Class argument to fulfill the return type generic parameter.
     * @param constructorArguments
     *            Not required, may be null; just passed to constructor
     *            delegates to provide an optional set of state for construction
     *            but not to be directly added to internal storage. If not
     *            supplied, the initial values argument will be passed to any
     *            {@link ConstructionDelegate} instances registered for the
     *            proxy's types, instead.
     * @param initialValues
     *            Initial property values the proxy will have, checked to see if
     *            they are valid.
     * @param additionalTypes
     *            Optional, additional types this object will implement.
     * @return An instance of the primary type that also extends all of the
     *         optionalTypes.
     */
    @SuppressWarnings("unchecked")
    public static <B> B createAs(Class<B> primaryType,
            Map<String, Object> constructorArguments,
            Map<String, Object> initialValues, Class<?>... additionalTypes)
    {
        Class<?>[] allTypes = new Class<?>[additionalTypes.length + 1];

        allTypes[0] = primaryType;
        System.arraycopy(additionalTypes, 0, allTypes, 1,
                additionalTypes.length);

        return (B) ProxyFactory.create(constructorArguments, initialValues,
                allTypes, new InterfaceDelegate[0]);
    }

    /**
     * Overload that does not set any initial values or constructor arguments.
     * 
     * @param allTypes
     *            All of the interfaces the proxy will implement.
     * @return A proxy that extends all of the specified types and has no
     *         initial property values.
     */
    public static Object create(Class<?>... allTypes)
    {
        return ProxyFactory.create(null, allTypes, new InterfaceDelegate[0]);
    }

    /**
     * Overload that uses the initial values to set the beans initial state and
     * the same values to pass in as constructor arguments for any registered
     * instances of {@link ConstructionDelegate}.
     * 
     * @param initialValues
     *            Initial property values the proxy will have, checked to see if
     *            they are valid.
     * @param allTypes
     *            All of the interfaces the proxy will implement.
     * @param initialDelegates
     *            Delegates to map in initially.
     * @return A proxy that extends all of the specified types and has the
     *         specified initial property values.
     */
    public static Object create(Map<String, Object> initialValues,
            Class<?>[] allTypes, InterfaceDelegate[] initialDelegates)
    {
        return ProxyCreator.create(null, null, initialValues, allTypes,
                initialDelegates);
    }

    /**
     * Fully specified factory method for generating a new Navel backed dynamic
     * proxy.
     * 
     * @param constructorArguments
     *            Not required, may be null; just passed to constructor
     *            delegates to provide an optional set of state for construction
     *            but not to be directly added to internal storage. If not
     *            supplied, the initial values argument will be passed to any
     *            {@link ConstructionDelegate} instances registered for the
     *            proxy's types, instead.
     * @param initialValues
     *            Initial property values the proxy will have, checked to see if
     *            they are valid.
     * @param allTypes
     *            All of the interfaces the proxy will implement.
     * @param initialDelegates
     *            Delegates to map in initially.
     * @return A proxy that extends all of the specified types and has the
     *         specified initial property values.
     */
    public static Object create(Map<String, Object> constructorArguments,
            Map<String, Object> initialValues, Class<?>[] allTypes,
            InterfaceDelegate[] initialDelegates)
    {
        return ProxyCreator.create(null, constructorArguments, initialValues,
                allTypes, initialDelegates);
    }

    /**
     * Overload that assumes shallow copy.
     * 
     * @param <T>
     *            Desired interface, must be one the source proxy supports.
     * @param primaryType
     *            For pegging the generic parameter.
     * @param source
     *            Source to copy, performs a shallow copy.
     * @param subTypes
     *            Subset of types out of all supported by the original.
     * @return A copy of the original whose data is the subset supported by the
     *         new primary type and subTypes.
     */
    public static <T> T viewAs(Class<T> primaryType, Object source,
            Class<?>... subTypes)
    {
        return ProxyFactory.viewAs(primaryType, false, subTypes);
    }

    /**
     * Generates a view, as a copy, of the source that is a safe subset of the
     * properties on the original.
     * 
     * @param <T>
     *            Desired interface, must be one the source proxy supports.
     * @param primaryType
     *            For pegging the generic parameter.
     * @param source
     *            Source to copy.
     * @param deepCopy
     *            Perform a deep copy if true, otherwise a shallow copy
     * @param subTypes
     *            Subset of types out of all supported by the original.
     * @return A copy of the original whose data is the subset supported by the
     *         new primary type and subTypes.
     */
    @SuppressWarnings("unchecked")
    public static <T> T viewAs(Class<T> primaryType, Object source,
            boolean deepCopy, Class<?>... subTypes)
    {
        Class<?>[] allTypes = new Class<?>[subTypes.length + 1];

        allTypes[0] = primaryType;
        System.arraycopy(subTypes, 0, allTypes, 1, subTypes.length);

        return (T) ProxyCopier.subset(source, deepCopy, subTypes);
    }

    /**
     * An overload that assumes shallow copy. Also performs a shallow copy of
     * any property delegates, to keep synthetic properties consistent with the
     * source. This means the new bean will share the exact same
     * {@link PropertyDelegate} instances as the source, but those delegates
     * should be stateless so should not be a problem.
     * 
     * @param <T>
     *            Desired interface, must be one the source proxy supports.
     * @param primaryType
     *            For pegging the generic parameter.
     * @param source
     *            Source to copy, performs a shallow copy.
     * @return A copy, safely type as T.
     */
    public static <T> T copyAs(Class<T> primaryType, Object source)
    {
        return copyAs(primaryType, source, false);
    }

    /**
     * A convenience version that also checks to see if the cast to T is safe,
     * then casts and returns as T. Also performs a shallow copy of any property
     * delegates, to keep synthetic properties consistent with the source. This
     * means the new bean will share the exact same {@link PropertyDelegate}
     * instances as the source, but those delegates should be stateless so
     * should not be a problem.
     * 
     * @param <T>
     *            Desired interface, must be one the source proxy supports.
     * @param primaryType
     *            For pegging the generic parameter.
     * @param source
     *            Source to copy, performs a shallow copy.
     * @param deep
     *            Perform a deep copy.
     * @return A copy, safely type as T.
     */
    @SuppressWarnings("unchecked")
    public static <T> T copyAs(Class<T> primaryType, Object source, boolean deep)
    {
        if (null == source)
        {
            return null;
        }

        JavaBeanHandler sourceHandler = ProxyFactory.getRequiredHandler(source,
                "Cannot copy anything other than a Navel bean!");

        if (!sourceHandler.proxiesFor(primaryType))
        {
            throw new IllegalArgumentException(
                    String
                            .format(
                                    "The copy source, %1$s, does not support the requested type, %2$s.",
                                    sourceHandler, primaryType.getName()));
        }

        return (T) copy(source, deep);
    }

    /**
     * Performs a deep copy of all the proxy support code and a shallow copy of
     * the internal bean state. Also performs a shallow copy of any property
     * delegates, to keep synthetic properties consistent with the source. This
     * means the new bean will share the exact same {@link PropertyDelegate}
     * instances as the source, but those delegates should be stateless so
     * should not be a problem.
     * 
     * @param source
     *            Bean to copy, must be a Navel bean.
     * @param deep
     *            Perform a deep copy.
     * @return A copy of the original bean.
     */
    public static Object copy(Object source, boolean deep)
    {
        return ProxyCopier.copy(source, deep, false);
    }

    /**
     * Generic version that helps ensure type safety; the same adviso applie as
     * the non-generic method.
     * 
     * @see #unmodifiableObject(Object)
     * @param source
     *            Bean to copy, must be a Navel bean.
     * @return An immutable copy of the original bean.
     */
    @SuppressWarnings("unchecked")
    public static <T> T unmodifiableObjectAs(Class<T> primaryType, Object source)
    {
        if (null == source)
        {
            return null;
        }

        JavaBeanHandler sourceHandler = getRequiredHandler(source,
                "Cannot copy anything other than a Navel bean!");

        if (!sourceHandler.proxiesFor(primaryType))
        {
            throw new IllegalArgumentException(
                    String
                            .format(
                                    "The copy source, %1$s, does not support the requested type, %2$s.",
                                    sourceHandler, primaryType.getName()));
        }

        return (T) unmodifiableObject(source);
    }

    /**
     * Creates a new instance, that cannot be changed, which is a copy of all
     * the proxy support code, a deep copy of the internal bean state, and a
     * shallow copy of any property delegates, to keep synthetic properties
     * consistent with the source. This means the new bean will share the exact
     * same {@link PropertyDelegate} instances as the source, but those
     * delegates should be stateless so should not be a problem. The immutable
     * quality protects the internal {@link PropertyValues} which includes its
     * collection of {@link PropertyDelegate} instnaces. Any attempts to change
     * the value portion of the return object will throw an unchecked exception.
     * Any attempt to alter the {@link PropertyDelegate} mapping will also cause
     * an unchecked exception to be thrown as doing so would introduce an
     * inconsistent view of the internal state.
     * 
     * {@link InterfaceDelegate} instances may be attached and detached as this
     * affects the behavior of the instance but not its state as such.
     * 
     * @param source
     *            Bean to copy, must be a Navel bean.
     * @return An immutable copy of the original bean.
     */
    public static Object unmodifiableObject(Object source)
    {
        return ProxyCopier.copy(source, true, true);
    }

    /**
     * Checks to see if there is a delegate for the specified interface.
     * 
     * @param bean
     *            Proxy to check.
     * @param delegatingInterface
     *            Interface to check.
     * @return Whether there is a delegate for the specified interface.
     */
    public static boolean isAttached(Object bean, Class<?> delegatingInterface)
    {
        JavaBeanHandler handler = getHandler(bean);

        if (null == handler)
        {
            throw new UnsupportedFeatureException(
                    "Cannot check a delegate on anything other than a Navel bean!");
        }

        return handler.delegateMapping.isAttached(delegatingInterface);
    }

    /**
     * Checks to see if there is a delegate for the specified property.
     * 
     * @param bean
     *            Proxy to check.
     * @param propertyName
     *            Property to check.
     * @return Whether there is a delegate for the specified property.
     */
    public static boolean isAttached(Object bean, String propertyName)
    {
        JavaBeanHandler handler = getRequiredHandler(bean,
                "Cannot check a delegate on anything other than a Navel bean!");

        return handler.propertyValues.isAttached(propertyName);
    }

    /**
     * Utility method that exposes the runtime delegation attachment code.
     * 
     * @param bean
     *            Target to which the delegate will be attached, if applicable.
     * @param delegate
     *            Delegate to attach.
     */
    public static void attach(Object bean, InterfaceDelegate delegate)
    {
        JavaBeanHandler handler = getRequiredHandler(bean,
                "Cannot attach a delegate to anything other than a Navel bean!");

        handler.delegateMapping.attach(delegate);
    }

    /**
     * Convenience method for attaching multiple delegates at once.
     * 
     * @param bean
     *            Existing bean instance to which new delegates will be
     *            attached.
     * @param delegates
     *            New delegates to attach.
     */
    public static void attach(Object bean, InterfaceDelegate... delegates)
    {
        for (InterfaceDelegate delegate : delegates)
        {
            attach(bean, delegate);
        }
    }

    /**
     * Utility method that exposes the runtime delegation attachment code.
     * 
     * @param bean
     *            Target to which the delegate will be attached, if applicable.
     * @param propertyName
     *            Property this delegate should support.
     * @param delegate
     *            Delegate to attach.
     */
    public static void attach(Object bean, String propertyName,
            PropertyDelegate<?> delegate)
    {
        JavaBeanHandler handler = getRequiredHandler(bean,
                "Cannot attach a delegate to anything other than a Navel bean!");

        handler.propertyValues.attach(propertyName, delegate);
    }

    /**
     * Removes a delegate, if any, mapped to the specific interface. Useful, for
     * instance, to participate in a State or Strategy pattern where at
     * different times or under different conditions different delegates, or
     * none at all, might be desirable.
     * 
     * @param bean
     *            Bean from which the delegate should be detached.
     * @param delegatingInterface
     *            If there is a delegate for this interface, remove it.
     * @return Indicate whether a delegate was removed.
     */
    public static boolean detach(Object bean, Class<?> delegatingInterface)
    {
        JavaBeanHandler handler = getRequiredHandler(bean,
                "Cannot detach a delegate from anything other than a Navel bean!");

        return handler.delegateMapping.detach(delegatingInterface);
    }

    /**
     * Removes a delegate, if any, mapped to the specific property. Useful, for
     * instance, to participate in a State or Strategy pattern where at
     * different times or under different conditions different delegates, or
     * none at all, might be desirable.
     * 
     * @param bean
     *            Bean from which the delegate should be detached.
     * @param propertyName
     *            If there is a delegate for this property, remove it.
     * @return Indicate whether a delegate was removed.
     */
    public static boolean detach(Object bean, String propertyName)
    {
        JavaBeanHandler handler = getRequiredHandler(bean,
                "Cannot detach a delegate from anything other than a Navel bean!");

        return handler.propertyValues.detach(propertyName);
    }

    /**
     * A convenience method to get the underlying Navel bean handler, if the
     * passed in object is a Navel bean. The bean argument is tested and if any
     * of the tests to figure out if it is a Navel bean fail, null is returned.
     * 
     * @param bean
     *            Object to test for being a Navel bean.
     * @return Null if the bean argument is not a Navel bean, otherwise, the
     *         Navel handler for the bean proxy.
     */
    public static JavaBeanHandler getHandler(Object bean)
    {
        if (null == bean || !Proxy.isProxyClass(bean.getClass()))
        {
            return null;
        }

        Object handler = Proxy.getInvocationHandler(bean);

        if (!(handler instanceof JavaBeanHandler))
        {
            return null;
        }

        JavaBeanHandler beanHandler = (JavaBeanHandler) handler;

        return beanHandler;
    }

    /**
     * Allows retrieval of the reflection information provided at construction
     * time, including the primary interface out of all the supported
     * interfaces.
     * 
     * @param bean
     *            Must be a navel bean.
     * @return The reflection descriptor for the proxy.
     */
    public static ProxyDescriptor getProxyDescriptor(Object bean)
    {
        if (null == bean)
        {
            throw new IllegalArgumentException("Bean argument cannot be null!");
        }

        JavaBeanHandler handler = ProxyFactory
                .getRequiredHandler(
                        bean,
                        "The bean argument must be a Navel bean, use the BeanManipulator to apply a Map to a plain, old JavaBean.");

        return handler.propertyValues.getProxyDescriptor();
    }

    static JavaBeanHandler getRequiredHandler(Object bean, String messageOnFail)
    {

        JavaBeanHandler handler = ProxyFactory.getHandler(bean);

        if (null == handler)
        {
            throw new UnsupportedFeatureException(messageOnFail);
        }

        return handler;
    }
}
