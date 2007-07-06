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

import net.sf.navel.example.IndexedBean;
import net.sf.navel.example.TypesBean;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * 
 * @author cmdln
 * 
 */
public class PropertyManipulatorTest
{

    @Test
    public void testIndexedProperty()
    {
        IndexedBean indexedBean = ProxyFactory.createAs(IndexedBean.class);

        TypesBean typesBean = ProxyFactory.createAs(TypesBean.class);

        indexedBean.setArray(new String[2]);
        indexedBean.setFloats(new float[2]);
        indexedBean.setTypes(new TypesBean[2]);

        indexedBean.setTypes(0, typesBean);

        PropertyManipulator.put(indexedBean, "array[0]", "foo");
        PropertyManipulator.put(indexedBean, "array[1]", "bar");
        PropertyManipulator.put(indexedBean, "floats[0]", new Float(32.0));
        PropertyManipulator.put(indexedBean, "floats[1]", new Float(64.0));
        PropertyManipulator.put(indexedBean, "types[0].boolean", Boolean.TRUE);
        PropertyManipulator.put(indexedBean, "types[1].boolean", Boolean.FALSE);

        Assert.assertEquals(indexedBean.getArray(0), "foo",
                "First String element should be set correctly.");
        Assert.assertEquals(indexedBean.getArray(1), "bar",
                "Second String element should be set correctly.");

        Assert.assertEquals(indexedBean.getFloats(0), 32.0, 0,
                "First float element should be set correctly.");
        Assert.assertEquals(indexedBean.getFloats(1), 64.0, 0,
                "Second float element should be set correctly.");

        Assert
                .assertEquals(indexedBean.getTypes(0).getBoolean(), true,
                        "Boolean property of first types element should be set correctly.");
        Assert
                .assertEquals(indexedBean.getTypes(1).getBoolean(), false,
                        "Boolean property of second types element should be set correctly.");
    }
}
