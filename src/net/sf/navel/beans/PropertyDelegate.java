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

import java.io.Serializable;

/**
 * Interface for providing handling on a property by property basis. Allows
 * overriding of the default behavior and provides access to the PropertyValues
 * component within the proxy to read and manipulate the bean's internal state.
 * 
 * Since the get and set calls provide direct access to the internal storage of
 * the delegating bean, implementers of this interface should be stateless. This
 * is also required to work safely with the copy methods on {@link ProxyFactory}.
 * 
 * @author cmdln
 * 
 */
public interface PropertyDelegate<T> extends Serializable
{

    /**
     * For constraint checking so that the parameterized type can be ensured to
     * work with the property to which this may be attached.
     */
    Class<T> propertyType();

    /**
     * Invoked by the PropertyHandler when a read is performed against a
     * specified property.
     * 
     * @param values
     *            Gives the delegate access to the internal state of the bean.
     * @param propertyName
     *            Discriminates which property is being accessed, so a single
     *            delegate can intelligently be re-used.
     * @return Must match the property type or will cause an
     *         InvocationTargetException.
     */
    T get(PropertyValues values, String propertyName);

    /**
     * Invoked by the PropertyHandler when a write is performed against a
     * specified property.
     * 
     * @param values
     *            Gives the delegate access to the internal state of the bean.
     * @param propertyName
     *            Discriminates which property is being accessed, so a single
     *            delegate can intelligently be re-used.
     * @return value Must match the property type or will cause an
     *         InvalidPropertyValueException.
     */
    void set(PropertyValues values, String propertyName, T value);
}
