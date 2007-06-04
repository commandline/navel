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

import static net.sf.navel.beans.PrimitiveSupport.validate;

import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.navel.beans.InvalidPropertyValueException;
import net.sf.navel.beans.PropertyBeanHandler;
import net.sf.navel.beans.UnsupportedFeatureException;

import org.apache.log4j.Logger;

/**
 * Provides validation on construction support for the DelegateBeanHandler,
 * ensuring that all the rules that must be true for the handler to work are.
 * This means that the proxied interface only defines JavaBean properties and
 * not any methods or events.
 * 
 * @author cmndln
 * @version $Revision: 1.8 $, $Date: 2005/09/26 16:42:53 $
 */
public class PropertyValidator implements Validator, Serializable
{

    private static final long serialVersionUID = -6780317578317368699L;

    private static final Logger LOGGER = Logger
            .getLogger(PropertyValidator.class);

    protected final PropertyBeanHandler<?> handler;

    protected final Class proxiedClass;

    protected final Map<String, Object> values;

    public PropertyValidator(PropertyBeanHandler<?> handler)
    {
        this.handler = handler;
        proxiedClass = handler.getProxiedClass();
        // this copies the map values, so we can fiddle with them without
        // hurting anything
        values = handler.getValues();
    }

    /**
     * Method for doing validation on construction. Must be support from
     * constructor to allow constructor to initialize everything, first.
     * 
     * @throws UnsupportedFeatureException
     *             Thrown if anything looks suspect.
     * @throws InvalidPropertyValueException
     *             Thrown if there is a problem with any of the initial values.
     */
    public void validateAll() throws UnsupportedFeatureException,
            InvalidPropertyValueException
    {
        BeanInfo beanInfo = introspect();

        validateEvents(beanInfo);
        validateMethods(beanInfo);
        validateData(beanInfo);
    }

    /**
     * Ensure that for the proxied class, there are no events declared, since
     * PropertyBeanHandler doesn't support events.
     * 
     * @param beanInfo
     *            Introspection data about the proxied class.
     */
    public void validateEvents(BeanInfo beanInfo)
            throws UnsupportedFeatureException
    {
        EventSetDescriptor[] events = beanInfo.getEventSetDescriptors();

        if ((null != events) && (0 != events.length))
        {
            throw new UnsupportedFeatureException(
                    "PropertyBeanHandler does not support JavaBeans with events.");
        }
    }

    /**
     * Ensure that all methods for the proxied class are concerned with property
     * manipulation, since PropertyBeanHandler doesn't support any purely
     * behavioral methods.
     * 
     * @param beanInfo
     *            Introspection data about the proxied class.
     */
    public void validateMethods(BeanInfo beanInfo)
            throws UnsupportedFeatureException
    {
        List<MethodDescriptor> methods = getAllMethods(beanInfo);

        if ((null == methods) || (methods.isEmpty()))
        {
            return;
        }

        Set<String> methodNames = new HashSet<String>(methods.size());

        // the default beaninof includes the accessors and mutators in the
        // method descriptors, so we need to filter them out and see if there
        // are any other non-property related methods
        for (MethodDescriptor method : methods)
        {
            methodNames.add(method.getMethod().getName());
        }

        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace("Found methods: ");
            LOGGER.trace(methodNames);
        }

        List<PropertyDescriptor> properties = getAllProperties(beanInfo);

        checkProperties(properties);

        for (PropertyDescriptor property : properties)
        {
            filterMethods(methodNames, property);
        }

