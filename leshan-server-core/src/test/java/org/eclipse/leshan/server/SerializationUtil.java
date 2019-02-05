/*******************************************************************************
 * Copyright (c) 2019 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class SerializationUtil {

    /**
     * Check is a class is serializable using default java serialization mechanism.
     * 
     * serializable means thats the class implements {@link Serializable} and all the fields of this class are
     * serializable too.
     * 
     * For parameterized field we only support {@link collection} and Map {@link Map}
     * 
     * @param class to check
     * @param excludes field or class to exclude of checks
     * @return null if class is serializable or a map of errors (unserializable class or field => error message).
     */
    public static Map<Object, String> isSerializable(Class<?> clazz, String... excludes) {
        Map<Object, String> notSerializableObject = new LinkedHashMap<>();
        isSerializable(clazz, notSerializableObject, Arrays.asList(excludes));
        return notSerializableObject;
    }

    private static boolean isSerializable(Type type, Map<Object, String> notSerializableObject,
            Collection<String> excludes) {

        // check generic type : e.g List<String>.
        if (type instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType) type;
            Class<?> clazz = (Class<?>) ptype.getRawType();
            if (excludes.contains(clazz.getName())) {
                return true;
            }
            // we support only collection and map and we consider that checking only if parameterized type is
            // serializable is enough.
            if (Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz)) {
                for (Type t : ptype.getActualTypeArguments()) {
                    if (!isSerializable(t, notSerializableObject, excludes)) {
                        notSerializableObject.put(clazz, String
                                .format("[%s] is parameterized with not serializable type [%s].", clazz.getName(), t));
                        return false;
                    }
                }
                return true;
            } else {
                notSerializableObject.put(clazz, String.format(
                        "[%s] is maybe serializable but we only support Collection and Map as parameterized class.",
                        clazz));
                return false;
            }
        }
        // check classic class
        else if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            // do not check type excluded
            if (excludes.contains(clazz.getName())) {
                return true;
            }
            if (clazz.isPrimitive()) {
                return true;
            }
            if (clazz.isArray()) {
                return isSerializable(clazz.getComponentType(), notSerializableObject, excludes);
            } else if (Serializable.class.isAssignableFrom(clazz)) {
                boolean isSerializable = true;
                for (Field field : clazz.getDeclaredFields()) {
                    if (!isSerializable(field, notSerializableObject, excludes)) {
                        notSerializableObject.put(field, String.format("[%s %s] field from [%s] is not serializable",
                                field.getType().getSimpleName(), field.getName(), field.getDeclaringClass().getName()));
                        isSerializable = false;
                    }
                }
                return isSerializable;
            } else {
                notSerializableObject.put(clazz,
                        String.format("[%s] is not primitive or does not implement Serializable.", clazz.getName()));
                return false;
            }
        } else {
            if (excludes.contains(type.toString())) {
                return true;
            }
            notSerializableObject.put(type, String.format("[%s] is maybe serializable but we don't support it.", type));
            return false;
        }
    }

    private static boolean isSerializable(Field field, Map<Object, String> notSerializableObject,
            Collection<String> excludes) {

        if (excludes.contains(field.getDeclaringClass().getName() + "." + field.getName())) {
            return true;
        }

        // we ignore static and transient field
        if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
            return true;
        }

        if (isSerializable(field.getGenericType(), notSerializableObject, excludes)) {
            return true;
        }
        return false;
    }
}