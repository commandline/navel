/**
 * Copyright (c) 2003-2007, Thomas Gideon
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
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * This abstract class is a base for working with properties of different types
 * through the BeanManipulator utility class via pure reflaction. These
 * manipulators allow dynamic programming of concrete JavaBeans.
 * 
 * For working with Navel beans dynamically, see {@link PropertyManipulator}.
 * 
 * @see BeanManipulator
 * @see PropertyManipulator
 * 
 * @author cmdln
 */
abstract class AbstractReflectionManipulator
{
    private static final Logger LOGGER = Logger
            .getLogger(AbstractReflectionManipulator.class);

    private static final Map<Class<?>, AbstractReflectionManipulator> MANIPULATORS = new HashMap<Class<?>, AbstractReflectionManipulator>(
            2);

    private static final AbstractReflectionManipulator DEFAULT_MANIPULATOR = new ReflectionSimpleManipulator();

    static
    {
        registerPropertyManipulator(IndexedPropertyDescriptor.class,
                new ReflectionIndexedManipulator());
    }

    /**
     * For custom PropertyDescriptor implementations, register an associated
     * PropertyManipulator here so that the framework can use it to fully
     * utilize the custom property extension.
     * 
     * @param descriptorType
     *            Must be assignable to PropertyDescriptor.
     * @param manipulator
     *            A daughter of PropertyManipulator.
     */
    static void registerPropertyManipulator(Class<?> descriptorType,
            AbstractReflectionManipulator manipulator)
    {
        if (!PropertyDescriptor.class.isAssignableFrom(descriptorType))
        {
            throw new IllegalArgumentException(
                    "Class argument must be assignable to PropertyDescriptor.");
        }

        if (PropertyDescriptor.class.equals(descriptorType))
        {
            throw new IllegalArgumentException(
                    "Class argument cannot be PropertyDescriptor.");
        }

        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace("Registering " + manipulator.getClass().getName()
                    + " for " + descriptorType.getName());
        }

        MANIPULATORS.put(descriptorType, manipulator);
    }

    /**
     * Factory method for getting PropertyManipulator instances based on the
     * PropertyDescriptor argument.
     * 
     * @param descriptorType
     *            Type of property to manipulate.
     * @return PropertyManipulator Instance suitable for working with the
     *         requested property type, may be null.
     */
    static AbstractReflectionManipulator getPropertyManipulator(
            Class<?> descriptorType)
    {
        AbstractReflectionManipulator manipulator = (AbstractReflectionManipulator) MANIPULATORS
                .get(descriptorType);

        if (manipulator == null)
        {
            manipulator = DEFAULT_MANIPULATOR;
        }

        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace("Getting " + manipulator.getClass().getName()
                    + " for " + descriptorType);
        }

        return manipulator;
    }

    /**
     * Implementations of this method should know how to parse out any special
     * expression embedded in the property name and manipulate the actual
     * property on the bean correctly, taking advantage of any custom
     * extensions.
     * 
     * @param property
     *            Descriptor for the target property.
     * @param propertyName
     *            Name of the property, may be an expression of some sort.
     * @param bean
     *            The bean to write to.
     * @param value
     *            The value to write.
     */
    abstract boolean handleWrite(PropertyDescriptor property,
            String propertyName, Object bean, Object value,
            boolean suppressExceptions);

    /**
     * Implementations of this method should know how to parse out any special
     * expression embedded in the property name and manipulate the actual
     * property on the bean correctly, taking advantage of any custom
     * extensions.
     * 
     * @param property
     *            Descriptor for the target property.
     * @param propertyName
     *            Name of the property, may be an expression of some sort.
     * @param bean
     *            The bean to write to.
     * @return The value read from the bean argument.
     */
    abstract Object handleRead(PropertyDescriptor property,
            String propertyName, Object bean, boolean suppressExceptions);

    /**
     * Utility method to find a PropertyDescriptor by name or partial name.
     * There is an implicit assumption that if the property name is actually
     * some sort of expression, like <code>foo[0]</code>, the starting
     * portion of the name will always be a valid property name.
     * 
     * @param beanClass
     *            Bean type to examine.
     * @param propertyName
     *            Property name, may be some sort of expression as long as it
     *            starts with a valid property name.
     */
    static final PropertyDescriptor findProperty(Class<?> beanClass,
            String propertyName)
    {
        if (null == beanClass || null == propertyName)
        {
            return null;
        }

        PropertyDescriptor[] properties = getProperties(beanClass);

        PropertyDescriptor property = null;

        for (int i = 0; i < properties.length; i++)
        {
            String candidateName = properties[i].getName();

            if (propertyName.equals(candidateName)
                    || propertyName.startsWith(candidateName))
            {
                property = properties[i];
            }
        }

        return property;
    }

    /**
     * This method will convert any String value into an appropriate type,
     * assuming a PropertyEditor for the target property type can be found
     * through the java.beans support classes. If the original value is not a
     * String or no suitable PropertyEditor can be found, the original value is
     * simply returned.
     * 
     * @param property
     *            Targety property, may have a registered PropertyEditor,
     *            otherwise will use the property type to try to find an editor.
     * @param propertyValue
     *            Original value to be converted, must be a String to be
     *            converted.
     */
    final Object convertPropertyValue(PropertyDescriptor property,
            Object propertyValue)
    {
        if (!(propertyValue instanceof String))
        {
            return propertyValue;
        }

        Class<?> propertyType = property.getPropertyType();
        PropertyEditor editor = getEditorForProperty(property);

        if (null == editor)
        {
            // the spec advises setting custom editors for entire types, the
            // PropertyEditorManager will find editors registered for a given
            // type, if any
            editor = PropertyEditorManager.findEditor(propertyType);

            if (null == editor)
            {
                return propertyValue;
            }
        }

        String valueText = (String) propertyValue;

        editor.setAsText(valueText);

        return editor.getValue();
    }

    /**
     * Utility class for getting the properties from a particular bean.
     * 
     * @param beanClass
     *            Type of bean of interest.
     * @return An array of PropertyDescriptor instances, may be zero length;
     *         will never be null.
     */
    static final PropertyDescriptor[] getProperties(Class<?> beanClass)
    {
        try
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("Introspecting " + beanClass.getName() + ".");
            }

            BeanInfo beanInfo = Introspector.getBeanInfo(beanClass);

            return beanInfo.getPropertyDescriptors();
        }
        catch (IntrospectionException e)
        {
            LOGGER.debug("Unable to introspect for properties.");

            // Null object pattern
            return new PropertyDescriptor[0];
        }
    }

    private PropertyEditor getEditorForProperty(PropertyDescriptor property)
    {
        PropertyEditor editor = null;

        // if an editor is set for the property, not advised by the spec per se,
        // then use that editor; if not explicitly set as part of the BeanInfo
        // for
        // the bean, then the editor will be null
        Class<?> editorClass = property.getPropertyEditorClass();

        if (null != editorClass)
        {
            try
            {
                editor = (PropertyEditor) editorClass.newInstance();
            }
            catch (IllegalAccessException e)
            {
                LOGGER.warn("Unable to create PropertyEditor of type "
                        + editorClass
                        + ", does this program have security access?");
            }
            catch (InstantiationException e)
            {
                LOGGER
                        .warn("Unable to create PropertyEditor of type "
                                + editorClass
                                + ", does this class provide a public, default constructor?");
            }
        }

        return editor;
    }
}
