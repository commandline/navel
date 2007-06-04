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

import java.beans.BeanInfo;

import net.sf.navel.beans.InvalidPropertyValueException;
import net.sf.navel.beans.UnsupportedFeatureException;

/**
 * Describes the operations to validate any initial values based into different
 * types of InvocationHandlers' constructors.
 * 
 * @author thomas
 */
public interface Validator
{

    /**
     * Method for doing validation on construction. Must be support from
     * constructor to allow constructor to initialize everything, first.
     * 
     * @throws UnsupportedFeatureException
     *             Thrown if anything looks suspect.
     * @throws InvalidPropertyValueException
     *             Thrown if there is a problem with any of the initial values.
     */
    void validateAll() throws UnsupportedFeatureException,
            InvalidPropertyValueException;

    /**
     * Ensure that for the proxied class, there are no events declared, since
     * PropertyBeanHandler doesn't support events.
     */
    void validateEvents(BeanInfo beanInfo) throws UnsupportedFeatureException;

    /**
     * Ensure that all methods for the proxied class are concerned with property
     * manipulation, since PropertyBeanHandler doesn't support any purely
     * behavioral methods.
     */
    void validateMethods(BeanInfo beanInfo) throws UnsupportedFeatureException;

    /**
     * Ensure that the map of initial values is valid per the properties
     * described by the proxied class.
     * 
     * @throws InvalidPropertyValueException
     *             Thrown in an initial value doesn't match up with the proxied
     *             class' properties, by name or type.
     */
    void validateData(BeanInfo beanInfo) throws InvalidPropertyValueException;

    /**
     * Any time the underlying map is changed directly through a forward, ensure
     * the changed Map is still valid for the Bean interface.
     * 
     * @throws InvalidPropertyValueException
     *             Thrown in an initial value doesn't match up with the proxied
     *             class' properties, by name or type.
     * @throws UnsupportedFeatureException
     *             If there are any problems with JavaBean introspection.
     */
    void revalidateData() throws InvalidPropertyValueException;
}