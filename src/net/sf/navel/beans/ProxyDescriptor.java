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
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectInputValidation;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Collects the introspection and reflection data for the proxy in one place,
 * where it can be safely shared. Also tucks away the serialization problems
 * with introspection data which is not serializable.
 * 
 * @author cmdln
 * 
 */
public class ProxyDescriptor implements Serializable, ObjectInputValidation
{

    private static final long serialVersionUID = 8336801578532507921L;

    private final Class<?> primaryType;

    private final Set<Class<?>> proxiedInterfaces;

    /**
     * The validateObject method re-populates this during deserialization.
     */
    private transient Set<BeanInfo> proxiedBeanInfo;

    /**
     * The validateObject method re-populates this during deserialization.
     */
    transient Map<String, PropertyDescriptor> propertyDescriptors;

    private ProxyDescriptor(Class<?>[] proxiedClasses)
    {
        Set<Class<?>> tempClasses = new HashSet<Class<?>>(proxiedClasses.length);
        Set<BeanInfo> tempInfo = new HashSet<BeanInfo>(proxiedClasses.length);

        Map<String, PropertyDescriptor> tempProperties = new HashMap<String, PropertyDescriptor>();

        this.primaryType = mapTypes(proxiedClasses, tempInfo, tempClasses);

        this.proxiedInterfaces = Collections.unmodifiableSet(tempClasses);
        this.proxiedBeanInfo = Collections.unmodifiableSet(tempInfo);

        for (BeanInfo beanInfo : this.proxiedBeanInfo)
        {
            tempProperties.putAll(mapProperties(beanInfo));
        }

        this.propertyDescriptors = Collections.unmodifiableMap(tempProperties);
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
     * @return the proxiedBeanInfo
     */
    public Set<BeanInfo> getProxiedBeanInfo()
    {
        return proxiedBeanInfo;
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

        for (BeanInfo beanInfo : proxiedBeanInfo)
        {
            tempProperties.putAll(PropertyValues.mapProperties(beanInfo));
        }

        this.propertyDescriptors = Collections.unmodifiableMap(tempProperties);
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

    static final Map<String, PropertyDescriptor> mapProperties(BeanInfo beanInfo)
    {
        Map<String, PropertyDescriptor> byNames = new HashMap<String, PropertyDescriptor>();

        for (PropertyDescriptor propertyDescriptor : beanInfo
                .getPropertyDescriptors())
        {
            byNames.put(propertyDescriptor.getName(), propertyDescriptor);
        }

        return byNames;
    }

    private final void readObject(ObjectInputStream input) throws IOException,
            ClassNotFoundException
    {
        input.defaultReadObject();

        input.registerValidation(this, 0);
    }
}
