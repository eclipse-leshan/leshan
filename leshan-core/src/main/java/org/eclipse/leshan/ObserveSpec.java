/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan;

import java.util.LinkedList;
import java.util.List;

/**
 * A container for attributes describing the intended behavior of a LWM2M Client regarding sending notifications for an
 * observed resource.
 * 
 * The Lightweight M2M spec defines the following attributes:
 * <ul>
 * <li>minimum period</li>
 * <li>maximum period</li>
 * <li>greater than</li>
 * <li>less than</li>
 * <li>step</li>
 * <li>cancel</li>
 * </ul>
 */
public final class ObserveSpec {

    private static final String PARAM_MIN_PERIOD = "pmin=%s";
    private static final String PARAM_MAX_PERIOD = "pmax=%s";
    private static final String PARAM_GREATER_THAN = "gt=%s";
    private static final String PARAM_LESS_THAN = "lt=%s";
    private static final String PARAM_STEP = "st=%s";
    private static final String PARAM_CANCEL = "cancel";
    private Integer minPeriod;
    private Integer maxPeriod;
    private Float greaterThan;
    private Float lessThan;
    private Float step;
    private boolean cancel;

    private ObserveSpec() {
    }

    public Integer getMinPeriod() {
        return this.minPeriod;
    }

    public Integer getMaxPeriod() {
        return this.maxPeriod;
    }

    public Float getGreaterThan() {
        return this.greaterThan;
    }

    public Float getLessThan() {
        return this.lessThan;
    }

    public Float getStep() {
        return this.step;
    }

    public Boolean getCancel() {
        return this.cancel;
    }

    public String[] toQueryParams() {
        List<String> queries = new LinkedList<>();
        if (this.cancel) {
            queries.add(PARAM_CANCEL);
        } else {
            if (this.minPeriod != null) {
                queries.add(String.format(PARAM_MIN_PERIOD, this.minPeriod));
            }
            if (this.maxPeriod != null) {
                queries.add(String.format(PARAM_MAX_PERIOD, this.maxPeriod));
            }
            if (this.lessThan != null) {
                queries.add(String.format(PARAM_LESS_THAN, this.lessThan));
            }
            if (this.greaterThan != null) {
                queries.add(String.format(PARAM_GREATER_THAN, this.greaterThan));
            }
            if (this.step != null) {
                queries.add(String.format(PARAM_STEP, this.step));
            }
        }
        return queries.toArray(new String[queries.size()]);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        for (String query : toQueryParams()) {
            b.append(query).append("&");
        }
        return b.toString();
    }

    /**
     * A builder for ObserveSpec instances.
     * 
     * Provides a <em>fluid API</em> for setting attributes. Creating an ObserveSpec instance works like this:
     * 
     * <pre>
     * ObserveSpec spec = new ObserveSpec.Builder().minPeriod(10).greaterThan(34.12).build();
     * </pre>
     */
    public static class Builder {

        private boolean cancel;
        private Float step;
        private Float greaterThan;
        private Float lessThan;
        private Integer minPeriod;
        private Integer maxPeriod;

        public Builder() {
            super();
        }

        public ObserveSpec build() {
            ObserveSpec spec = new ObserveSpec();
            if (this.cancel) {
                spec.cancel = Boolean.TRUE;
                return spec;
            }

            if (this.maxPeriod != null && this.minPeriod != null && this.maxPeriod < this.minPeriod) {
                throw new IllegalStateException("minPeriod must be smaller than maxPeriod");
            } else {
                spec.greaterThan = this.greaterThan;
                spec.lessThan = this.lessThan;
                spec.minPeriod = this.minPeriod;
                spec.maxPeriod = this.maxPeriod;
                spec.step = this.step;
                return spec;
            }
        }

        public Builder cancel() {
            this.cancel = true;
            return this;
        }

        public Builder step(float step) {
            this.step = step;
            return this;
        }

        public Builder greaterThan(float threshold) {
            this.greaterThan = threshold;
            return this;
        }

        public Builder lessThan(float threshold) {
            this.lessThan = threshold;
            return this;
        }

        public Builder minPeriod(int seconds) {
            this.minPeriod = seconds;
            return this;
        }

        public Builder maxPeriod(int seconds) {
            this.maxPeriod = seconds;
            return this;
        }

    }
}
