package org.apache.seata.saga.engine.expression.spel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * @author jingliu_xiong@foxmail.com
 */
public class SpringELExpressionTest {
    @Test
    public void testGetValue() {
        ExpressionParser parser = new SpelExpressionParser();
        Expression defaultExpression = parser.parseExpression("'Hello World'.concat('!')");
        String value = (String) new SpringELExpression(defaultExpression).getValue(null);
        Assertions.assertEquals(value, "Hello World!");
    }

    @Test
    void testSetValue() {
        ExpressionParser parser = new SpelExpressionParser();
        String expression = "name";
        SpringELExpressionObject springELExpressionObject = new SpringELExpressionObject();
        Expression defaultExpression = parser.parseExpression(expression);
        SpringELExpression springELExpression = new SpringELExpression(defaultExpression);
        springELExpression.setValue("test", springELExpressionObject);
        Assertions.assertEquals(springELExpressionObject.getName(), "test");
    }

    @Test
    void testGetExpressionString() {
        ExpressionParser parser = new SpelExpressionParser();
        Expression defaultExpression = parser.parseExpression("'Hello World'.concat('!')");
        SpringELExpression springELExpression = new SpringELExpression(defaultExpression);
        Assertions.assertEquals(springELExpression.getExpressionString(), "'Hello World'.concat('!')");
    }
}