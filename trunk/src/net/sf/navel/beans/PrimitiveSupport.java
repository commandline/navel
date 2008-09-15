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

import org.apache.log4j.Logger;

/**
 * A utility class for dealing with properties of primitive types, including
 * arrays of primitives, which can be very tricky to reflect correctly.
 * 
 * @author cmdln
 */
public enum PrimitiveSupport
{

    BOOLEAN(Boolean.FALSE, Boolean.class, boolean.class),

    BYTE(Byte.valueOf((byte) 0), Byte.class, byte.class),

    CHARACTER(Character.valueOf('\u0000'), Character.class, char.class),

    DOUBLE(Double.valueOf(0.0d), Double.class, double.class),

    FLOAT(Float.valueOf((float) 0.0), Float.class, float.class),

    INTEGER(Integer.valueOf(0), Integer.class, int.class),

    LONG(Long.valueOf(0L), Long.class, long.class),

    SHORT(Short.valueOf((short) 0), Short.class, short.class);

    private static final Logger LOGGER = Logger
            .getLogger(PrimitiveSupport.class);

    private static final Map<Class<?>, PrimitiveSupport> byPrimitives = initPrimitives();

    private static final Map<Class<?>, PrimitiveSupport> byWrappers = initWrappers();

    private final Class<?> wrapperType;

    private final Class<?> primitiveType;

    private final Object defaultValue;

    private PrimitiveSupport(Object defaultValue, Class<?> wrapperType,
            Class<?> primitiveType)
    {
        this.defaultValue = defaultValue;
        this.wrapperType = wrapperType;
        this.primitiveType = primitiveType;
    }

    /**
     * Lookup for when the caller has the primitive type and needs access to the
     * members of the associated enum.
     * 
     * @param primitiveType
     *            Must be non-null and the class for a primitive type.
     * @return May be null if there is no associated enum value.
     */
    public static PrimitiveSupport getForPrimitive(Class<?> primitiveType)
    {
        if (null == primitiveType)
        {
            throw new IllegalArgumentException("Type to check must be valid!");
        }

        if (!primitiveType.isPrimitive())
        {
            throw new IllegalArgumentException(String.format(
                    "The type, %1$s, is not a primitive type.", primitiveType
                            .getName()));
        }

        return byPrimitives.get(primitiveType);
    }

    /**
     * @return The primitive type member for the association between auto-box
     *         types.
     */
    public Class<?> getPrimitiveType()
    {
        return primitiveType;
    }

    /**
     * @return The wrapper type member for the association between auto-box
     *         types.
     */
    public Class<?> getWrapperType()
    {
        return wrapperType;
    }

    /**
     * @return Value to use for the uninitialized value of the primitive type
     *         member.
     */
    public Object getDefaultValue()
    {
        return defaultValue;
    }

    /**
     * Lookup for when the caller has the wrapper type and needs access to the
     * members of the associated enum.
     * 
     * @param warpperType
     *            May be any type but if not a valid primitive wrapper, null
     *            will be returned.
     * @return May be null if there is no associated enum value.
     */
    public static PrimitiveSupport getForWrapper(Class<?> wrapperType)
    {
        return byWrappers.get(wrapperType);
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
    static void validate(String propertyName, Class<?> propertyType,
            Object propertyValue) throws InvalidPropertyValueException
    {
        assert propertyType != null : "Cannot check null property type.";

        Class<?> wrapperClass = byPrimitives.get(propertyType).wrapperType;

        if (wrapperClass.isInstance(propertyValue))
        {
            return;
        }

        String valueType = "";

        if (propertyValue != null)
        {
            valueType = String.format(" of type, %s,", propertyValue.getClass()
                    .getName());
        }

        throw new InvalidPropertyValueException(
                String
                        .format(
                                "Value, %1$s,%2$s is not a valid value for property, %3$s, of type, %4$s.",
                                propertyValue, valueType, propertyName,
                                propertyType.getName()));
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
    static boolean isValid(Class<?> propertyType, Object propertyValue)
    {
        assert propertyType != null : "Cannot check null property type.";

        Class<?> wrapperClass = byPrimitives.get(propertyType).wrapperType;

        if (null == wrapperClass)
        {
            LOGGER.warn(String.format("No wrapper for ,%1$s.", propertyType
                    .getName()));

            return false;
        }

        return wrapperClass.isInstance(propertyValue);
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
    static Object handleNull(Class<?> returnType, Object value)
    {
        assert returnType != null : "Cannot handle null return type.";

        if (!returnType.isPrimitive() || (null != value))
        {
            return value;
        }

        PrimitiveSupport defaultPrimitive = byPrimitives.get(returnType);

        if (null == defaultPrimitive)
        {
            LOGGER.warn(String.format(
                    "Could not find DefaultPrimitive for type, %1$s.",
                    returnType.getName()));

            return value;
        }

        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace(String.format("Handling null for type, %1$s.",
                    returnType.getName()));
            LOGGER.trace("Found DefaultPrimitive "
                    + defaultPrimitive.getClass().getName());
        }

        return defaultPrimitive.defaultValue;
    }

    private static Map<Class<?>, PrimitiveSupport> initPrimitives()
    {
        Map<Class<?>, PrimitiveSupport> temp = new HashMap<Class<?>, PrimitiveSupport>(
                PrimitiveSupport.values().length);

        for (PrimitiveSupport support : PrimitiveSupport.values())
        {
            temp.put(support.primitiveType, support);
        }

        return Collections.unmodifiableMap(temp);
    }

    private static Map<Class<?>, PrimitiveSupport> initWrappers()
    {
        Map<Class<?>, PrimitiveSupport> temp = new HashMap<Class<?>, PrimitiveSupport>(
                PrimitiveSupport.values().length);

        for (PrimitiveSupport support : PrimitiveSupport.values())
        {
            temp.put(support.wrapperType, support);
        }

        return Collections.unmodifiableMap(temp);
    }
}
