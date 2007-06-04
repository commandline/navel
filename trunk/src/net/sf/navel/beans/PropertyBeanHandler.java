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

import static net.sf.navel.beans.PrimitiveSupport.getElement;
import static net.sf.navel.beans.PrimitiveSupport.handleNull;
import static net.sf.navel.beans.PrimitiveSupport.isPrimitiveArray;
import static net.sf.navel.beans.PrimitiveSupport.setElement;

import java.beans.Introspector;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.sf.navel.beans.validation.NestedValidator;
import net.sf.navel.beans.validation.PropertyValidator;
import net.sf.navel.beans.validation.Validator;

import org.apache.log4j.Logger;

/**
 * An invocation handler meant to support dynamic proxies for interfaces that
 * adhere to the JavaBean spec and only contain properties. On construction,
 * this class will throw an exception if the interface includes any events or
 * methods not having to do with property manipulation.
 * 
 * @author cmdln
 */
public class PropertyBeanHandler<T> implements InvocationHandler, Serializable
{

    private static final long serialVersionUID = 8234362825076556906L;

    private static final Logger LOGGER = Logger
            .getLogger(PropertyBeanHandler.class);

    // verb constants
    static final String WRITE = "set";

    static final String READ = "get";

    static final String BEING = "is";

    protected Validator validator;

    protected Map<String, Object> values;

    protected Class<T> proxiedClass;

    protected T proxy;

    /**
     * Each handler instance hangs onto a reference of the interface it
     * supports, so it can introspect and make sure it is able to support
     * everything declared in the interface.
     * 
     * @param proxiedClass
     *            The interface this handler supports via a dynamic proxy.
     * @throws UnsupportedFeatureException
     *             Thrown if the interface declares anything this handler
     *             doesn't support.
     * @throws InvalidPropertyValueException
     *             Thrown if some value in the Map is inappropriate for the
     *             properties of the proxied class.
     */
    public PropertyBeanHandler(Class<T> proxiedClass)
            throws UnsupportedFeatureException, InvalidPropertyValueException
    {
        this(proxiedClass, new HashMap<String, Object>());
    }

    /**
     * Overload that assumes false for resolving nested properties in the values
     * argument, to preserve backwards compatibility.
     */
    public PropertyBeanHandler(Class<T> proxiedClass, Map<String, Object> values)
            throws UnsupportedFeatureException, InvalidPropertyValueException
    {
        this(proxiedClass, values, false);
    }

    /**
     * Each handler instance hangs onto a reference of the interface it
     * supports, so it can introspect and make sure it is able to support
     * everything declared in the interface. This version of the constructor
     * also accepts a map, which is copied to initialize the handler's
     * underlying storage. A shallow copy is made so that changes to the
     * original map don't affect the bean contents. This constructor is the only
     * way to set the values for read-only properties.
     * 
     * @param proxiedClass
     *            The interface this handler supports via a dynamic proxy.
     * @param values
     *            Initial values.
     * @param resolveNested
     *            Should this handler attempt to resolve nested properties, by
     *            name. The parent property shared by nested properties itself
     *            must be a Navel bean.
     * @throws UnsupportedFeatureException
     *             Thrown if the interface declares anything this handler
     *             doesn't support.
     * @throws InvalidPropertyValueException
     *             Thrown if some value in the Map is inappropriate for the
     *             properties of the proxied class.
     */
    public PropertyBeanHandler(Class<T> proxiedClass,
            Map<String, Object> values, boolean resolveNested)
            throws UnsupportedFeatureException, InvalidPropertyValueException
    {
        this.proxiedClass = proxiedClass;

        // shallow copy so that edits to the original map
        // don't affect the bean contents
        this.values = new HashMap<String, Object>(values);

        if (resolveNested)
        {
            validator = new NestedValidator(this);
        }
        else
        {
            validator = new PropertyValidator(this);
        }

        validator.validateAll();
    }

    /**
     * Default constructor so children do not have to provide a constructor or
     * call super() at all.
     */
    protected PropertyBeanHandler()
    {
        // default constructor for daughters
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
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable
    {
        String methodName = method.getName();

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Invoking " + methodName);

            if (args != null)
            {
                LOGGER.debug("Arguments " + Arrays.asList(args));
            }
        }

        if (methodName.startsWith(WRITE))
        {
            handleWrite(methodName, args);

            return null;
        }
        else if (methodName.startsWith(READ))
        {
            return handleRead(method, args);
        }
        else if (methodName.startsWith(BEING))
        {
            return handleBeing(methodName);
        }
        else
        {
            return proxyToObject("", method, args);
        }
    }

    /**
     * Allows the associated DelegationTarget to manipulate the underlying
     * values.
     * 
     * @param key
     *            Property by name.
     * @return Value of property.
     */
    public Object get(String key)
    {
        return values.get(key);
    }

