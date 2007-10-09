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

import java.util.Collections;
import java.util.Map;

import net.sf.navel.beans.PropertyDelegate;
import net.sf.navel.beans.PropertyValues;

/**
 * @author thomas
 *
 */
public class MapPropertyDelegate implements
        PropertyDelegate<Map<String, Object>>
{

    /**
     * 
     */
    private static final long serialVersionUID = 1568363339560938339L;

    /**
     * @see net.sf.navel.beans.PropertyDelegate#get(net.sf.navel.beans.PropertyValues, java.lang.String)
     */

    public Map<String, Object> get(PropertyValues values, String propertyName)
    {
        return values.copyValues(true);
    }

    /**
     * @see net.sf.navel.beans.PropertyDelegate#propertyType()
     */

    @SuppressWarnings("unchecked")
    public Class<Map<String, Object>> propertyType()
    {
        return (Class<Map<String, Object>>) Collections.EMPTY_MAP.getClass();
    }

    /**
     * @see net.sf.navel.beans.PropertyDelegate#set(net.sf.navel.beans.PropertyValues, java.lang.String, java.lang.Object)
     */

    public void set(PropertyValues values, String propertyName,
            Map<String, Object> value)
    {
        values.putAll(value);
    }

}
