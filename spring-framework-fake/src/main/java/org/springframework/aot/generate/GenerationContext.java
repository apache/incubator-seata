package org.springframework.aot.generate;

import org.springframework.aot.hint.RuntimeHints;

public interface GenerationContext {

//    GeneratedClasses getGeneratedClasses();
//
//    GeneratedFiles getGeneratedFiles();

    RuntimeHints getRuntimeHints();

    GenerationContext withName(String name);

}
