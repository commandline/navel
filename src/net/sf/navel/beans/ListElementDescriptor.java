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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parameter object that describes the dot notation found in the key for a
 * potential List element value in a raw Map.
 * 
 * @author cmdln
 * 
 */
class ListElementDescriptor
{

    private final String parentProperty;

    private final Integer index;

    private final String nestedProperty;

    private static final Pattern pattern = Pattern.compile("\\[.*\\]");

    /**
     * Uses a factory method to decouple the internals, which may need to be
     * optimized depending on the impact on heap usage.
     */
    static ListElementDescriptor describe(final String listElementExpression)
    {
        return new ListElementDescriptor(listElementExpression);
    }

    private ListElementDescriptor(final String listElementExpression)
    {
        Matcher matcher = pattern.matcher(listElementExpression);

        boolean found = matcher.find();

        // there is no bracket operator, return a single token that is the
        // original value
        if (!found)
        {
            parentProperty = listElementExpression;
            index = null;
            nestedProperty = null;

            return;
        }

        String property = listElementExpression.substring(0, matcher.start());
        String indexString = matcher.group();
        indexString = indexString.substring(1, indexString.length() - 1);

        parentProperty = property;
        index = parseIndex(property, indexString);

        // the bracket operator was found in a non-terminal position, there is a
        // nested property
        if (matcher.end() < listElementExpression.length())
        {
            nestedProperty = stripLeadingDot(listElementExpression
                    .substring(matcher.end() + 1));
        }
        else
        {
            nestedProperty = null;
        }
    }

    String getParentProperty()
    {
        return parentProperty;
    }

    boolean hasIndex()
    {
        return index != null;
    }

    Integer getIndex()
    {
        return index;
    }

    boolean hasNestedProperty()
    {
        return nestedProperty != null;
    }

    String getNestedProperty()
    {
        return nestedProperty;
    }

    private final Integer parseIndex(String propertyKey, String index)
    {
        if (index.trim().length() == 0)
        {
            return null;
        }

        try
        {
            return Integer.parseInt(index);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException(
                    String
                            .format(
                                    "Index value, %1$s, could not be parsed as a number for use with List property, %2$s.",
                                    index, propertyKey), e);
        }
    }

    private final String stripLeadingDot(String toTrim)
    {
        if (!toTrim.startsWith("."))
        {
            return toTrim;
        }

        return toTrim.substring(1);
    }
}
