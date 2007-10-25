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

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Map;

/**
 * Instances of this interface may be registered for types of interest and
 * whenever those types are included in the total set for a new Navel bean, any
 * matching delegate instances will be called before and after construction.
 * 
 * @author thomas
 * 
 */
public interface ConstructionDelegate
{

    /**
     * Give callers a chance to hook into the {@link ProxyFactory} for purposes
     * of conditionally augmenting the interfaces any given dynamic proxy will
     * implement based on information made available declaratively through the
     * create call.
     * 
     * @param nestingDepth
     *            For directly or indirectly recursive relationships between
     *            types, this argument lets the delegate consider nesting depth
     *            as part of its criteria as to whether it should do anything
     *            special.
     * @param primaryType
     *            The primary type for the {@link Proxy} about to be created,
     *            may or may not match the thisType argument.
     * @param thisType
     *            This is the type that triggered the call into the delegate,
     *            specifically.
     * @param allTypes
     *            All of the original types requested, in case the code that
     *            initially invoked create already included an interface of
     *            interest.
     * @param initialValues
     *            May be empty, will never be null; allows type augmentation
     *            based on the initial values for a new {@link JavaBeanHandler}.
     *            The value based in will be immutable as this method is not
     *            meant to alter the state of the bean under construction.
     * @return Any additional interfaces that the {@link Proxy} should implement
     *         or null if no additional interfaces should be added. Any
     *         duplicates from other {@link ConstructionDelegate} instances
     *         invoked before {@link Proxy} creation will be eliminated.
     */
    Collection<Class<?>> additionalTypes(int nestingDepth, Class<?> thisType,
            Class<?> primaryType, Class<?>[] allTypes,
            Map<String, Object> initialValues);

    /**
     * This code will get invoked any any new Navel been that implements the
     * type for which this instance is registered immediately after the
     * {@link Proxy} is created but before the reference is returned out of
     * {@link ProxyFactory} making this suitable to do custom "construction"
     * work.
     * 
     * @param nestingDepth
     *            For directly or indirectly recursive relationships between
     *            types, this argument lets the delegate consider nesting depth
     *            as part of its criteria as to whether it should do anything
     *            special.
     * @param thisType
     *            This is the type that triggered the call into the delegate,
     *            specifically. Navel will guarantee that it is safe to cast the
     *            bean argument to this type.
     * @param bean
     *            Dynamic {@link Proxy} that was just created, provided so that
     *            its state may be initialized as desired.
     */
    void init(int nestingDepth, Class<?> thisType, Object bean);

}
