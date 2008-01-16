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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.navel.example.TypesBean;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author cmdln
 *
 */
public class PropertyValuesExpanderTest
{

    @Test
    public void testList()
    {
        List<TypesBean> nestedList = new ArrayList<TypesBean>(3);
        TypesBean types = ProxyFactory.createAs(TypesBean.class);
        types.setBoolean(true);
        
        nestedList.add(types);
        nestedList.add(null);
        nestedList.add(ProxyFactory.copyAs(TypesBean.class, types));
        
        Map<String,Object> original = new HashMap<String, Object>(2);
        
        original.put("types", ProxyFactory.copy(types, true));
        original.put("nestedList", nestedList);
        
        Map<String,Object> toExpand = new HashMap<String, Object>(original);
        
        Map<String, PropertyValues> nestedProxies = new HashMap<String, PropertyValues>(1);
        nestedProxies.put("types", ProxyFactory.getHandler(types).propertyValues);
        
        PropertyValuesExpander.expand(nestedProxies, toExpand);
        
        Assert.assertEquals(toExpand.size(), 3, "Should have expanded List correctly.");
        Assert.assertTrue(toExpand.containsKey("types.boolean"));
        Assert.assertTrue(toExpand.containsKey("nestedList[0].boolean"));
        Assert.assertTrue(toExpand.containsKey("nestedList[2].boolean"));
        
        original.remove("nestedList");
        original.put("nestedArray", nestedList.toArray(new TypesBean[3]));
        
        toExpand = new HashMap<String, Object>(original);
        
        PropertyValuesExpander.expand(nestedProxies, toExpand);
        
        Assert.assertEquals(toExpand.size(), 3, "Should have expanded array correctly.");
        Assert.assertTrue(toExpand.containsKey("types.boolean"));
        Assert.assertTrue(toExpand.containsKey("nestedArray[0].boolean"));
        Assert.assertTrue(toExpand.containsKey("nestedArray[2].boolean"));
    }
}
