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

import net.sf.navel.example.Delegated;
import net.sf.navel.example.DelegatedBean;
import net.sf.navel.example.DelegatedImpl;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author thomas
 *
 */
public class ObjectProxyTest
{

    @Test
    public void testEquals()
    {
        DelegatedBean left = ProxyFactory.createAs(DelegatedBean.class, Delegated.class);
        
        left.setReadWrite(17);
        
        DelegatedBean right = ProxyFactory.createAs(DelegatedBean.class, Delegated.class);
        
        right.setReadWrite(23);
        
        Assert.assertTrue(left.equals(left));
        Assert.assertFalse(left.equals(right));
        
        right.setReadWrite(17);
        Assert.assertTrue(left.equals(right));
        
        ProxyFactory.attach(right, new DelegatedImpl());
        
        Assert.assertTrue(left.equals(right));
        
        right = ProxyFactory.createAs(DelegatedBean.class);
        
        Assert.assertFalse(left.equals(right));
    }
    
    @Test
    public void testHashCode()
    {
        DelegatedBean left = ProxyFactory.createAs(DelegatedBean.class, Delegated.class);
        
        left.setReadWrite(17);
        
        DelegatedBean right = ProxyFactory.createAs(DelegatedBean.class, Delegated.class);
        
        right.setReadWrite(23);
        
        Assert.assertEquals(left.hashCode(), left.hashCode());
        Assert.assertFalse(left.hashCode() == right.hashCode());
        
        right.setReadWrite(17);
        Assert.assertEquals(left.hashCode(), right.hashCode());
        
        ProxyFactory.attach(right, new DelegatedImpl());
        
        Assert.assertEquals(left.hashCode(), right.hashCode());
        
        right = ProxyFactory.createAs(DelegatedBean.class);
        
        Assert.assertFalse(left.hashCode() == right.hashCode());
    }
}