    /**
     * Returns a copy of the underlying Map that provides storage for the
     * handler. The copy is a shallow copy so that changes to it won't affect
     * the bean contents.
     * 
     * @return Shallow copy.
     */
    public Map<String, Object> getValues()
    {
        return getValues(false);
    }

    /**
     * Overload that allows specification of handling of nested properties.
     * 
     * @return Shallow copy.
     */
    public Map<String, Object> getValues(boolean flattenNested)
    {
        Map<String, Object> copy = new HashMap<String, Object>(values);

        if (flattenNested)
        {
            BeanManipulator.expandNestedBeans(copy);
        }

        return copy;
    }

    /**
     * Call forward since getValues() returns a copy, as a convenience for Navel
     * aware code to be able to do cheaper bulk updates.
     * 
     * @param key
     *            Property name to which the new value should be associated.
     * @param value
     *            New value to be put into the underlying storage.
     * @throws UnsupportedFeatureException
     *             In case re-validating encounters a problem with JavaBean
     *             introspection.
     * @throws InvalidPropertyValueException
     *             In case the new value violates the property definitions for
     *             the proxied class.
     */
    public void put(String key, Object value)
            throws InvalidPropertyValueException
    {
        this.values.put(key, value);
        validator.revalidateData();
    }

    /**
     * Call forward since getValues() returns a copy, as a convenience for Navel
     * aware code to be able to do cheaper bulk updates.
     * 
     * @param values
     *            New values to be put into the underlying storage.
     * @throws UnsupportedFeatureException
     *             In case re-validating encounters a problem with JavaBean
     *             introspection.
     * @throws InvalidPropertyValueException
     *             In case the new value violates the property definitions for
     *             the proxied class.
     */
    public void putAll(Map<String, Object> values)
            throws InvalidPropertyValueException
    {
        this.values.putAll(values);
        validator.revalidateData();
    }

    /**
     * Call forward since getValues() returns a copy, as a convenience for Navel
     * aware code to be able to do cheaper bulk updates.
     * 
     * @param key
     *            Keys to remove from the underlying map.
     */
    public Object remove(String key)
    {
        return this.values.remove(key);
    }

    /**
     * Call forward since getValues() returns a copy, as a convenience for Navel
     * aware code to be able to do cheaper bulk updates.
     * 
     * @param keys
     *            Keys to remove from the underlying map.
     */
    public void removeAll(Set<String> keys)
    {
        for (Iterator<String> keyIter = keys.iterator(); keyIter.hasNext();)
        {
            this.values.remove(keyIter.next());
        }
    }

    /**
     * Call forward since getValues() returns a copy, as a convenience for Navel
     * aware code to be able to clear out the underlying storage.
     */
    public void clear()
    {
        this.values.clear();
    }

    /**
     * Accessor for proxied class member, mostly so validator can get to it.
     * 
     * @return Proxed class member.
     */
    public Class<T> getProxiedClass()
    {
        return proxiedClass;
    }

    /**
     * Factory method to create and/or return a dynamic proxy for the indicated
     * class supported by this handler instance.
     * 
     * @return A dynamic proxy which should be castabled to the proxied class.
     */
    @SuppressWarnings("unchecked")
    public T getProxy()
    {
        if (null == proxy)
        {
            ClassLoader classLoader = proxiedClass.getClassLoader();
            Class[] implemented = new Class[]
            { proxiedClass };
            proxy = (T) Proxy.newProxyInstance(classLoader, implemented, this);
        }

        return proxy;
    }