        if (!methodNames.isEmpty())
        {
            throw new UnsupportedFeatureException(
                    "PropertyBeanHandler does not support JavaBeans with methods other than property methods.  "
                            + "Found the following invalid methods, "
                            + methodNames + ".");
        }
    }

    /**
     * Ensure that the map of initial values is valid per the properties
     * described by the proxied class.
     * 
     * @param beanInfo
     *            Introspection data about proxied class.
     * @throws InvalidPropertyValueException
     *             Thrown in an initial value doesn't match up with the proxied
     *             class' properties, by name or type.
     */
    public void validateData(BeanInfo beanInfo)
            throws InvalidPropertyValueException
    {
        if ((null == values) || values.isEmpty())
        {
            return;
        }

        List<PropertyDescriptor> properties = getAllProperties(beanInfo);

        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace("Found properties:");
            LOGGER.trace(formatProperties(properties));
        }

        eliminateMatches(properties);

        if (!values.isEmpty())
        {
            throw new InvalidPropertyValueException(
                    "Extra values found in initial value map that do not match any known property for bean type "
                            + proxiedClass.getName() + ": " + values.keySet());
        }
    }

    public void revalidateData() throws InvalidPropertyValueException
    {
        // need to flush and re-fill the local copy so we are validating the
        // latest data in the handler we are supporting
        values.clear();
        values.putAll(handler.getValues());

        validateData(introspect());
    }

    /**
     * Scrape out valid property handlers, allow overriding for non-trivial
     * property types.
     */
    protected void filterMethods(Set<String> methodNames,
            PropertyDescriptor property)
    {
        checkMethod(methodNames, property.getReadMethod());
        checkMethod(methodNames, property.getWriteMethod());
    }

    /**
     * Eliminiate any values that exactly match known properties.
     * 
     * @param properties
     *            Properties to eliminate against.
     * @throws InvalidPropertyValueException
     *             In case there is a coercion problem with an eliminated
     *             property.
     */
    protected void eliminateMatches(List<PropertyDescriptor> properties)
            throws InvalidPropertyValueException
    {
        for (int i = 0; i < properties.size(); i++)
        {
            String propertyName = properties.get(i).getName();

            if (!values.containsKey(propertyName))
            {
                continue;
            }

            Object propertyValue = values.get(propertyName);

            checkValue(properties.get(i), propertyName, propertyValue);

            values.remove(propertyName);
        }
    }

    protected String formatProperties(List<PropertyDescriptor> properties)
    {
        StringBuffer buffer = new StringBuffer("[");

        for (PropertyDescriptor property : properties)
        {
            if (buffer.length() != 1)
            {
                buffer.append(", ");
            }

            buffer.append(property.getName());
        }

        buffer.append(']');

        return buffer.toString();
    }

    protected final void checkMethod(Set methodNames, Method method)
    {
        if ((method != null) && methodNames.contains(method.getName()))
        {
            methodNames.remove(method.getName());
        }
    }

    protected void checkValue(PropertyDescriptor propertyDescriptor,
            String propertyName, Object propertyValue)
    {
        Class<?> propertyType = propertyDescriptor.getPropertyType();

        if (propertyType.isPrimitive())
        {
            validate(propertyName, propertyType, propertyValue);

            return;
        }

        if (propertyValue != null && !propertyType.isInstance(propertyValue))
        {
            throw new InvalidPropertyValueException(propertyValue + " of type "
                    + propertyValue.getClass().getName()
                    + " is not a valid value for property " + propertyName
                    + " of type " + propertyType.getName());
        }
    }

    List<PropertyDescriptor> getAllProperties(BeanInfo beanInfo)
    {
        List<PropertyDescriptor> properties = new ArrayList<PropertyDescriptor>(
                Arrays.asList(beanInfo.getPropertyDescriptors()));

        for (Class<?> parent : proxiedClass.getInterfaces())
        {
            getProperties(properties, parent);
        }

        return properties;
    }

    private void getProperties(List<PropertyDescriptor> properties,
            Class targetClass)
    {
        if (targetClass == null || Serializable.class.equals(targetClass))
        {
            return;
        }

        BeanInfo parentBeanInfo = introspect(targetClass);

        PropertyDescriptor[] found = parentBeanInfo.getPropertyDescriptors();

        if (found.length != 0)
        {
            properties.addAll(Arrays.asList(found));
        }

        for (Class<?> parent : targetClass.getInterfaces())
        {
            getProperties(properties, parent);
        }
    }

    private List<MethodDescriptor> getAllMethods(BeanInfo beanInfo)
    {
        List<MethodDescriptor> methods = new ArrayList<MethodDescriptor>(Arrays
                .asList(beanInfo.getMethodDescriptors()));

        for (Class<?> parent : proxiedClass.getInterfaces())
        {
            getMethods(methods, parent);
        }

        return methods;
    }

    private void getMethods(List<MethodDescriptor> methods, Class<?> targetClass)
    {
        if (targetClass == null || Serializable.class.equals(targetClass))
        {
            return;
        }

        BeanInfo parentBeanInfo = introspect(targetClass);

        MethodDescriptor[] found = parentBeanInfo.getMethodDescriptors();

        if (found.length != 0)
        {
            methods.addAll(Arrays.asList(found));
        }

        for (Class<?> parent : targetClass.getInterfaces())
        {
            getMethods(methods, parent);
        }
    }

    private void checkProperties(List<PropertyDescriptor> properties)
            throws UnsupportedFeatureException
    {
        if ((null == properties) || (properties.isEmpty()))
        {
            throw new UnsupportedFeatureException(
                    "JavaBean interface must have at least one property.");
        }
        else if (1 == properties.size())
        {
            PropertyDescriptor property = properties.get(0);

            if ("class".equals(property.getName()))
            {
                throw new UnsupportedFeatureException(
                        "JavaBean interface must have at least one property, other than \"class\".");
            }
        }
    }

    private BeanInfo introspect() throws InvalidPropertyValueException
    {
        return introspect(proxiedClass);
    }

    private BeanInfo introspect(Class targetClass)
            throws InvalidPropertyValueException
    {
        try
        {
            BeanInfo beanInfo = Introspector.getBeanInfo(targetClass);

            return beanInfo;
        }
        catch (IntrospectionException e)
        {
            throw new InvalidPropertyValueException(
                    "Unable to introspect proxied class.");
        }
    }
}
