/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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
package org.eclipse.leshan.core.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.leshan.core.util.Validate;

/**
 * A {@link LwM2mPath} prefixed by some URI segment.
 */
public class PrefixedLwM2mPath implements Comparable<PrefixedLwM2mPath> {

    private final List<String> prefix;
    private final LwM2mPath path;

    public PrefixedLwM2mPath(List<String> prefix, LwM2mPath path) {
        Validate.notNull(path);
        Validate.notNull(prefix);
        this.prefix = prefix.isEmpty() ? Collections.emptyList() : prefix;
        this.path = path;
    }

    public PrefixedLwM2mPath(LwM2mPath path) {
        Validate.notNull(path);
        this.prefix = Collections.emptyList();
        this.path = path;
    }

    public boolean hasPrefix() {
        return !prefix.isEmpty();
    }

    public List<String> getPrefix() {
        return prefix;
    }

    public PrefixedLwM2mPath removePrefix(List<String> prePrefix) {
        validateStartWith(prePrefix);

        List<String> newPrefix = new ArrayList<>(this.prefix.subList(0, prePrefix.size()));
        return new PrefixedLwM2mPath(newPrefix, this.getPath());
    }

    public boolean startWith(List<String> prePrefix) {
        // all prefixed path start by an empty prefix
        if (prePrefix == null || prePrefix.isEmpty()) {
            return true;
        }
        // if current prefix path is shorter than pre-prefix, it can not be its start
        if (this.prefix.isEmpty() || this.prefix.size() < prePrefix.size()) {
            return false;
        }
        // now check if all part of pre-prefix match start of current prefix
        for (int i = 0; i < prePrefix.size(); i++) {
            String prePrefixpart = prePrefix.get(i);
            String prefixPart = this.prefix.get(i);
            if (!Objects.equals(prePrefixpart, prefixPart)) {
                return false;
            }
        }
        return true;
    }

    public boolean startWith(PrefixedLwM2mPath prefixedPath) {
        if (this.hasSamePrefix(prefixedPath)) {
            return this.path.startWith(prefixedPath.path);
        } else {
            if (prefixedPath.getPath().isRoot()) { // so it has only prefixed
                return this.startWith(prefixedPath.getPrefix());
            }
            return false;
        }
    }

    public void validateStartWith(List<String> prePrefix) {
        if (!startWith(prePrefix)) {
            throw new InvalidLwM2mPathException(
                    String.format("%s path doesn't start by %s", this, String.join("/", prePrefix)));
        }
    }

    /**
     * @return prefix as string if there is no prefix then it returns an empty string
     */
    public String getPrefixAsString() {
        StringBuilder b = new StringBuilder();
        appendPrefixTo(b, true);
        return b.toString();
    }

    /**
     * @return prefix as string without starting slash if there is no prefix then it returns an empty string
     */
    public String getPrefixWithoutSlashAsString() {
        StringBuilder b = new StringBuilder();
        appendPrefixTo(b, false);
        return b.toString();
    }

    /**
     * @param rootPath it should start "/" but not end with "/". (can eventually be <code>null</code> if there is no
     *        rootpath)
     * @return <code>true</code> if this path start with given rootPath.
     */
    public boolean useRootPath(String rootPath) {
        if (rootPath == null) {
            return true;
        }
        if (rootPath.length() == 0 || rootPath.charAt(0) != '/') {
            throw new IllegalArgumentException("rootPath should start by '/'");
        }
        if (rootPath.length() == 1) {
            return true;
        }
        return rootPath.startsWith(getPrefixAsString());
    }

    public boolean hasSamePrefix(PrefixedLwM2mPath path) {
        return this.prefix.equals(path.getPrefix());
    }

