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
 * When resolving values to populate or create Navel beans, this factory is used
 * in instantiating nested properties that are not of primitive types. The
 * default behavior is to return null for properties that are not of interface
 * types and cannot be proxied by Navel. Callers may provider alternate
 * behaviors to obliquely created nested JavaBeans of any variety or to enforce
 * additional construction requirements even for Navel beans.
 * 
 * @author cmdln
 * 
 */
public class NestedBeanFactory
{

    private static final Logger LOGGER = LogManager
            .getLogger(NestedBeanFactory.class);

    private static final NestedBeanFactory SINGLETON = new NestedBeanFactory();

    private NestedBeanHandler handler = new DefaultNestedBeanHandler();

    /**
     * Register a custom handler, will affect all calls to the BeanManipulator,
     * PropertyManipulator and PropertyValues classes that may need to resolve
     * and lazily instantiate nested bean properties.
     * 
     * @param handler
     *            Custom handler that encapsulates external factory behavior.
     */
    public static void register(NestedBeanHandler handler)
    {
        if (null == handler)
        {
            throw new IllegalArgumentException(
                    "Cannot register a null handler!");
        }

        if (LOGGER.isInfoEnabled())
        {
            LOGGER.info(String.format(
                    "Registering new NestedBeanHandler, %1$s.", handler
                            .getClass().getName()));
        }
        SINGLETON.handler = handler;
    }

    /**
     * Utilizes the registered handler to lazily create beans for nested
     * properties as needed. See the class comment for the default behavior
     * provided with Navel.
     * 
     * @param propertyName
     *            Allows the internal handler context for differential creation
     *            logic.
     * @param propertyType
     *            Required at a minimum for default proxy construction.
     * @return May return null if the handler does not know what to do,
     *         otherwise should be a fully constructed object ready to be added
     *         into a PropertyValues instance and used immediately.
     */
    static Object create(String propertyName, Class<?> propertyType)
    {
        return create(propertyName, propertyType, null);
    }

    /**
     * Utilizes the registered handler to lazily create beans for nested
     * properties as needed. See the class comment for the default behavior
     * provided with Navel.
     * 
     * @param propertyName
     *            This is the name of the property whose value is being created.
     *            Allows the internal handler context for differential creation
     *            logic.
     * @param propertyType
     *            Required at a minimum for default proxy construction.
     * @param initialValues
     *            May be null, if provided, the NestedBeanHandler should
     *            populate the create object accordingly.
     * @return May return null if the handler does not know what to do,
     *         otherwise should be a fully constructed object ready to be added
     *         into a PropertyValues instance and used immediately.
     */
    static Object create(String propertyName, Class<?> propertyType,
            Map<String, Object> initialValues)
    {
        return SINGLETON.handler.create(propertyName, propertyType,
                initialValues);
    }
}
