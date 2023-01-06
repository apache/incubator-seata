package org.springframework.beans.factory.aot;

import org.springframework.aot.generate.GenerationContext;

@FunctionalInterface
public interface BeanRegistrationAotContribution {

//    default BeanRegistrationCodeFragments customizeBeanRegistrationCodeFragments(
//            GenerationContext generationContext, BeanRegistrationCodeFragments codeFragments) {
//        return codeFragments;
//    }

    void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode);

//    static BeanRegistrationAotContribution withCustomCodeFragments(UnaryOperator<BeanRegistrationCodeFragments> defaultCodeFragments) {
//        return null;
//    }

}
