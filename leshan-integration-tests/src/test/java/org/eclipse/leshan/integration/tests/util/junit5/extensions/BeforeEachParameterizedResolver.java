/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.integration.tests.util.junit5.extensions;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.engine.execution.BeforeEachMethodAdapter;
import org.junit.jupiter.engine.extension.ExtensionRegistry;

/**
 * Junit5 Extension to use parameter in BeforeEach. Largely inspired by :
 * <ul>
 * <li>https://code-case.hashnode.dev/how-to-pass-parameterized-test-parameters-to-beforeeachaftereach-method-in-junit5
 * <li>https://stackoverflow.com/a/69265907/5088764
 * </ul>
 * See https://github.com/junit-team/junit5/issues/3157 about find a better way ?
 */
public class BeforeEachParameterizedResolver implements BeforeEachMethodAdapter, ParameterResolver {

    private ParameterResolver parameterizedTestParameterResolver = null;

    @Override
    public void invokeBeforeEachMethod(ExtensionContext context, ExtensionRegistry registry) throws Throwable {
        // get default ParameterResolver
        Optional<ParameterResolver> parameterResolver = registry.getExtensions(ParameterResolver.class).stream()
                .filter(resolver -> resolver.getClass().getName().contains("ParameterizedTestParameterResolver"))
                .findFirst();

        if (!parameterResolver.isPresent()) {
            throw new IllegalStateException(
                    "ParameterizedTestParameterResolver missed in the registry. Probably it's not a Parameterized Test");
        } else {
            parameterizedTestParameterResolver = parameterResolver.get();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (isExecutedOnAfterOrBeforeMethod(parameterContext)) {
            ParameterContext pContext = getMappedContext(parameterContext, extensionContext);
            return parameterizedTestParameterResolver.supportsParameter(pContext, extensionContext);
        }
        return false;
    }

    private boolean isExecutedOnAfterOrBeforeMethod(ParameterContext parameterContext) {
        return Arrays.stream(parameterContext.getDeclaringExecutable().getDeclaredAnnotations())
                .anyMatch(this::isAfterEachOrBeforeEachAnnotation);
    }

    private boolean isAfterEachOrBeforeEachAnnotation(Annotation annotation) {
        return annotation.annotationType() == BeforeEach.class || annotation.annotationType() == AfterEach.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (isExecutedOnAfterOrBeforeMethod(parameterContext)) {
            ParameterContext pContext = getMappedContext(parameterContext, extensionContext);
            return parameterizedTestParameterResolver.resolveParameter(pContext, extensionContext);
        }
        return null;
    }

    private MappedParameterContext getMappedContext(ParameterContext parameterContext,
            ExtensionContext extensionContext) {
        return new MappedParameterContext(parameterContext.getIndex(),
                extensionContext.getRequiredTestMethod().getParameters()[parameterContext.getIndex()],
                Optional.of(parameterContext.getTarget()));
    }

}
