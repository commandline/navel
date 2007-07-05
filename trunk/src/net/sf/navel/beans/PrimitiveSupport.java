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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.sf.navel.beans.support.ArrayManipulator;
import net.sf.navel.beans.support.BooleanSupport;
import net.sf.navel.beans.support.ByteSupport;
import net.sf.navel.beans.support.CharacterSupport;
import net.sf.navel.beans.support.DefaultPrimitive;
import net.sf.navel.beans.support.DoubleSupport;
import net.sf.navel.beans.support.FloatSupport;
import net.sf.navel.beans.support.IntegerSupport;
import net.sf.navel.beans.support.LongSupport;
import net.sf.navel.beans.support.ShortSupport;

import org.apache.log4j.Logger;

/**
 * A utility class for dealing with properties of primitive types, including
 * arrays of primitives, which can be very tricky to reflect correctly.
 * Implemented as a Singleton for efficiency and simplicity--all methods should
 * remain re-entrant, i.e. should never rely on member data.
 * 
 * @author cmdln
 */
public class PrimitiveSupport
{
    private static final Logger LOGGER = Logger
            .getLogger(PrimitiveSupport.class);

    private static final PrimitiveSupport SINGLETON = new PrimitiveSupport();

    private final Map<Class, ArrayManipulator> manipulators;

    private final Map<Class, DefaultPrimitive> defaults;

    private final Map<Class, Class> wrappers;

    /**
     * Prevent external instantiation.
     */
    private PrimitiveSupport()
    {
        manipulators = initManipulators();
        defaults = initDefaults();
        wrappers = initWrappers();
    }

    /**
     * Method brute forces checking that a given value can be used with the
     * defined primitive property type. Only maintainable because the list of
     * primitive types is set.
     * 
     * @param propertyName
     *            Property in question, for descriptive exception message.
     * @param propertyType
     *            Type of property from the declaring interface.
     * @param propertyValue
     *            Value, typically from the PropertyBeanHandler's underlying
     *            Map.
     */
    public static void validate(String propertyName, Class propertyType,
            Object propertyValue) throws InvalidPropertyValueException
    {
        SINGLETON.validatePrimitive(propertyName, propertyType, propertyValue);
    }

    /**
     * Similar to <code>validate</code> except that instead of throwing an
     * exception if the value argument isn't a valid wrapper for a primitive,
     * this method just returns a boolean.
     * 
     * @param propertyType
     *            Type of property from the declaring interface.
     * @param propertyValue
     *            Value, typically from the PropertyBeanHandler's underlying
     *            Map.
     * @return Is the propertyValue a valid wrapper instance for the property
     *         type?
     */
    public static boolean isValid(Class propertyType, Object propertyValue)
    {
        return SINGLETON.isValidPrimitive(propertyType, propertyValue);
    }

    /**
     * Checks to see that the Class argument is one of the primitive array types
     * supported by this class.
     * 
     * @param propertyType
     *            Use a class, because the String from getName isn't descriptive
     *            and my suspicion is it may change from JVM to JVM.
     * @return Can this class work with the array type?
     */
    public static boolean isPrimitiveArray(Class propertyType)
    {
        return SINGLETON.manipulators.containsKey(propertyType);
    }

    /**
     * Gets an element out of a primitive array for the given index. Uses a
     * batch of inner classes keyed to the various primitive array Class
     * instances.
     * 
     * @param array
     *            Object reference to the array.
     * @param index
     *            Index of desired element.
     * @return Element at index, type as Object--dynamic proxies will cast on
     *         the way out.
     */
    public static Object getElement(Object array, int index)
    {
        ArrayManipulator manipulator = SINGLETON.manipulators.get(array
                .getClass());

        return manipulator.getElement(array, index);
    }

    /**
     * Sets an element into a primitive array for the given index. Uses a batch
     * of inner classes keyed to the various primitive array Class instances.
     * 
     * @param array
     *            Object reference to the array.
     * @param index
     *            Index of desired element.
     * @param value
     *            Value to set into the array, inner classes will cast
     *            appropriately based on Class on array argument.
     */
    public static void setElement(Object array, int index, Object value)
    {
        ArrayManipulator manipulator = SINGLETON.manipulators.get(array
                .getClass());

        manipulator.setElement(array, index, value);
    }

