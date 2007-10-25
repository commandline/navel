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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

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

    private static final Logger LOGGER = LogManager
            .getLogger(ProxyFactory.class);

    static final ProxyFactory SINGLETON = new ProxyFactory();

    private static final int MAX_NESTING_DEPTH = 10;

    private final Map<Class<?>, ConstructionDelegate> constructionDelegates = new HashMap<Class<?>, ConstructionDelegate>();

    private static ThreadLocal<Integer> nestingDepth = new ThreadLocal<Integer>();

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

        SINGLETON.constructionDelegates.put(forType, delegate);
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
        if (!SINGLETON.constructionDelegates.containsKey(forType))
        {
            return null;
        }

        return SINGLETON.constructionDelegates.remove(forType);
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
     * Overload that does not set any initial values.
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
        Class<?>[] allTypes = new Class<?>[additionalTypes.length + 1];

        allTypes[0] = primaryType;
        System.arraycopy(additionalTypes, 0, allTypes, 1,
                additionalTypes.length);

        return (B) ProxyFactory.create(initialValues, allTypes,
                new InterfaceDelegate[0]);
    }

    /**
     * Fully specified factory method for generating a new Navel backed dynamic
     * proxy.
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
        return create(null, initialValues, allTypes, initialDelegates);
    }

    /**
     * An overload that assumes shallow copy.
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
     * then casts and returns as T.
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

        JavaBeanHandler sourceHandler = getHandler(source);

        if (null == sourceHandler)
        {
            throw new UnsupportedFeatureException(
                    "Cannot copy anything other than a Navel bean!");
        }

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
     * the internal bean state.
     * 
     * @param source
     *            Bean to copy, must be a Navel bean.
     * @param deep
     *            Perform a deep copy.
     * @return A copy of the original bean.
     */
    public static Object copy(Object source, boolean deep)
    {
        return SINGLETON.copyObject(source, deep, false);
    }

    /**
     * Generic version that helps ensure type safety.
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

        JavaBeanHandler sourceHandler = getHandler(source);

        if (null == sourceHandler)
        {
            throw new UnsupportedFeatureException(
                    "Cannot copy anything other than a Navel bean!");
        }

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
     * Performs a deep copy of all the proxy support code, a deep copy of the
     * internal bean state, and marks the internal bean state as immutable which
     * only protects the internal PropertyValues. Any attempts to change the
     * value portion of the return object will throw an unchecked exception. All
     * kinds of delegates may be attached and detached as this affects the
     * behavior of the instance but not its state as such.
     * 
     * @param source
     *            Bean to copy, must be a Navel bean.
     * @return An immutable copy of the original bean.
     */
    public static Object unmodifiableObject(Object source)
    {
        return SINGLETON.copyObject(source, true, true);
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
        JavaBeanHandler handler = getHandler(bean);

        if (null == handler)
        {
            throw new UnsupportedFeatureException(
                    "Cannot check a delegate on anything other than a Navel bean!");
        }

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
        JavaBeanHandler handler = getHandler(bean);

        if (null == handler)
        {
            throw new UnsupportedFeatureException(
                    "Cannot attach a delegate to anything other than a Navel bean!");
        }

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
        JavaBeanHandler handler = getHandler(bean);

        if (null == handler)
        {
            throw new UnsupportedFeatureException(
                    "Cannot attach a delegate to anything other than a Navel bean!");
        }

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
        JavaBeanHandler handler = getHandler(bean);

        if (null == handler)
        {
            throw new UnsupportedFeatureException(
                    "Cannot detach a delegate from anything other than a Navel bean!");
        }

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
        JavaBeanHandler handler = getHandler(bean);

        if (null == handler)
        {
            throw new UnsupportedFeatureException(
                    "Cannot detach a delegate from anything other than a Navel bean!");
        }

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

        JavaBeanHandler handler = ProxyFactory.getHandler(bean);

        if (null == handler)
        {
            throw new UnsupportedFeatureException(
                    "The bean argument must be a Navel bean, use the BeanManipulator to apply a Map to a plain, old JavaBean.");
        }

        return handler.propertyValues.getProxyDescriptor();
    }

    /**
     * Package private overload required to satisfy copy logic, allowing the
     * copy code in JavaBeanHandler to provide its own handler copy.
     * 
     * @param handler
     *            If null, should trigger creation of a new handler; otherwise
     *            use the one provided.
     * @param initialValues
     *            Initial property values the proxy will have, checked to see if
     *            they are valid. May only be null if the handler argument is
     *            not null.
     * @param allTypes
     *            All of the interfaces the proxy will implement.
     * @param initialDelegates
     *            Delegates to map in initially.
     * @return A proxy that extends all of the specified types and has the
     *         specified initial property values.
     */
    static Object create(JavaBeanHandler handler,
            Map<String, Object> initialValues, Class<?>[] allTypes,
            InterfaceDelegate[] initialDelegates)
    {
        if (allTypes.length <= 0)
        {
            throw new IllegalArgumentException(
                    "Must supply at least interface for the proxy to implement!");
        }

        incrementNesting();

        // in some environments, such as Ant, trying harder is required
        try
        {
            return SINGLETON.instantiate(Thread.currentThread()
                    .getContextClassLoader(), handler, allTypes, initialValues,
                    initialDelegates);
        }
        catch (IllegalArgumentException e)
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug(
                        "Failed to instantiate using thread context's loader.",
                        e);
                LOGGER.debug(String.format(
                        "Trying the loader for class, %1$s.", allTypes[0]
                                .getName()));
            }

            try
            {
                return SINGLETON.instantiate(allTypes[0].getClassLoader(),
                        handler, allTypes, initialValues, initialDelegates);
            }
            catch (IllegalArgumentException again)
            {
                if (LOGGER.isDebugEnabled())
                {
                    LOGGER
                            .debug(String
                                    .format(
                                            "Failed to instantiate using loader for class, %1$s.",
                                            allTypes[0].getName()));
                    LOGGER.debug("Trying the system's loader.");
                }

                return SINGLETON.instantiate(
                        ClassLoader.getSystemClassLoader(), handler, allTypes,
                        initialValues, initialDelegates);
            }
        }
        finally
        {
            decrementNesting();
        }
    }

    private static void incrementNesting()
    {
        if (null == nestingDepth.get())
        {
            nestingDepth.set(0);
        }
        else
        {
            nestingDepth.set(nestingDepth.get() + 1);
        }

        if (nestingDepth.get() > MAX_NESTING_DEPTH)
        {
            throw new IllegalStateException(
                    String
                            .format(
                                    "Exceeded maximum nesting depth, %1$d, for delegate construction using registered ConstructorDelegat instnance.  Make sure to check the nestingDepth argument to ConstructorDelegate's methods.",
                                    MAX_NESTING_DEPTH));
        }
    }

    private static void decrementNesting()
    {
        if (nestingDepth.get() == 0)
        {
            nestingDepth.remove();
        }
        else
        {
            nestingDepth.set(nestingDepth.get() - 1);
        }
    }

    /*
     * This methid keeps the custom initialization hook logic close to the
     * actual point of instantiation of the dynamic Proxy.
     */
    private Object instantiate(ClassLoader loader, JavaBeanHandler handler,
            Class<?>[] allTypes, Map<String, Object> initialValues,
            InterfaceDelegate[] initialDelegates)
    {
        // only perform the pre-init for a new instance, NOT for a copy
        Class<?>[] amendedTypes = null == handler ? doBeforeInit(initialValues,
                allTypes) : allTypes;

        JavaBeanHandler newHandler = null == handler ? new JavaBeanHandler(
                initialValues, amendedTypes, initialDelegates) : handler;

        Object bean = Proxy.newProxyInstance(loader, amendedTypes, newHandler);

        // perform the post-init for new and copy both
        doAfterInit(bean, amendedTypes);

        return bean;
    }

    private Object copyObject(Object source, boolean deep,
            boolean immutableValues)
    {
        if (null == source)
        {
            return null;
        }

        JavaBeanHandler sourceHandler = getHandler(source);

        if (null == sourceHandler)
        {
            throw new UnsupportedFeatureException(
                    "Cannot copy anything other than a Navel bean!");
        }

        Object copy = sourceHandler.copy(deep, immutableValues);

        return copy;
    }

    @SuppressWarnings("unchecked")
    private Class<?>[] doBeforeInit(Map<String, Object> initialValues,
            Class<?>[] allTypes)
    {
        Map<String, Object> initialFixedCopy = null == initialValues ? Collections.EMPTY_MAP
                : initialValues;
        initialFixedCopy = Collections.unmodifiableMap(initialFixedCopy);

        List<Class<?>> combinedTypes = new LinkedList<Class<?>>(Arrays
                .asList(allTypes));

        // using a separate set makes the containment check cheaper
        Set<Class<?>> uniqueTypes = new HashSet<Class<?>>(combinedTypes);

        for (Class<?> singleType : allTypes)
        {
            if (!constructionDelegates.containsKey(singleType))
            {
                continue;
            }

            ConstructionDelegate delegate = constructionDelegates
                    .get(singleType);

            Collection<Class<?>> additionalTypes = delegate.additionalTypes(
                    nestingDepth.get(), allTypes[0], singleType, allTypes,
                    initialFixedCopy);

            // null is acceptable to indicate a no-op
            if (null == additionalTypes)
            {
                continue;
            }

            // the size of the additional type collection is expected to be
            // small, so not presently concerned about nesting loops
            for (Class<?> additionalType : additionalTypes)
            {
                if (null == additionalType)
                {
                    throw new IllegalStateException(
                            "Do not include any null types in the return Collection for ConstructorDelegate.additionalTypes().");
                }

                if (!additionalType.isInterface())
                {
                    throw new IllegalStateException(
                            String
                                    .format(
                                            "The type, %1$s, is not an interface.  The additional types returned by ConstructorDelegate.additionalTypes() must all be interfaces.",
                                            additionalType.getName()));
                }

                if (uniqueTypes.contains(additionalType))
                {
                    continue;
                }

                // add the new interfaces to the end so as not to inadvertently
                // clobber
                // the special zeroth element, the primary type
                combinedTypes.add(additionalType);

                // guard against duplication of the newly added
                uniqueTypes.add(additionalType);
            }
        }

        return combinedTypes.toArray(new Class<?>[combinedTypes.size()]);
    }

    private void doAfterInit(Object bean, Class<?>[] allTypes)
    {
        for (Class<?> singleType : allTypes)
        {
            if (!constructionDelegates.containsKey(singleType))
            {
                continue;
            }

            ConstructionDelegate delegate = constructionDelegates
                    .get(singleType);

            delegate.init(nestingDepth.get(), singleType, bean);
        }
    }
}
