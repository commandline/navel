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
package net.sf.navel.example;

import java.io.Serializable;


/**
 * This interface describes a bean with no properties, so we can test that
 * PropertyBeanHandler correctly errors out on construction with this.  Unit
 * tests have proven that the BeanInfo generated for this interface contains no
 * PropertyDescriptors, for the reasons indicated in the method comments below.
 * There is a subtle error case that this interface does not represent, because
 * it isn't possible to have the PropertyBeanHandler guess that it is really an
 * error.  When read and write methods for the same property deal with different
 * data types, it will cause the generated BeanInfo to mis-identify the property
 * as read-only or write-only depending on which method it finds first--the
 * order of introspection seems to very from JVM to JVM and even over time in a
 * long running JVM, when the introspection cache is automatically flushed.  It
 * is always a good idea to be sure that read and write methods for the same
 * property either agree in type, or provide your own BeanInfo implementation
 * that reconciles them.
 *
 * @author cmdln
 */
public interface BadPropertyBean extends Serializable
{
    /**
     * Indexed property would have an int for the first argument.
     */
    void setFoo(String value1, String value2);

    /**
     * Even an indexed property read only takes one argument, an int.
     */
    int getFoo(String value1, String value2);

    /**
     * Writing properties should not have a return type.
     */
    String setBar(String value1);

    /**
     * Reading properties should always have a return type.
     */
    void getBar();

    /**
     * Is alternate cannot be used with any type other than boolean.
     */
    String isNotBooleanAlt();

    /**
     * Is alternate cannot be used with any type other than boolean.
     */
    Boolean isWrapperAlt();
}
