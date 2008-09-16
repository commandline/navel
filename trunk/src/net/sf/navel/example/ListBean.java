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

import java.beans.IndexedPropertyDescriptor;
import java.beans.PropertyDescriptor;
import java.util.List;

import net.sf.navel.beans.CollectionType;

/**
 * @author thomas
 * 
 */
public interface ListBean
{

    public long getListID();

    public void setListID(long listID);

    public boolean getBoolean();

    public void setBoolean(boolean value);

    /**
     * Introspection won't see this but given how indexed gets/sets are handled
     * under the hood, this should work statically.
     */
    public List<TypesBean> getCollection();

    /**
     * This method should introspect into an {@link IndexedPropertyDescriptor}
     * that returns null for {@link IndexedPropertyDescriptor#getPropertyType()}.
     */
    public TypesBean getCollection(int index);

    /**
     * This method should introspect into an {@link IndexedPropertyDescriptor}
     * that returns null for {@link IndexedPropertyDescriptor#getPropertyType()}.
     */
    public void setCollection(int index, TypesBean typeBean);

    /**
     * This method is necessary to provide a plain type tp
     * {@link PropertyDescriptor#getPropertyType()}.
     */
    public TypesBean[] getArray();

    /**
     * This method should introspect into an {@link IndexedPropertyDescriptor}
     * that returns an array type for
     * {@link IndexedPropertyDescriptor#getPropertyType()}.
     */
    public void setArray(TypesBean[] array);

    /**
     * This method should introspect into an {@link IndexedPropertyDescriptor}
     * that returns an array type for
     * {@link IndexedPropertyDescriptor#getPropertyType()}.
     */
    public TypesBean getArray(int index);

    public void setArray(int index, TypesBean array);
    
    public List<TypesBean> getUnannotated();
    
    public void setUnannotated(List<TypesBean> list);

    /**
     * In the absence of indexed methods that indicate a specific element type,
     * this annotation is required to make Collections transparent.
     */
    @CollectionType(TypesBean.class)
    public List<TypesBean> getAnnotated();
    
    public void setAnnotated(List<TypesBean> list);
}
