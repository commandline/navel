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

import java.beans.BeanInfo;
import java.beans.MethodDescriptor;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;

/**
 * Provides validation on construction support for the DelegateBeanHandler,
 * ensuring that all the rules that must be true for the handler to work are.
 * 
 * @author cmndln
 */
class InterfaceValidator
{

    private static final Logger LOGGER = Logger
            .getLogger(InterfaceValidator.class);

    private static final InterfaceValidator SINGLETON = new InterfaceValidator();

    private InterfaceValidator()
    {
        // enforce Singleton pattern
    }

    /**
     * This method ensures that the delegate provided has methods for each of
     * the methods found through introspection on the interface it is meant to
     * support.
     */
    public static void validate(InterfaceDelegate delegate)
    {
        SINGLETON.validateDelegate(delegate);
    }

    private void validateDelegate(InterfaceDelegate delegate)
    {
        if (null == delegate)
        {
            throw new IllegalArgumentException(
                    "A DelegationTarget cannot be null!");
        }

        Class<?> delegatingInterface = delegate.getDelegatingInterface();

        BeanInfo beanInfo = JavaBeanHandler.introspect(delegatingInterface);

        MethodDescriptor[] methodDescriptors = beanInfo.getMethodDescriptors();

        if (methodDescriptors.length == 0)
        {
            LOGGER
                    .warn(String
                            .format(
                                    "The delegating interface, %1$s, does not have any JavaBean methods.",
                                    delegatingInterface.getName()));

            return;
        }

        // IMPROVE add a short cut for delegates that implement the delegatingInterface
        Class<?> delegateClass = delegate.getClass();

        for (MethodDescriptor methodDescriptor : methodDescriptors)
        {
            Method method = methodDescriptor.getMethod();

            try
            {
                Method delegateMethod = delegateClass.getMethod(method
                        .getName(), method.getParameterTypes());

                if (!method.getReturnType().equals(
                        delegateMethod.getReturnType()))
                {
                    throw new InvalidDelegateException(
                            String
                                    .format(
                                            "Invalid method, %1$s, in InterfaceDelegate, %2$s.  Requires return type, %3$s, but found return type, %4$s.",
                                            delegateMethod.getName(),
                                            delegateClass.getName(), method
                                                    .getReturnType().getName(),
                                            delegateMethod.getReturnType()
                                                    .getName()));
                }
            }
            catch (SecurityException e)
            {
                throw new InvalidDelegateException(
                        String
                                .format(
                                        "Insufficient permissions to reflect delegate's class, %1$s.",
                                        delegateClass.getName()), e);
            }
            catch (NoSuchMethodException e)
            {
                throw new InvalidDelegateException(
                        String
                                .format(
                                        "The delegate, %1$s, does not provide the method, %2$s, required by the delegating interface, %3$s.",
                                        delegateClass.getName(), method
                                                .getName(), delegatingInterface
                                                .getName()), e);
            }
        }
    }
}