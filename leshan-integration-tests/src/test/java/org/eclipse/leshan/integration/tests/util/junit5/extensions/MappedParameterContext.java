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
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.platform.commons.util.AnnotationUtils;

public class MappedParameterContext implements ParameterContext {

    private final int index;
    private final Parameter parameter;
    private final Optional<Object> target;

    public MappedParameterContext(int index, Parameter parameter, Optional<Object> target) {
        this.index = index;
        this.parameter = parameter;
        this.target = target;
    }

    @Override
    public boolean isAnnotated(Class<? extends Annotation> annotationType) {
        return AnnotationUtils.isAnnotated(parameter, annotationType);
    }

    @Override
    public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationType) {
        return Optional.empty();
    }

    @Override
    public <A extends Annotation> List<A> findRepeatableAnnotations(Class<A> annotationType) {
        return null;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public Parameter getParameter() {
        return parameter;
    }

    @Override
    public Optional<Object> getTarget() {
        return target;
    }
}
