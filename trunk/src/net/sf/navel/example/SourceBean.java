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


public class SourceBean implements Serializable
{
    private static final long serialVersionUID = -536114975583823000L;
    
    private byte foo;
    private short bar;
    private int baz;
    private float quux;
    private String[] array;
    private IndexedBean indexed;

    public byte getFoo()
    {
        return foo;
    }

    public void setFoo(byte value)
    {
        foo = value;
    }

    public short getBar()
    {
        return bar;
    }

    public void setBar(short value)
    {
        bar = value;
    }

    public int getBaz()
    {
        return baz;
    }

    public void setBaz(int value)
    {
        baz = value;
    }

    public float getQuux()
    {
        return quux;
    }

    public void setQuux(float value)
    {
        quux = value;
    }
    
    public String[] getArray()
    {
        return array;
    }
    
    public String getArray(int index)
    {
        return array[index];
    }
    
    public void setArray(String[] array)
    {
        this.array = array;
    }
    
    public void setArray(int index, String element)
    {
        this.array[index] = element;
    }
    
    public IndexedBean getIndexed()
    {
        return indexed;
    }
    
    public void setIndexed(IndexedBean indexed)
    {
        this.indexed = indexed;
    }
}
