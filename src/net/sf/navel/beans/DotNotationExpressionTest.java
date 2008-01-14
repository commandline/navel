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

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author cmdln
 * 
 */
public class DotNotationExpressionTest
{

    @Test
    public void testParse()
    {
        DotNotationExpression dotExpression = new DotNotationExpression(
                "foo.bar.baz");

        Assert.assertEquals(dotExpression.getDepth(), 3, String.format(
                "Expression, %1$s, should have parsed to the correct depth.",
                dotExpression.getExpression()));

        PropertyExpression propertyExpression = dotExpression.getRoot();

        Assert.assertEquals(propertyExpression.getPropertyName(), "foo");
        Assert.assertTrue(propertyExpression.isRoot(),
                "Foo should be the root property.");
        Assert.assertFalse(propertyExpression.isLeaf(),
                "Foo should not be a leaf property.");
        Assert.assertEquals(propertyExpression.expressionToLeaf(),
                "foo.bar.baz");
        Assert.assertEquals(propertyExpression.expressionToRoot(), "foo");

        propertyExpression = propertyExpression.getChild();

        Assert.assertEquals(propertyExpression.getPropertyName(), "bar");
        Assert.assertFalse(propertyExpression.isRoot(),
                "Bar should not be the root property.");
        Assert.assertFalse(propertyExpression.isLeaf(),
                "Bar should not be a leaf property.");
        Assert.assertSame(propertyExpression.getParent(), dotExpression
                .getRoot(),
                "Bar's parent should be the root of the dot expression.");
        Assert.assertEquals(propertyExpression.expressionToLeaf(), "bar.baz");
        Assert.assertEquals(propertyExpression.expressionToRoot(), "foo.bar");

        propertyExpression = propertyExpression.getChild();

        Assert.assertEquals(propertyExpression.getPropertyName(), "baz");
        Assert.assertFalse(propertyExpression.isRoot(),
                "Baz should not be the root property.");
        Assert.assertTrue(propertyExpression.isLeaf(),
                "Baz should be a leaf property.");
        Assert.assertNotSame(propertyExpression.getParent(), dotExpression
                .getRoot(),
                "Baz's parent should not be the root of the dot expression.");
        Assert.assertEquals(propertyExpression.expressionToLeaf(), "baz");
        Assert.assertEquals(propertyExpression.expressionToRoot(),
                "foo.bar.baz");
    }

    @Test
    public void testParseSingle()
    {
        DotNotationExpression dotExpression = new DotNotationExpression("foo");

        Assert.assertEquals(dotExpression.getDepth(), 1, String.format(
                "Expression, %1$s, should have parsed to the correct depth.",
                dotExpression.getExpression()));

        PropertyExpression propertyExpression = dotExpression.getRoot();

        Assert.assertEquals(propertyExpression.getPropertyName(), "foo");
        Assert.assertTrue(propertyExpression.isRoot(),
                "Foo should be the root property.");
        Assert.assertTrue(propertyExpression.isLeaf(),
                "Foo should be a leaf property.");
    }

    @Test
    public void testParseIndexed()
    {
        DotNotationExpression dotExpression = new DotNotationExpression(
                "foo.bar[].baz[1]");

        Assert.assertEquals(dotExpression.getDepth(), 3, String.format(
                "Expression, %1$s, should have parsed to the correct depth.",
                dotExpression.getExpression(), 3));

        PropertyExpression propertyExpression = dotExpression.getRoot();

        Assert.assertEquals(propertyExpression.getPropertyName(), "foo");
        Assert.assertTrue(propertyExpression.isRoot(),
                "Foo should be the root property.");
        Assert.assertFalse(propertyExpression.isLeaf(),
                "Foo should not be a leaf property.");
        Assert.assertFalse(propertyExpression.isIndexed(),
                "Foo should not be indexed.");

        propertyExpression = propertyExpression.getChild();

        Assert.assertEquals(propertyExpression.getPropertyName(), "bar");
        Assert.assertFalse(propertyExpression.isRoot(),
                "Bar should not be the root property.");
        Assert.assertFalse(propertyExpression.isLeaf(),
                "Bar should not be a leaf property.");
        Assert.assertSame(propertyExpression.getParent(), dotExpression
                .getRoot(),
                "Bar's parent should be the root of the dot expression.");
        Assert.assertTrue(propertyExpression.isIndexed(),
                "Bar should be indexed.");
        Assert.assertFalse(propertyExpression.hasIndex(),
                "Bar should not have a valid index value.");

        propertyExpression = propertyExpression.getChild();

        Assert.assertEquals(propertyExpression.getPropertyName(), "baz");
        Assert.assertFalse(propertyExpression.isRoot(),
                "Baz should not be the root property.");
        Assert.assertTrue(propertyExpression.isLeaf(),
                "Baz should be a leaf property.");
        Assert.assertNotSame(propertyExpression.getParent(), dotExpression
                .getRoot(),
                "Baz's parent should not be the root of the dot expression.");
        Assert.assertTrue(propertyExpression.isIndexed(),
                "Baz should be indexed.");
        Assert.assertEquals(propertyExpression.getIndex(), 1,
                "Baz should have a valid index value.");
    }

    @Test
    public void testGetLeaf()
    {
        DotNotationExpression dotExpression = new DotNotationExpression(
                "foo.bar.baz");

        Assert.assertNotNull(dotExpression.getLeaf(), "Leaf should be valid.");
        Assert.assertEquals(dotExpression.getLeaf().getPropertyName(), "baz");

        Assert.assertEquals(dotExpression.getLeaf().expressionToRoot(),
                "foo.bar.baz",
                "Expression to root should match the original expression.");

        dotExpression = new DotNotationExpression("foo");

        Assert.assertNotNull(dotExpression.getLeaf(),
                "Single property Leaf should be valid.");
        Assert.assertTrue(dotExpression.getLeaf().isRoot(),
                "Leaf should also be correctly flagged as root expression.");
        Assert.assertEquals(dotExpression.getLeaf().getPropertyName(),
                dotExpression.getRoot().getPropertyName(),
                "Leaf and root should be equal.");
    }
}