    public LwM2mPath getPath() {
        return path;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(path, prefix);
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof PrefixedLwM2mPath))
            return false;
        PrefixedLwM2mPath other = (PrefixedLwM2mPath) obj;
        return Objects.equals(path, other.path) && Objects.equals(prefix, other.prefix);
    }

    /**
     * Append prefix to given {@link StringBuilder}
     */
    public void appendPrefixTo(StringBuilder b, boolean startWithSlash) {
        if (hasPrefix()) {
            List<String> pfx = getPrefix();
            if (startWithSlash) {
                b.append('/');
            }
            b.append(pfx.get(0));
            for (int i = 1; i < pfx.size(); i++) {
                b.append('/');
                b.append(pfx.get(i));
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        appendPrefixTo(b, true);
        getPath().appendTo(b);
        return b.toString();
    }

    @Override
    public int compareTo(PrefixedLwM2mPath other) {
        if (!hasPrefix()) {
            if (other.hasPrefix()) {
                return -1; // arbitrary we decide that prefixed path should be after unprefixed path
            } else {
                // both without prefix
                return getPath().compareTo(other.getPath());
            }
        } else {
            if (!other.hasPrefix()) {
                return 1; // arbitrary we decide that prefixed path should be after unprefixed path
            } else {
                // both with prefix
                int minLength = Math.min(this.getPrefix().size(), other.getPrefix().size());
                for (int i = 0; i < minLength; i++) {
                    int cmp = getPrefix().get(i).compareTo(other.getPrefix().get(i));
                    if (cmp != 0) {
                        return cmp;
                    }
                }
                // If all elements are equal so far, compare by length
                int lengthComparison = Integer.compare(this.getPrefix().size(), other.getPrefix().size());
                if (lengthComparison == 0) {
                    // if length are equal, compare path
                    return getPath().compareTo(other.getPath());
                } else {
                    // else shorter is before
                    return lengthComparison;
                }
            }
        }
    }

    public static PrefixedLwM2mPath fromString(String prefix) {
        return new PrefixedLwM2mPathParser().parsePrefixedPath(prefix);
    }

    public static List<PrefixedLwM2mPath> fromPathList(Collection<LwM2mPath> paths) {
        List<PrefixedLwM2mPath> prefixedPaths = new ArrayList<PrefixedLwM2mPath>();
        for (LwM2mPath path : paths) {
            prefixedPaths.add(new PrefixedLwM2mPath(path));
        }
        return prefixedPaths;
    }

    public static List<PrefixedLwM2mPath> fromStringArray(String... paths) {
        return fromStringList(Arrays.asList(paths));
    }

    public static List<PrefixedLwM2mPath> fromStringList(List<String> paths) {
        List<PrefixedLwM2mPath> prefixedPaths = new ArrayList<>();
        PrefixedLwM2mPathParser prefixedParser = new PrefixedLwM2mPathParser();
        for (String path : paths) {
            prefixedPaths.add(prefixedParser.parsePrefixedPath(path));
        }
        return prefixedPaths;
    }

    public static List<LwM2mPath> toPathList(Collection<PrefixedLwM2mPath> prefixedPaths) {
        List<LwM2mPath> path = new ArrayList<>();
        for (PrefixedLwM2mPath prefixedPath : prefixedPaths) {
            if (prefixedPath.hasPrefix()) {
                throw new IllegalArgumentException(
                        String.format("can not convert %s to LwM2mPath because it has a prefix", prefixedPath));
            }
            path.add(prefixedPath.getPath());
        }
        return path;
    }

    /**
     * Checks if any pair of paths from a list of paths is overlapping i.e. one path contains the other
     *
     * @param paths list of paths to be validated
     *
     * @exception IllegalArgumentException if there exists any pair of overlapping paths in the list
     */
    public static void validateNotOverlapping(List<PrefixedLwM2mPath> paths) {
        for (int i = 0; i < paths.size(); i++) {
            PrefixedLwM2mPath firstPath = paths.get(i);
            for (int j = i + 1; j < paths.size(); j++) {
                PrefixedLwM2mPath secondPath = paths.get(j);
                if (firstPath.startWith(secondPath) || secondPath.startWith(firstPath)) {
                    throw new IllegalArgumentException(String
                            .format("Invalid path list :  %s and %s are overlapped paths", firstPath, secondPath));
                }
            }
        }
    }
}