    /**
     * Since we cannot have null primitives, this method figures out what to
     * return if the value argument is associated with a primitive type an
     * assigned null, as if value were an uninitialized primitive of the
     * appropriate type. PropertyBeanHandler uses this to allow correct function
     * of primitive property types when get has been called without an initial
     * value being set in the constructor or by a previous write to the
     * property, just as if the bean were implemented concretely and were
     * accessing an uninitialized member variable.
     * 
     * @param returnType
     *            Return type of interest.
     * @param value
     *            Value of interest, may be null.
     * @return The value itself it it is not a primitive or is not null, an
     *         appropriate primitive default otherwise.
     */
    public static Object handleNull(Class returnType, Object value)
    {
        if (!returnType.isPrimitive() || (null != value))
        {
            return value;
        }

        DefaultPrimitive defaultPrimitive = SINGLETON.defaults.get(returnType);

        if (null == defaultPrimitive)
        {
            LOGGER.warn("Could not find DefaultPrimitive for "
                    + returnType.getName());
            return value;
        }

        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace("Handling null for " + returnType.getName());
            LOGGER.trace("Found DefaultPrimitive "
                    + defaultPrimitive.getClass().getName());
        }

        return defaultPrimitive.getValue();
    }

    private Map<Class, ArrayManipulator> initManipulators()
    {
        Map<Class, ArrayManipulator> manipulators = new HashMap<Class, ArrayManipulator>(
                8);
        manipulators.put(BooleanSupport.ARRAY_TYPE, BooleanSupport
                .getInstance());
        manipulators.put(ByteSupport.ARRAY_TYPE, ByteSupport.getInstance());
        manipulators.put(ShortSupport.ARRAY_TYPE, ShortSupport.getInstance());
        manipulators.put(IntegerSupport.ARRAY_TYPE, IntegerSupport
                .getInstance());
        manipulators.put(LongSupport.ARRAY_TYPE, LongSupport.getInstance());
        manipulators.put(FloatSupport.ARRAY_TYPE, FloatSupport.getInstance());
        manipulators.put(DoubleSupport.ARRAY_TYPE, DoubleSupport.getInstance());
        manipulators.put(CharacterSupport.ARRAY_TYPE, CharacterSupport
                .getInstance());

        return Collections.unmodifiableMap(manipulators);
    }

    private Map<Class, DefaultPrimitive> initDefaults()
    {
        Map<Class, DefaultPrimitive> defaults = new HashMap<Class, DefaultPrimitive>(
                8);

        defaults.put(Boolean.TYPE, BooleanSupport.getInstance());
        defaults.put(Byte.TYPE, ByteSupport.getInstance());
        defaults.put(Short.TYPE, ShortSupport.getInstance());
        defaults.put(Integer.TYPE, IntegerSupport.getInstance());
        defaults.put(Long.TYPE, LongSupport.getInstance());
        defaults.put(Float.TYPE, FloatSupport.getInstance());
        defaults.put(Double.TYPE, DoubleSupport.getInstance());
        defaults.put(Character.TYPE, CharacterSupport.getInstance());

        return Collections.unmodifiableMap(defaults);
    }

    private Map<Class, Class> initWrappers()
    {
        Map<Class, Class> wrappers = new HashMap<Class, Class>(8);

        wrappers.put(Boolean.TYPE, Boolean.class);
        wrappers.put(Byte.TYPE, Byte.class);
        wrappers.put(Short.TYPE, Short.class);
        wrappers.put(Integer.TYPE, Integer.class);
        wrappers.put(Long.TYPE, Long.class);
        wrappers.put(Float.TYPE, Float.class);
        wrappers.put(Double.TYPE, Double.class);
        wrappers.put(Character.TYPE, Character.class);

        return Collections.unmodifiableMap(wrappers);
    }

    private void validatePrimitive(String propertyName, Class propertyType,
            Object propertyValue) throws InvalidPropertyValueException
    {
        Class wrapperClass = wrappers.get(propertyType);

        if (!wrapperClass.isInstance(propertyValue))
        {
            String valueType = "";

            if (propertyValue != null)
            {
                valueType = String.format(" of type, %s,", propertyValue
                        .getClass().getName());
            }

            throw new InvalidPropertyValueException(
                    String
                            .format(
                                    "Value, %1$s,%2$s is not a valid value for property, %3$s, of type, %4$s.",
                                    propertyValue, valueType, propertyName,
                                    propertyType.getName()));
        }
    }

    private boolean isValidPrimitive(Class propertyType, Object propertyValue)
    {
        Class wrapperClass = wrappers.get(propertyType);

        if (null == wrapperClass)
        {
            LOGGER.warn("No wrapper for " + propertyType.getName() + ".");

            return false;
        }

        return wrapperClass.isInstance(propertyValue);
    }
}
