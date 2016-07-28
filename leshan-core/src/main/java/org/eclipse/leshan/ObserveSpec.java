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

import java.util.Arrays;
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

    private static final String PARAM_SEP = "&";

    private static final String CANCEL = "cancel";
    private static final String GREATER_THAN = "gt";
    private static final String LESS_THAN = "lt";
    private static final String MAX_PERIOD = "pmax";
    private static final String MIN_PERIOD = "pmin";
    private static final String STEP = "st";

    private static final String PARAM_MIN_PERIOD = MIN_PERIOD + "=%s";
    private static final String PARAM_MAX_PERIOD = MAX_PERIOD + "=%s";
    private static final String PARAM_GREATER_THAN = GREATER_THAN + "=%s";
    private static final String PARAM_LESS_THAN = LESS_THAN + "=%s";
    private static final String PARAM_STEP = STEP + "=%s";

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
            queries.add(CANCEL);
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
            b.append(query).append(PARAM_SEP);
        }
        return b.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (cancel ? 1231 : 1237);
        result = prime * result + ((greaterThan == null) ? 0 : greaterThan.hashCode());
        result = prime * result + ((lessThan == null) ? 0 : lessThan.hashCode());
        result = prime * result + ((maxPeriod == null) ? 0 : maxPeriod.hashCode());
        result = prime * result + ((minPeriod == null) ? 0 : minPeriod.hashCode());
        result = prime * result + ((step == null) ? 0 : step.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ObserveSpec other = (ObserveSpec) obj;
        if (cancel != other.cancel)
            return false;
        if (greaterThan == null) {
            if (other.greaterThan != null)
                return false;
        } else if (!greaterThan.equals(other.greaterThan))
            return false;
        if (lessThan == null) {
            if (other.lessThan != null)
                return false;
        } else if (!lessThan.equals(other.lessThan))
            return false;
        if (maxPeriod == null) {
            if (other.maxPeriod != null)
                return false;
        } else if (!maxPeriod.equals(other.maxPeriod))
            return false;
        if (minPeriod == null) {
            if (other.minPeriod != null)
                return false;
        } else if (!minPeriod.equals(other.minPeriod))
            return false;
        if (step == null) {
            if (other.step != null)
                return false;
        } else if (!step.equals(other.step))
            return false;
        return true;
    }

    public static ObserveSpec parse(String uriQueries) {
        if (uriQueries == null)
            return null;

        String[] queriesArray = uriQueries.split(PARAM_SEP);
        return ObserveSpec.parse(Arrays.asList(queriesArray));
    }

    public static ObserveSpec parse(List<String> uriQueries) {
        ObserveSpec.Builder builder = new ObserveSpec.Builder();
        // parse parameter without value
        if (uriQueries.equals(Arrays.asList(CANCEL))) {
            return builder.cancel().build();
        }

        // parse parameters with value
        for (final String query : uriQueries) {
            final String[] split = query.split("=");
            if (split.length != 2) {
                throw new IllegalArgumentException();
            }

            final String key = split[0];
            final String value = split[1];

            switch (key) {
            case GREATER_THAN:
                builder.greaterThan(Float.parseFloat(value));
                break;
            case LESS_THAN:
                builder.lessThan(Float.parseFloat(value));
                break;
            case STEP:
                builder.step(Float.parseFloat(value));
                break;
            case MIN_PERIOD:
                builder.minPeriod(Integer.parseInt(value));
                break;
            case MAX_PERIOD:
                builder.maxPeriod(Integer.parseInt(value));
                break;
            default:
                throw new IllegalArgumentException();
            }
        }
        return builder.build();
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
