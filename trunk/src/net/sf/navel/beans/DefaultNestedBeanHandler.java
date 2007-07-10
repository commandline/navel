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

import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Encapsulates the stock behavior of creating a new proxy for nested properties
 * that are typed as interfaces or returning null, otherwise. External
 * implementers of NestedBeanHandler may either extend or compose this
 * implementation on order to re-use and augment it.
 * 
 * @author cmdln
 * 
 */
public class DefaultNestedBeanHandler implements NestedBeanHandler
{

    private static final Logger LOGGER = LogManager
            .getLogger(DefaultNestedBeanHandler.class);

    /**
     * @see net.sf.navel.beans.NestedBeanHandler#create(java.lang.String,
     *      java.lang.Class, java.util.Map)
     */
    public Object create(String propertyName, Class<?> propertyType,
            Map<String, Object> initialValues)
    {
        if (!propertyType.isInterface())
        {
            LOGGER
                    .warn(String
                            .format(
                                    "Nested property, %1$s, must currently be an interface to allow automatic instantiation.  Was of type, %2$s.",
                                    propertyName, propertyType.getName()));

            return null;
        }

        return ProxyFactory.createAs(propertyType, initialValues);
    }

}
