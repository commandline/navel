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

import net.sf.navel.example.CharacterAsString;
import net.sf.navel.example.CharacterAsStringDelegate;
import net.sf.navel.example.TypesBean;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test that property delegate works as desired.
 * 
 * @author cmdln
 * 
 */
public class PropertyDelegateTest
{

    @Test
    public void testStringView()
    {
        TypesBean typesBean = ProxyFactory.createAs(TypesBean.class,
                CharacterAsString.class);

        CharacterAsString stringBean = (CharacterAsString) typesBean;

        stringBean.setCharacterString("foo");

        Assert.assertEquals(stringBean.getCharacterString(), "foo",
                "Should work with default behavior.");

        ProxyFactory.attach(typesBean, "characterString",
                new CharacterAsStringDelegate());

        Assert.assertTrue(
                ProxyFactory.isAttached(typesBean, "characterString"),
                "Should spot the property delegate.");

        typesBean.setCharacter('a');

        Assert.assertEquals(stringBean.getCharacterString(), "a",
                "Should have gotten the correct view.");

        stringBean.setCharacterString("b");

        Assert.assertEquals(typesBean.getCharacter(), 'b',
                "Updating the view should have updated the live value.");

        ProxyFactory.detach(typesBean, "characterString");

        JavaBeanHandler handler = ProxyFactory.getHandler(typesBean);

        Assert.assertFalse(ProxyFactory
                .isAttached(typesBean, "characterString"),
                "Should not longer have the property delegate.");

        Assert.assertEquals(stringBean.getCharacterString(), "foo",
                "Detaching should restore original behavior.");

        Assert.assertEquals(typesBean.getCharacter(), 'b',
                "Live value should be permanently changed.");

        Assert.assertEquals(handler.propertyValues.copyValues().size(), 2,
                "Should have two entries.");
    }
}
