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
import java.util.ArrayList;
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
 * @author cmdln
 * 
 */
class ProxyCreator
{

    private static final Logger LOGGER = LogManager
            .getLogger(ProxyCreator.class);

    private static final ProxyCreator SINGLETON = new ProxyCreator();

    private static final int MAX_NESTING_DEPTH = 10;

    private ConstructionDelegate defaultConstructor = DefaultConstructor.CONSTRUCTOR;

    private static ThreadLocal<Integer> nestingDepth = new ThreadLocal<Integer>();

    private final Map<Class<?>, ConstructionDelegate> constructionDelegates = new HashMap<Class<?>, ConstructionDelegate>();

    private ProxyCreator()
    {
        // enforce Singleton pattern
    }

    static void register(Class<?> forType, ConstructionDelegate delegate)
    {
        SINGLETON.constructionDelegates.put(forType, delegate);
    }

    static void registerDefault(ConstructionDelegate delegate)
    {
        SINGLETON.defaultConstructor = delegate;
    }

    static ConstructionDelegate unregister(Class<?> forType)
    {
        if (!SINGLETON.constructionDelegates.containsKey(forType))
        {
            return null;
        }

        return SINGLETON.constructionDelegates.remove(forType);
    }

    /**
     * Package private overload required to satisfy copy logic, allowing the
     * copy code in JavaBeanHandler to provide its own handler copy.
     * 
     * @param handler
     *            If null, should trigger creation of a new handler; otherwise
     *            use the one provided.
     * @param constructorArguments
     *            Not required, may be null; just passed to constructor
     *            delegates to provide an optional set of state for construction
     *            but not to be directly added to internal storage. If not
     *            supplied, the initial values argument will be passed to any
     *            {@link ConstructionDelegate} instances registered for the
     *            proxy's types, instead.
     * @param initialValues
     *            Initial property values the proxy will have, checked to see if
     *            they are valid. May only be null if the handler argument is
     *            not null.
     * @param allTypes
     *            All of the interfaces the proxy will implement.
     * @return A proxy that extends all of the specified types and has the
     *         specified initial property values.
     */
    static Object create(JavaBeanHandler handler,
            Map<String, Object> constructorArguments,
            Map<String, Object> initialValues, Class<?>[] allTypes)
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
                    .getContextClassLoader(), handler, allTypes,
                    constructorArguments, initialValues);
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
                        handler, allTypes, constructorArguments, initialValues);
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
                        constructorArguments, initialValues);
            }
        }
        finally
        {
            decrementNesting();
        }
    }

    static Class<?>[] combineAdditionalTypes(Class<?>[] allTypes)
    {
        return SINGLETON.doBeforeInit(null, allTypes);
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
            Class<?>[] allTypes, Map<String, Object> constructorArguments,
            Map<String, Object> initialValues)
    {
        Class<?>[] amendedTypes = null;

        // if the caller supplied a handler, then it is a copy; the handler is a
        // result of JavaBeanHandler's copy constructor
        boolean copy = handler != null;

        // only perform the pre-init for a new instance, NOT for a copy
        if (!copy)
        {
            if (null == constructorArguments)
            {
                amendedTypes = doBeforeInit(initialValues, allTypes);
            }
            else
            {
                amendedTypes = doBeforeInit(constructorArguments, allTypes);
            }
        }
        else
        {
            amendedTypes = allTypes;
        }

        JavaBeanHandler newHandler = null == handler ? new JavaBeanHandler(
                initialValues, amendedTypes) : handler;

        Object bean = Proxy.newProxyInstance(loader, amendedTypes, newHandler);

        // allow the post init code to know if this is a copy so it can
        // condition value and behavior handling appropriately
        doAfterInit(copy, bean, amendedTypes);

        return bean;
    }

    @SuppressWarnings("unchecked")
    private Class<?>[] doBeforeInit(Map<String, Object> constructorArguments,
            Class<?>[] allTypes)
    {
        Map<String, Object> fixedArgCopy = null == constructorArguments ? Collections.EMPTY_MAP
                : constructorArguments;
        fixedArgCopy = Collections.unmodifiableMap(fixedArgCopy);

        Class<?> primaryType = allTypes[0];

        List<Class<?>> combinedTypes = new LinkedList<Class<?>>(Arrays
                .asList(allTypes));

        // using a separate set makes the containment check cheaper
        Set<Class<?>> uniqueTypes = new HashSet<Class<?>>(combinedTypes);

        // run the registered default
        Collection<Class<?>> additionalTypes = defaultConstructor
                .additionalTypes(getNestingDepth(), primaryType, primaryType,
                        allTypes, fixedArgCopy);

        // null is acceptable to indicate a no-op
        if (null != additionalTypes)
        {
            addAdditionalTypes(additionalTypes, uniqueTypes, combinedTypes);
        }

        List<Class<?>> allWithParents = new ArrayList<Class<?>>(allTypes.length);

        flattenAllInterfaces(allWithParents, allTypes);

        for (Class<?> singleType : allWithParents)
        {
            if (!constructionDelegates.containsKey(singleType))
            {
                continue;
            }

            ConstructionDelegate delegate = constructionDelegates
                    .get(singleType);

            if (null == delegate)
            {
                continue;
            }

            additionalTypes = delegate.additionalTypes(getNestingDepth(),
                    primaryType, singleType, allTypes, fixedArgCopy);

            // null is acceptable to indicate a no-op
            if (null == additionalTypes)
            {
                continue;
            }

            addAdditionalTypes(additionalTypes, uniqueTypes, combinedTypes);
        }

        return combinedTypes.toArray(new Class<?>[combinedTypes.size()]);
    }

    private void doAfterInit(boolean copy, Object bean, Class<?>[] allTypes)
    {
        if (!copy)
        {
            defaultConstructor
                    .initValues(nestingDepth.get(), allTypes[0], bean);
        }

        defaultConstructor.initBehaviors(nestingDepth.get(), allTypes[0], bean);

        List<Class<?>> allWithParents = new ArrayList<Class<?>>(allTypes.length);

        flattenAllInterfaces(allWithParents, allTypes);

        for (Class<?> singleType : allWithParents)
        {
            if (!constructionDelegates.containsKey(singleType))
            {
                continue;
            }

            ConstructionDelegate delegate = constructionDelegates
                    .get(singleType);

            if (null == delegate)
            {
                continue;
            }

            if (!copy)
            {
                delegate.initValues(nestingDepth.get(), singleType, bean);
            }

            delegate.initBehaviors(nestingDepth.get(), singleType, bean);
        }
    }

    private void addAdditionalTypes(Collection<Class<?>> additionalTypes,
            Set<Class<?>> uniqueTypes, List<Class<?>> combinedTypes)
    {

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

    private int getNestingDepth()
    {
        // set to zero if this is called from combineAdditionalTypes
        return nestingDepth == null || nestingDepth.get() == null ? 0
                : nestingDepth.get();
    }

    /*
     * build flat list of all interfaces by traversing and adding any interfaces
     * elements in allTypes extend
     */
    private void flattenAllInterfaces(List<Class<?>> allTypesWithParents,
            Class<?>[] allTypes)
    {
        for (Class<?> forType : allTypes)
        {
            Class<?>[] interfaces = forType.getInterfaces();

            allTypesWithParents.add(forType);

            if (null == interfaces)
            {
                continue;
            }

            flattenAllInterfaces(allTypesWithParents, interfaces);
        }
    }
}
