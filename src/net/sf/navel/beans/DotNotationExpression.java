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

/**
 * @author cmdln
 * 
 */
class DotNotationExpression
{

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

    String getExpression()
    {
        return expression;
    }

    PropertyExpression getRoot()
    {
        return rootExpression;
    }

    int getDepth()
    {
        return depth;
    }
}

class PropertyExpression
{

    private final DotNotationExpression fullExpression;
    
    private final String localExpression; 
    
    private final String expressionToLeaf;
    
    private String expressionToRoot;
    
    private final PropertyExpression parent;

    private final PropertyExpression child;

    private final String propertyName;

    private final Integer elementIndex;

    PropertyExpression(DotNotationExpression fullExpression, PropertyExpression parent, String toEvaluate)
    {
        this.expressionToLeaf = toEvaluate;
        this.fullExpression = fullExpression;
        
        int dotIndex = toEvaluate.indexOf('.');

        this.localExpression = -1 == dotIndex ? toEvaluate
                : toEvaluate.substring(0, dotIndex);

        if (-1 == dotIndex)
        {
            child = null;
        }
        else
        {
            String remainingExpression = toEvaluate.substring(dotIndex + 1);

            child = new PropertyExpression(fullExpression, this, remainingExpression);
        }

        this.parent = parent;

        if (localExpression.endsWith("]")
                && localExpression.indexOf('[') != -1)
        {
            this.propertyName = localExpression.substring(0, toEvaluate
                    .indexOf('['));
            this.elementIndex = IndexedPropertyManipulator
                    .getIndex(toEvaluate);
        }
        else
        {
            this.propertyName = localExpression;
            this.elementIndex = null;
        }
    }
    
    DotNotationExpression getFullExpression()
    {
        return fullExpression;
    }
    
    String getExpression()
    {
        return localExpression;
    }

    boolean isRoot()
    {
        return null == parent;
    }

    PropertyExpression getParent()
    {
        return parent;
    }

    boolean isLeaf()
    {
        return null == child;
    }

    PropertyExpression getChild()
    {
        return child;
    }

    String getPropertyName()
    {
        return propertyName;
    }

    boolean isIndexed()
    {
        return elementIndex != null;
    }

    int getIndex()
    {
        return null == elementIndex ? -1 : (int) elementIndex;
    }

    String expressionToLeaf()
    {
        return expressionToLeaf;
    }

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
}