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

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Encapsulates the parse tree generated from a dot-notation expression. This is
 * the chaining of coventional property names, per the JavaBeans spec, with dots
 * (.). It also supports the use of the bracket operator ([]) with or without an
 * enclosed index to represent expressions meant to de-reference elements in
 * {@link List} or array properties. This code is useful to all of the access
 * and manipulate logic that needs to consume an arbitrary expression and
 * reflect over or traverse a bean graph.
 * 
 * @author cmdln
 * 
 */
public final class DotNotationExpression
{

    private static final Logger LOGGER = LogManager
            .getLogger(DotNotationExpression.class);

    private static final String OPEN_BRACKET = "[";

    private static final String CLOSE_BRACKET = "]";

    private final String expression;

    private final PropertyExpression rootExpression;

    private final PropertyExpression leafExpression;

    private final int depth;

    public DotNotationExpression(String expression)
    {
        if (null == expression)
        {
            throw new InvalidExpressionException(
                    "Cannot parse a null expression.");
        }

        this.expression = expression;
        this.rootExpression = new PropertyExpression(this, null, expression);

        int countingDepth = 1;

        PropertyExpression leafCandidate = null;

        for (PropertyExpression currentProperty = rootExpression; !currentProperty
                .isLeaf(); currentProperty = currentProperty.getChild())
        {
            leafCandidate = currentProperty;

            countingDepth++;
        }

        // if the depth is one, the loop never executed but the assumption that
        // the root and leaf are the same is valid
        if (1 == countingDepth)
        {
            this.leafExpression = rootExpression;
        }
        else
        {
            // the loop above will not execute for the last step, the leaf node,
            // so
            // need to step one further if the candidate is set
            this.leafExpression = null == leafCandidate ? null : leafCandidate
                    .getChild();
        }

        this.depth = countingDepth;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (null == obj || !(obj instanceof DotNotationExpression))
        {
            return false;
        }

        DotNotationExpression otherNotation = (DotNotationExpression) obj;

        return expression.equals(otherNotation.expression);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        return expression.hashCode();
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return expression;
    }

    /**
     * @return The original expression that was parsed to build this instance.
     */
    public String getExpression()
    {
        return expression;
    }

    /**
     * @return The root expression on the set of linked expressions that
     *         describe how to traverse/evaluate the expression.
     */
    public PropertyExpression getRoot()
    {
        return rootExpression;
    }

    /**
     * @return The leaf expression on the set of linked expressions that
     *         describe how to traverse/evaluate the expression.
     */
    public PropertyExpression getLeaf()
    {
        return leafExpression;
    }

    /**
     * @return The total number of de-references, exception for indexing, that
     *         the expression represents.
     */
    public int getDepth()
    {
        return depth;
    }

    /**
     * Extract the index value, if present, from an expression if it has a
     * bracket operator.
     * 
     * @param propertyExpression
     *            Expression to parse.
     * @return Null if there is no bracket operator, the operator is empty, or
     *         the contained value is not numeric.
     */
    static Integer getIndex(String propertyExpression)
    {
        int braceStart = propertyExpression.indexOf(OPEN_BRACKET);
        int braceEnd = propertyExpression.indexOf(CLOSE_BRACKET);

        if ((-1 == braceStart) || (-1 == braceEnd) || (braceEnd <= braceStart))
        {
            LOGGER
                    .warn(String
                            .format(
                                    "One or both braces missing or invalid positioning for property expression, %1$s.",
                                    propertyExpression));

            return null;
        }

        String indexString = propertyExpression.substring(braceStart + 1,
                braceEnd);

        if (indexString.trim().length() == 0)
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER
                        .debug(String
                                .format(
                                        "Index value was not specified in property expression, %1$s.",
                                        propertyExpression));
            }

            return null;
        }

        try
        {
            Integer index = Integer.valueOf(indexString);

            // compare the parsed to the original in case there are any
            // trailing characters on the original
            if (index.toString().equals(indexString))
            {
                return index.intValue();
            }
            else
            {
                return null;
            }
        }
        catch (NumberFormatException e)
        {
            LOGGER
                    .warn(String
                            .format(
                                    "%1$s cannot be parsed as an int for property expression, %2$s.",
                                    indexString, propertyExpression));

            return null;
        }
    }
}
