package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.util.SpElMetadataUtil;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

@Service
public class MetadataExpressionEvaluationService {

  public Object evaluateExpression(String expressionString, Class<?> aClass, Object object, Class<?> resultClassType, String dateForDateScoped) throws NoSuchMethodException {

    StandardEvaluationContext standardEvaluationContext = getEvaluationContext(aClass);

    ExpressionParser parser = new SpelExpressionParser();

    Expression expression = parser.parseExpression(expressionString);

    standardEvaluationContext.setVariable("metadata",object);
    standardEvaluationContext.setVariable("date",dateForDateScoped);

    return expression.getValue(standardEvaluationContext, resultClassType);

  }



  StandardEvaluationContext getEvaluationContext(Class<?> inputClass) throws NoSuchMethodException {

    StandardEvaluationContext standardEvaluationContext = new StandardEvaluationContext();
    standardEvaluationContext.registerFunction("eq_", SpElMetadataUtil.class.getDeclaredMethod("eq_",
        inputClass, String.class, Object.class, String.class));
    standardEvaluationContext.registerFunction("le_", SpElMetadataUtil.class.getDeclaredMethod("le_",
        inputClass, String.class, Object.class));
    standardEvaluationContext.registerFunction("lt_", SpElMetadataUtil.class.getDeclaredMethod("lt_",
        inputClass, String.class, Object.class));
    standardEvaluationContext.registerFunction("gt_", SpElMetadataUtil.class.getDeclaredMethod("gt_",
        inputClass, String.class, Object.class, String.class));
    standardEvaluationContext.registerFunction("ge_", SpElMetadataUtil.class.getDeclaredMethod("ge_",
        inputClass, String.class, Object.class));
    standardEvaluationContext.registerFunction("like_", SpElMetadataUtil.class.getDeclaredMethod("like_",
        inputClass, String.class, Object.class));
    standardEvaluationContext.registerFunction("get_",SpElMetadataUtil.class.getDeclaredMethod("get_"
        , inputClass, String.class, String.class));

    return standardEvaluationContext;

  }

}
