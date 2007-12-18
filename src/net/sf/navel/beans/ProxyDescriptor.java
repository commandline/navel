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
import java.beans.EventSetDescriptor;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectInputValidation;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

/**
 * Collects the introspection and reflection data for the proxy in one place,
 * where it can be safely shared. Also tucks away the serialization problems
 * with introspection data which is not serializable. Despite the transient
 * members that cannot be flagged as final, this class should be considered
 * effectively immutable.
 * 
 * @author cmdln
 * 
 */
public class ProxyDescriptor implements Serializable, ObjectInputValidation
{

    private static final long serialVersionUID = 8336801578532507921L;

    private final Class<?> primaryType;

    private final Set<Class<?>> proxiedInterfaces;

    private transient Set<Method> methods;

    /**
     * Interfaces that can have {@link InterfaceDelegate} instances attached.
     */
    final Set<Class<?>> withDelegatableMethods;

    /**
     * The validateObject method re-populates this during deserialization.
     */
    transient Set<BeanInfo> proxiedBeanInfo;

    /**
     * The validateObject method re-populates this during deserialization.
     */
    transient Map<String, PropertyDescriptor> propertyDescriptors;

    ProxyDescriptor(Class<?>[] proxiedClasses)
    {
        Set<Class<?>> tempClasses = new HashSet<Class<?>>(proxiedClasses.length);
        Set<BeanInfo> tempInfo = new HashSet<BeanInfo>(proxiedClasses.length);

        Map<String, PropertyDescriptor> tempProperties = new HashMap<String, PropertyDescriptor>();
        Set<Method> tempMethods = new HashSet<Method>();
        Set<Class<?>> tempWithDelegatable = new HashSet<Class<?>>();

        this.primaryType = mapTypes(proxiedClasses, tempInfo, tempClasses);

        this.proxiedInterfaces = Collections.unmodifiableSet(tempClasses);
        this.proxiedBeanInfo = Collections.unmodifiableSet(tempInfo);

        for (BeanInfo beanInfo : this.proxiedBeanInfo)
        {
            Class<?> candidateBeanClass = beanInfo.getBeanDescriptor()
                    .getBeanClass();
            Map<String, PropertyDescriptor> forType = mapProperties(beanInfo);

            checkConflicts(tempProperties, forType, candidateBeanClass);

            tempProperties.putAll(forType);

            int nonPropertyMethods = filterMethods(tempMethods, beanInfo);

            if (0 == nonPropertyMethods)
            {
                continue;
            }

            tempWithDelegatable
                    .add(beanInfo.getBeanDescriptor().getBeanClass());
        }

        this.propertyDescriptors = Collections.unmodifiableMap(tempProperties);
        this.withDelegatableMethods = Collections
                .unmodifiableSet(tempWithDelegatable);
        this.methods = Collections.unmodifiableSet(tempMethods);
    }

    /**
     * @return the primaryType
     */
    public Class<?> getPrimaryType()
    {
        return primaryType;
    }

    /**
     * @return the proxiedInterfaces
     */
    public Set<Class<?>> getProxiedInterfaces()
    {
        return proxiedInterfaces;
    }

    /**
     * @return The {@link PropertyDescriptor} values for all of the proxy's
     *         interfaces keyed by each property's name. The map returned is not
     *         modifiable and will throw an exception if an alteration is
     *         attempted.
     */
    public Map<String, PropertyDescriptor> getPropertyDescriptors()
    {
        // this is already unmodifiabe, so safe to share as is
        return propertyDescriptors;
    }

    /**
     * Used during de-serialization to restore the non serializable
     * instrospection metadata from the JavaBeans API.
     * 
     * @see java.io.ObjectInputValidation#validateObject()
     */
    public void validateObject() throws InvalidObjectException
    {
        Set<BeanInfo> tempInfo = new HashSet<BeanInfo>();

        for (Class<?> proxiedInterface : proxiedInterfaces)
        {
            BeanInfo beanInfo = JavaBeanHandler.introspect(proxiedInterface);

            tempInfo.add(beanInfo);
        }

        this.proxiedBeanInfo = Collections.unmodifiableSet(tempInfo);

        Map<String, PropertyDescriptor> tempProperties = new HashMap<String, PropertyDescriptor>();
        Set<Method> tempMethods = new HashSet<Method>();

        for (BeanInfo beanInfo : proxiedBeanInfo)
        {
            tempProperties.putAll(PropertyValues.mapProperties(beanInfo));

            filterMethods(tempMethods, beanInfo);
        }

        this.propertyDescriptors = Collections.unmodifiableMap(tempProperties);
        this.methods = Collections.unmodifiableSet(tempMethods);
    }

    /**
     * Factory method allows for future enhancements, like caching if required,
     * without breaking existing code.
     * 
     * @param proxiedClasses
     *            Classes the proxy under construction will back.
     * 
     * @return An immutable, safe to share collection of introspection and
     *         reflection data.
     */
    static ProxyDescriptor create(Class<?>[] proxiedClasses)
    {
        return new ProxyDescriptor(proxiedClasses);
    }

