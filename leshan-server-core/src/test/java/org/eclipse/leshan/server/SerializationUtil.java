/*******************************************************************************
 * Copyright (c) 2019 Sierra Wireless and others.
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
package org.eclipse.leshan.server;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
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

            if (Serializable.class.isAssignableFrom(clazz)) {
                boolean isSerializable = true;
                for (Field field : clazz.getDeclaredFields()) {
                    // check if this field is Serializable
                    if (!isSerializable(field, ptype, notSerializableObject, excludes)) {
                        notSerializableObject.put(field, String.format("[%s %s] field from [%s] is not serializable",
                                field.getType().getSimpleName(), field.getName(), field.getDeclaringClass().getName()));
                        isSerializable = false;
                    }
                }
                return isSerializable;
            }
            // we accept Collection, Map interface because most implementation is Serializable and checking if
            // parameterized type is serializable should enough.
            else if (clazz.isInterface()) {
                if (Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz)) {
                    for (Type t : ptype.getActualTypeArguments()) {
                        if (!isSerializable(t, notSerializableObject, excludes)) {
                            notSerializableObject.put(clazz, String.format(
                                    "[%s] is parameterized with not serializable type [%s].", clazz.getName(), t));
                            return false;
                        }
                    }
                    return true;
                } else {
                    notSerializableObject.put(clazz, String.format(
                            "[%s] interface is maybe serializable but we only support Collection and Map as parameterized interface class.",
                            clazz));
                    return false;
                }
            } else {
                notSerializableObject.put(clazz,
                        String.format("[%s] is not primitive or does not implement Serializable.", clazz.getName()));
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
                    if (!isSerializable(field, null, notSerializableObject, excludes)) {
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
        }
        // others cases
        else {
            if (excludes.contains(type.toString())) {
                return true;
            }
            notSerializableObject.put(type, String.format("[%s] is maybe serializable but we don't support it.", type));
            return false;
        }
    }

    public static Type resolveType(TypeVariable<?> fieldType, ParameterizedType ptype) {
        if (!(ptype.getRawType() instanceof Class)) {
            return null;
        }
        Class<?> clazz = (Class<?>) ptype.getRawType();
        TypeVariable<?>[] typeParameters = clazz.getTypeParameters();
        int index = Arrays.asList(typeParameters).indexOf(fieldType);
        if (index < 0)
            return null;
        return ptype.getActualTypeArguments()[index];
    }

    private static boolean isSerializable(Field field, ParameterizedType ptype,
            Map<Object, String> notSerializableObject, Collection<String> excludes) {

        if (excludes.contains(field.getDeclaringClass().getName() + "." + field.getName())) {
            return true;
        }

        // we ignore static and transient field
        if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
            return true;
        }

        Type fieldType = field.getGenericType();
        // if this is a generic field (e.g. private V myAttribute from myClass<T>) resolve type first.
        if (fieldType instanceof TypeVariable<?> && ptype != null) {
            fieldType = resolveType((TypeVariable<?>) fieldType, ptype);
            if (fieldType == null) {
                notSerializableObject.put(field,
                        String.format("Generic type [%s] of [%s] field from [%s] is can not be resolved",
                                field.getGenericType(), field.getName(), field.getDeclaringClass().getName()));
            }
        }
        if (isSerializable(fieldType, notSerializableObject, excludes)) {
            return true;
        }
        return false;
    }
}