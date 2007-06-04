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
 * Generic interface for manipulating a primitive array.  Use anonymous
 * implementations of this interface to work on each of the eight specific
 * primitive types. This interface is only used internally by its outer
 * class to polymorphically manipulate primitive arrays based on reflection.
 *
 * @author cmdln
 * @version $Revision: 1.1 $, $Date: 2003/10/21 16:49:50 $
 */
public interface ArrayManipulator
{
    /**
     * Gets the primitive value at the specified index and returns it enclosed
     * in the appropriate wrapper type.
     *
     * @param array Primitive array to access, will need to cast appropriately.
     * @param index Index of the desired element.
     * @return A wrapped primitive.
     */
    Object getElement(Object array, int index);

    /**
     * Sets a primitive into the specified index of a primitive array, unboxing
     * the value argument to its primitive equivalent.
     *
     * @param array Primitive array to update.
     * @param index Index to set.
     * @param value Wrapper containing the primitive to set.
     */
    void setElement(Object array, int index, Object value);
}
