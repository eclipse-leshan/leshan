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
 *******************************************************************************/

package org.eclipse.leshan.integration.tests.util.junit5;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.engine.discovery.predicates.IsTestMethod;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.platform.commons.util.AnnotationUtils;
import org.junit.platform.commons.util.ReflectionUtils;

public class TestingUtils {

    private TestingUtils() {
    }

    private static interface TestInvoker {

        void invoke(Method testMethod) throws Exception;

    }

    public static Stream<DynamicNode> parameterizedClassTester(String displayName, Class<?> clazz,
            Stream<Arguments> streamOfArguments) {

        final List<Method> testMethods = ReflectionUtils.findMethods(clazz, new IsTestMethod());
        if (testMethods.isEmpty()) {
            throw new IllegalStateException(clazz.getName() + " has no supported @Test methods");
        }

        List<Constructor<?>> candidateConstructors = Arrays.stream(clazz.getDeclaredConstructors())
                .filter(ctor -> ctor.getParameterCount() != 0).collect(Collectors.toList());
        if (candidateConstructors.size() != 1) {
            if (candidateConstructors.isEmpty()) {
                throw new IllegalStateException(clazz.getName() + " has no candiate constructors");
            }
            throw new IllegalStateException(clazz.getName() + " has more than one candiate constructors");
        }

        final MessageFormat displayNameFormatter = new MessageFormat(displayName);

        final Constructor<?> constructor = candidateConstructors.get(0);
        constructor.setAccessible(true);

        final List<Method> beforeEachMethods = AnnotationUtils.findAnnotatedMethods(clazz, BeforeEach.class,
                ReflectionUtils.HierarchyTraversalMode.TOP_DOWN);

        final List<Method> afterEachMethods = AnnotationUtils.findAnnotatedMethods(clazz, AfterEach.class,
                ReflectionUtils.HierarchyTraversalMode.BOTTOM_UP);

        for (List<Method> methods : Arrays.asList(testMethods, beforeEachMethods, afterEachMethods)) {
            for (Method method : methods) {
                method.setAccessible(true);
            }
        }

        return streamOfArguments.map(arguments -> {
            final Object[] arrayOfArguments = arguments.get();

            final TestInvoker testInvoker = new TestInvoker() {
                private Object instance;

                @Override
                public void invoke(Method testMethod) throws Exception {
                    if (instance == null) {
                        instance = constructor.newInstance(arrayOfArguments);
                    }

                    try {
                        for (Method method : beforeEachMethods) {
                            method.invoke(instance);
                        }

                        testMethod.invoke(instance);

                    } finally {
                        for (Method method : afterEachMethods) {
                            method.invoke(instance);
                        }
                    }
                }
            };

            return DynamicContainer.dynamicContainer(displayNameFormatter.format(arrayOfArguments), testMethods.stream()
                    .map(method -> DynamicTest.dynamicTest(method.getName() + "()", () -> testInvoker.invoke(method))));
        });
    }

}
