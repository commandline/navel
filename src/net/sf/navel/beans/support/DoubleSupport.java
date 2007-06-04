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
package net.sf.navel.beans.support;

/**
 * This class implements several interfaces used by PrimitiveSupport for
 * handling primitives through introspection and reflection.  This class is
 * responsible for working with double primitives.
 */
public class DoubleSupport implements ArrayManipulator, DefaultPrimitive
{
    public static final Class ARRAY_TYPE = double[].class;

    private static final DoubleSupport SINGLETON = new DoubleSupport();
    private static final Double DEFAULT_VALUE = new Double(0.0d);

    private DoubleSupport()
    {
        // enforce Singleton pattern
    }

    public static final DoubleSupport getInstance()
    {
        return SINGLETON;
    }

    public Object getElement(Object array, int index)
    {
        double[] primitiveArray = (double[])array;

        return new Double(primitiveArray[index]);
    }

    public void setElement(Object array, int index, Object value)
    {
        double[] primitiveArray = (double[])array;
        Double wrappedValue = (Double)value;
        primitiveArray[index] = wrappedValue.doubleValue();
    }

    public Object getValue()
    {
        return DEFAULT_VALUE;
    }
}
