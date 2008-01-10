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
class DotNotationExpression
{

    private static final Logger LOGGER = LogManager
            .getLogger(DotNotationExpression.class);

    private static final String OPEN_BRACKET = "[";

    private static final String CLOSE_BRACKET = "]";

    private final String expression;

    private final PropertyExpression rootExpression;

    private final int depth;

    DotNotationExpression(String expression)
    {
        this.expression = expression;
        this.rootExpression = new PropertyExpression(this, null, expression);

        int countingDepth = 1;

        for (PropertyExpression currentProperty = rootExpression; !currentProperty
                .isLeaf(); currentProperty = currentProperty.getChild())
        {
            countingDepth++;
        }

        this.depth = countingDepth;
    }

    /**
     * @return The original expression that was parsed to build this instance.
     */
    String getExpression()
    {
        return expression;
    }

    /**
     * @return The root expression on the set of linked expressions that
     *         describe how to traverse/evaluate the expression.
     */
    PropertyExpression getRoot()
    {
        return rootExpression;
    }

    /**
     * @return The total number of de-references, exception for indexing, that
     *         the expression represents.
     */
    int getDepth()
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
                                    "%1$s cannot be parsed as an int for property expression, $2%s.",
                                    indexString, propertyExpression));

            return null;
        }
    }
}

/**
 * Individual properties that make up the parse tree. All instances that compose
 * and expression are linked together and maintain a reference to the total
 * expression.
 * 
 * @author cmdln
 * 
 */
class PropertyExpression
{

    private final DotNotationExpression fullExpression;

    private final String localExpression;

    private final String expressionToLeaf;

    private String expressionToRoot;

    private final PropertyExpression parent;

    private final PropertyExpression child;

    private final String propertyName;

    private final boolean isIndexed;

    private final Integer elementIndex;

    PropertyExpression(DotNotationExpression fullExpression,
            PropertyExpression parent, String toEvaluate)
    {
        this.expressionToLeaf = toEvaluate;
        this.fullExpression = fullExpression;

        int dotIndex = toEvaluate.indexOf('.');

        this.localExpression = -1 == dotIndex ? toEvaluate : toEvaluate
                .substring(0, dotIndex);

        if (-1 == dotIndex)
        {
            child = null;
        }
        else
        {
            String remainingExpression = toEvaluate.substring(dotIndex + 1);

            child = new PropertyExpression(fullExpression, this,
                    remainingExpression);
        }

        this.parent = parent;

        if (localExpression.endsWith("]") && localExpression.indexOf('[') != -1)
        {
            this.propertyName = localExpression.substring(0, toEvaluate
                    .indexOf('['));
            this.isIndexed = true;
            this.elementIndex = DotNotationExpression.getIndex(toEvaluate);
        }
        else
        {
            this.propertyName = localExpression;
            this.isIndexed = false;
            this.elementIndex = null;
        }
    }

    /**
     * @return The total expression of which this instance is a part.
     */
    DotNotationExpression getFullExpression()
    {
        return fullExpression;
    }

    /**
     * @return The part of the total expression this instance represents.
     */
    String getExpression()
    {
        return localExpression;
    }

    /**
     * @return True if this is the root property in the total expression.
     */
    boolean isRoot()
    {
        return null == parent;
    }

    /**
     * @return Null if {@link #isRoot()} returns true, otherwise, the parent
     *         property that needs to be de-referenced to get to this instance.
     */
    PropertyExpression getParent()
    {
        return parent;
    }

    /**
     * @return True if this is the leaf property in the total expression.
     */
    boolean isLeaf()
    {
        return null == child;
    }

    /**
     * @return Null if {@link #isChild()} returns true, otherwise, the child
     *         property that can be de-reference from this one.
     */
    PropertyExpression getChild()
    {
        return child;
    }

    /**
     * @return JavaBean complaint property name, the different between this and
     *         the result of {@link #getExpression()} is that the other method
     *         preserves the optional bracket operator and this will strip it.
     */
    String getPropertyName()
    {
        return propertyName;
    }

    /**
     * @return True if {@link #getExpression()} contains a bracket operator.
     */
    boolean isIndexed()
    {
        return isIndexed;
    }

    /**
     * @return True if the expression is indexed and has a parseable index
     *         value.
     */
    boolean hasIndex()
    {
        return null != elementIndex;
    }

    /**
     * @return -1 if {@link #isIndexed()} or {@link #hasIndex()} returns false
     *         or the bracket operator is empty, otherwise the number value in
     *         the bracket operator.
     */
    int getIndex()
    {
        if (null == elementIndex)
        {
            throw new IllegalStateException("Index is unset or invalid.");
        }

        return (int) elementIndex;
    }

    /**
     * @return The sub-expression to the leaf end, inclusive of this instance.
     */
    String expressionToLeaf()
    {
        return expressionToLeaf;
    }

    /**
     * @return The sub-expression to the root end, inclusive of this instance.
     */
    String expressionToRoot()
    {
        if (expressionToRoot != null)
        {
            return expressionToRoot;
        }

        StringBuilder buffer = new StringBuilder();

        for (PropertyExpression current = this; current != null; current = current.parent)
        {
            if (buffer.length() != 0)
            {
                buffer.insert(0, '.');
            }

            buffer.insert(0, current.getExpression());
        }

        this.expressionToRoot = buffer.toString();

        return expressionToRoot;
    }

    /**
     * When performing an indexed access based on this expression, if the index
     * value is invalid, throw an exception.
     */
    void validateIndex()
    {
        if (getIndex() != -1)
        {
            return;
        }

        throw new InvalidExpressionException(
                String
                        .format(
                                "The expression, %1$s, requires a valid index for the bracket operator.",
                                expressionToRoot()));
    }

    /**
     * When performing an indexed access based on this expression, if the index
     * value is invalid for the size, throw an exception.
     */
    void validateListBounds(int size)
    {
        if (getIndex() < size)
        {
            return;
        }

        throw new InvalidExpressionException(
                String
                        .format(
                                "The expression, %1$s, uses an index, %2$d, for the bracket operator that would fall out of bounds for the List of size, %3$d.",
                                expressionToRoot(), getIndex(), size));
    }

    /**
     * When performing an indexed access based on this expression, if the target
     * value is not an array, throw an exception.
     */
    void validateArray(Object value)
    {
        if (value.getClass().isArray())
        {
            return;
        }

        throw new InvalidExpressionException(
                String
                        .format(
                                "The expression, %1$s, requires a value of either a List or array type for the bracket operator.",
                                expressionToRoot()));
    }

    /**
     * When performing an indexed access based on this expression, if the index
     * value is invalid for the length, throw an exception.
     */
    void validateArrayBounds(int length)
    {
        if (getIndex() < length)
        {
            return;
        }

        throw new InvalidExpressionException(
                String
                        .format(
                                "The expression, %1$s, uses an index, %2$d, for the bracket operator that would fall out of bounds for the array of length, %3$d.",
                                expressionToRoot(), getIndex(), length));
    }

    /**
     * When performing an indexed access based on this expression, if the target
     * value is null and the expression is indexed, throw an exception.
     */
    void validateInterimForIndexed(Object interimValue)
    {
        if (null != interimValue || !isIndexed())
        {
            return;
        }

        throw new InvalidExpressionException(
                String
                        .format(
                                "The expression, %1$s, requires a bracket evaluation for the sub-expression, %2$s, but the value for that property is null.",
                                getFullExpression().getExpression(),
                                getExpression()));
    }
}