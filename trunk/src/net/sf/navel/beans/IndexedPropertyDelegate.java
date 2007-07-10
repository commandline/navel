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

/**
 * This delegate is to PropertyDelegate as IndexedPropertyDescriptor in the
 * JavaBeans API is to PropertyDescriptor.
 * 
 * @author cmdln
 * @param C
 *            Component type of the array.
 * @param T
 *            Type of the array itself, such as {Object[].class}.
 * 
 */
public interface IndexedPropertyDelegate<C, T> extends PropertyDelegate<T>
{

    /**
     * Used to ensure that the parameterization of the component type of the
     * indexed property won't cause problems for the PropertyHandler.
     * 
     * @return Should match the component type of the T parameters, which will
     *         be checked to ensure it is an array.
     */
    Class<C> componentType();

    /**
     * Invoked by the PropertyHandler when a read is performed against a
     * specified indexed property, alternate that returns the element at the
     * specified index, rather than the entire array.
     * 
     * @param values
     *            Gives the delegate access to the internal state of the bean.
     * @param propertyName
     *            Discriminates which property is being accessed, so a single
     *            delegate can intelligently be re-used.
     * @param index
     *            Element to manipulate.
     * @return Must match the property type or will cause an
     *         InvocationTargetException.
     */
    C get(PropertyValues values, String propertyName, int index);

    /**
     * Invoked by the PropertyHandler when a write is performed against a
     * specified indexed property, alternate that sets the element at the
     * specified index, rather than the entire array.
     * 
     * @param values
     *            Gives the delegate access to the internal state of the bean.
     * @param propertyName
     *            Discriminates which property is being accessed, so a single
     *            delegate can intelligently be re-used.
     * @param index
     *            Element to manipulate.
     * @return value Must match the property type or will cause an
     *         InvalidPropertyValueException.
     */
    void set(PropertyValues values, String propertyName, int index, C value);
}