    Object proxyToObject(final String message, final Method method,
            final Object[] args) throws UnsupportedFeatureException
    {
        int count = (null == args) ? 0 : args.length;

        Class[] argTypes = new Class[count];

        // the only method in Object that takes an argument is equals, and it
        // takes another Object as an argument
        for (int i = 0; i < count; i++)
        {
            argTypes[i] = Object.class;
        }

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Proxying method, " + method.getName()
                    + " with arguments (" + parseArguments(argTypes)
                    + ") to underlying Map.");
        }

        if (method.getName() == "toString" && argTypes.length == 0)
        {
            return filteredToString();
        }

        try
        {
            // use Object so anythingt else causes an exception--this is merely
            // a convenience so we don't have to implement the usual object
            // methods directly
            Object.class.getDeclaredMethod(method.getName(), argTypes);

            // need to handle equals a little differently, somewhere
            // between the proxy and the underlying map, based on experience
            if (method.getName().equals("equals"))
            {
                return handleEquals(args[0]);
            }

            return method.invoke(values, args);
        }
        catch (NoSuchMethodException e)
        {
            throw new UnsupportedFeatureException(
                    "Could not find a usable target for  method, "
                            + method.getName() + " on with arguments ("
                            + parseArguments(argTypes) + ").  " + message);
        }
        catch (IllegalAccessException e)
        {
            LOGGER
                    .warn("Illegal access proxying Object methods to internal Map.");

            return null;
        }
        catch (InvocationTargetException e)
        {
            LOGGER
                    .warn("Error invoking while proxying Object methods to internal Map.");

            return null;
        }
    }

    private String filteredToString()
    {
        // create a shallow map to filter out ignored properties, as well as to
        // consistently sort by the property names
        Map<String, Object> toPrint = new TreeMap<String, Object>(values);

        IgnoreToString ignore = proxiedClass
                .getAnnotation(IgnoreToString.class);

        if (null == ignore)
        {
            return toPrint.toString();
        }

        for (String ignoreName : ignore.value())
        {
            toPrint.remove(ignoreName);
        }

        return toPrint.toString();
    }

    private String parseArguments(Class[] argTypes)
    {
        if (null == argTypes)
        {
            return "";
        }

        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < argTypes.length; i++)
        {
            if (i != 0)
            {
                buffer.append(", ");
            }

            buffer.append(argTypes[i].getName());
        }

        return buffer.toString();
    }

    private void handleWrite(String methodName, Object[] args)
    {
        if (null == args)
        {
            throw new IllegalArgumentException(
                    "PropertyBeanHandler needs a value to write to the indicated property.");
        }

        String propertyName = getPropertyName(methodName, WRITE);

        if (1 == args.length)
        {
            values.put(propertyName, args[0]);
        }
        else if (2 == args.length)
        {
            handleIndexedWrite(propertyName, args);
        }
        else
        {
            throw new IllegalArgumentException(
                    "PropertyBeanHandler only supports writing simple and indexed properties.");
        }
    }

    private void handleIndexedWrite(String propertyName, Object[] args)
    {
        int index = getIndex(args[0], true);

        Object value = values.get(propertyName);

        if (isPrimitiveArray(value.getClass()))
        {
            setElement(value, index, args[1]);
        }
        else
        {
            Object[] indexed = (Object[]) value;

            indexed[index] = args[1];
        }
    }

    private Object handleRead(Method method, Object[] args)
    {
        String propertyName = getPropertyName(method.getName(), READ);

        if ((null == args) || (0 == args.length))
        {
            return handleNull(method.getReturnType(), values.get(propertyName));
        }
        else if (1 == args.length)
        {
            return handleIndexedRead(propertyName, args);
        }
        else
        {
            throw new IllegalArgumentException(
                    "PropertyBeanHandler only supports reading simple and indexed properties.");
        }
    }

    private Object handleIndexedRead(String propertyName, Object[] args)
    {
        int index = getIndex(args[0], false);

        Object value = values.get(propertyName);

        if (null == value)
        {
            LOGGER.warn("Trying to read null array.");
            return null;
        }

        if (isPrimitiveArray(value.getClass()))
        {
            return getElement(value, index);
        }

        if (List.class.isAssignableFrom(value.getClass()))
        {
            List indexed = (List) values.get(propertyName);

            Object element = indexed.get(index);

            return element;
        }

        Object[] indexed = (Object[]) values.get(propertyName);

        return indexed[index];
    }

    private Object handleBeing(String methodName)
    {
        String propertyName = getPropertyName(methodName, BEING);

        return handleNull(Boolean.class, values.get(propertyName));
    }

    private String getPropertyName(String methodName, String prefix)
    {
        int firstChar = prefix.length();
        
        String propertyName = methodName.substring(firstChar);
        
        propertyName = Introspector.decapitalize(propertyName);

        return propertyName;
    }

    private int getIndex(Object raw, boolean forWrite)
    {
        if ((null == raw) || !(raw instanceof Integer))
        {
            if (forWrite)
            {
                throw new IllegalArgumentException(
                        "Index for write is invalid.");
            }
            else
            {
                throw new IllegalArgumentException("Index for read is invalid.");
            }
        }

        Integer indexWrapper = (Integer) raw;
        int index = indexWrapper.intValue();

        if (index < 0)
        {
            if (forWrite)
            {
                throw new IllegalArgumentException(
                        "Index for write must be positive.");
            }
            else
            {
                throw new IllegalArgumentException(
                        "Index for read must be positive.");
            }
        }

        return index;
    }

    private Boolean handleEquals(Object value)
    {
        if (null == value)
        {
            return Boolean.FALSE;
        }

        // I don't think there is any sensical comparison, if the argument is
        // not a Proxy, too; maybe at some point iterating the defined fields
        // and comparing them individually? Too hard to second guess the bean
        // interface author, I think this is safer until something better occurs
        // to me
        if (!Proxy.isProxyClass(value.getClass()))
        {
            return Boolean.FALSE;
        }

        Object other = Proxy.getInvocationHandler(value);

        if (!(other instanceof PropertyBeanHandler))
        {
            return Boolean.FALSE;
        }

        PropertyBeanHandler otherHandler = (PropertyBeanHandler) other;

        return Boolean.valueOf(values.equals(otherHandler.values));
    }
}