    final boolean handles(Method method)
    {
        return methods.contains(method);
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */

    @Override
    public boolean equals(Object obj)
    {
        if (null == obj)
        {
            return false;
        }

        if (!(obj instanceof ProxyDescriptor))
        {
            return false;
        }

        ProxyDescriptor other = (ProxyDescriptor) obj;

        // primary type needs to be considered, too, as the sets may otherwise
        // be the same
        return primaryType.equals(other.primaryType)
                && proxiedInterfaces.equals(other.proxiedInterfaces);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        int result = 17;

        result = 37 * result + primaryType.hashCode();

        result = 37 * result + proxiedInterfaces.hashCode();

        return result;
    }

    /**
     * @see java.lang.Object#toString()
     */

    @Override
    public String toString()
    {
        return String
                .format(
                        "proxyDescriptor = {primary type = %1$s, additional interfaces = %2$s}",
                        primaryType.getName(), printClasses(primaryType,
                                proxiedInterfaces));
    }

    private static final Map<String, PropertyDescriptor> mapProperties(
            BeanInfo beanInfo)
    {
        Map<String, PropertyDescriptor> byNames = new HashMap<String, PropertyDescriptor>();

        for (PropertyDescriptor propertyDescriptor : beanInfo
                .getPropertyDescriptors())
        {
            byNames.put(propertyDescriptor.getName(), propertyDescriptor);
        }

        return byNames;
    }

    private static void checkConflicts(
            Map<String, PropertyDescriptor> entriesSoFar,
            Map<String, PropertyDescriptor> forType, Class<?> candidateBeanClass)
    {
        for (Entry<String, PropertyDescriptor> newEntry : forType.entrySet())
        {
            if (!entriesSoFar.containsKey(newEntry.getKey()))
            {
                continue;
            }

            PropertyDescriptor existingDescriptor = entriesSoFar.get(newEntry
                    .getKey());
            PropertyDescriptor newDescriptor = newEntry.getValue();

            if (!existingDescriptor.getPropertyType().equals(
                    newDescriptor.getPropertyType()))
            {
                throw new IllegalStateException(
                        String
                                .format(
                                        "The property, %1$s, of type, %2$s, on the interface, %3$s, conflicts on type with the property, %4$s, of type, %5$s, on interface, %6$s.",
                                        newDescriptor.getName(), newDescriptor
                                                .getPropertyType(),
                                        candidateBeanClass.getName(),
                                        existingDescriptor.getName(),
                                        existingDescriptor.getPropertyType(),
                                        existingDescriptor.getWriteMethod()
                                                .getDeclaringClass().getName()));
            }
        }
    }

    private static final int filterMethods(Set<Method> methods,
            BeanInfo beanInfo)
    {
        MethodDescriptor[] methodDescriptors = beanInfo.getMethodDescriptors();

        // don't bother with interfaces that only specify properties and
        // events
        if (methodDescriptors.length == 0)
        {
            return 0;
        }

        int nonPropertyCount = 0;

        for (MethodDescriptor methodDescriptor : methodDescriptors)
        {
            if (PropertyHandler.handles(methodDescriptor.getMethod()))
            {
                continue;
            }

            methods.add(methodDescriptor.getMethod());

            nonPropertyCount++;
        }

        return nonPropertyCount;
    }

    private final Class<?> mapTypes(Class<?>[] proxiedClasses,
            Set<BeanInfo> tempInfo, Set<Class<?>> tempClasses)
    {
        Class<?> candidatePrimary = null;

        for (int i = 0; i < proxiedClasses.length; i++)
        {
            Class<?> proxiedInterface = proxiedClasses[i];

            if (null == proxiedInterface)
            {
                throw new IllegalArgumentException(String.format(
                        "Found a null class at index, %1$d!", i));
            }

            if (!proxiedInterface.isInterface())
            {

                throw new IllegalArgumentException(
                        String
                                .format(
                                        "The class, %1$s, at index, %2$d, is not an interface.  Only interfaces may be proxied.",
                                        proxiedInterface.getName(), i));
            }

            if (null == candidatePrimary)
            {
                candidatePrimary = proxiedInterface;
            }

            BeanInfo beanInfo = JavaBeanHandler.introspect(proxiedInterface);

            tempInfo.add(beanInfo);

            forbidEvents(beanInfo);

            tempClasses.add(proxiedInterface);

            // recurse any interfaces this one extends
            mapTypes(proxiedInterface.getInterfaces(), tempInfo, tempClasses);
        }

        return candidatePrimary;
    }

    /**
     * Ensure that for the proxied class, there are no events declared, since
     * PropertyBeanHandler doesn't support events.
     * 
     * @param beanInfo
     *            Introspection data about the proxied class.
     */
    private final void forbidEvents(BeanInfo beanInfo)
            throws UnsupportedFeatureException
    {
        EventSetDescriptor[] events = beanInfo.getEventSetDescriptors();

        if ((null != events) && (0 != events.length))
        {
            throw new UnsupportedFeatureException(
                    "PropertyBeanHandler does not support JavaBeans with events.");
        }
    }

    private final void readObject(ObjectInputStream input) throws IOException,
            ClassNotFoundException
    {
        input.defaultReadObject();

        input.registerValidation(this, 0);
    }

    private String printClasses(Class<?> primaryType,
            Set<Class<?>> proxiedInterfaces)
    {
        Set<String> sortedInterfaces = new TreeSet<String>();

        for (Class<?> additionalInterface : proxiedInterfaces)
        {
            if (additionalInterface.equals(primaryType))
            {
                continue;
            }

            sortedInterfaces.add(additionalInterface.getName());
        }

        return sortedInterfaces.toString();
    }
}
