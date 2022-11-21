package org.eclipse.leshan.client.demo;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import org.eclipse.leshan.client.resource.SimpleInstanceEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.node.ObjectLink;
import org.eclipse.leshan.core.request.argument.Argument;
import org.eclipse.leshan.core.request.argument.Arguments;
import org.eclipse.leshan.core.request.argument.InvalidArgumentException;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.RandomStringUtils;

/**
 * This aims to implements LWM2M Test Object(Id:3441) from LWM2M registry.
 */
public class LwM2mTestObject extends SimpleInstanceEnabler {

    private final Random random = new Random(System.currentTimeMillis());

    public LwM2mTestObject() {
        super();
        initialValues = new HashMap<>();

        initialValues.put(4, Collections.EMPTY_MAP);

        // single
        initialValues.put(110, "initial value");
        initialValues.put(120, 1024l);
        initialValues.put(130, 3.14159d);
        initialValues.put(140, true);
        initialValues.put(150, Hex.decodeHex("0123456789ABCDEF".toCharArray()));
        initialValues.put(160, new Date(946684800000l));
        initialValues.put(170, new ObjectLink(3, 0));

        // multi
        initialValues.put(1110, getValues(0, "initial value"));
        initialValues.put(1120, getValues(0, 1024l));
        initialValues.put(1130, getValues(0, 3.14159d));
        initialValues.put(1140, getValues(0, true));
        initialValues.put(1150, getValues(0, Hex.decodeHex("0123456789ABCDEF".toCharArray())));
        initialValues.put(1160, getValues(0, new Date(946684800000l)));
        initialValues.put(1170, getValues(0, new ObjectLink(3, 0)));
    }

    private Map<Integer, Object> getValues(Integer id, Object value) {
        HashMap<Integer, Object> res = new HashMap<>();
        res.put(id, value);
        return res;
    }

    private void clearValues() {
        Map<Integer, Object> clearedValues = new HashMap<>();

        clearedValues.put(4, Collections.EMPTY_MAP);

        // single
        clearedValues.put(110, "");
        clearedValues.put(120, 0l);
        clearedValues.put(130, 0.0d);
        clearedValues.put(140, false);
        clearedValues.put(150, new byte[0]);
        clearedValues.put(160, new Date(0l));
        clearedValues.put(170, new ObjectLink());

        // multi
        clearedValues.put(1110, Collections.EMPTY_MAP);
        clearedValues.put(1120, Collections.EMPTY_MAP);
        clearedValues.put(1130, Collections.EMPTY_MAP);
        clearedValues.put(1140, Collections.EMPTY_MAP);
        clearedValues.put(1150, Collections.EMPTY_MAP);
        clearedValues.put(1160, Collections.EMPTY_MAP);
        clearedValues.put(1170, Collections.EMPTY_MAP);

        fireResourcesChange(applyValues(clearedValues));
    }

    private void resetValues() {
        fireResourcesChange(applyValues(initialValues));
    }

    private void randomValues() {

        Map<Integer, Object> randomValues = new HashMap<>();
        randomValues.put(4, generateResourceInstances(new StringGenerator()));

        // single
        randomValues.put(110, new StringGenerator().generate());
        randomValues.put(120, new LongGenerator().generate());
        randomValues.put(130, new DoubleGenerator().generate());
        randomValues.put(140, new BooleanGenerator().generate());
        randomValues.put(150, new BytesGenerator().generate());
        randomValues.put(160, new DateGenerator().generate());
        randomValues.put(170, new ObjectLinkGenerator().generate());

        // multi
        randomValues.put(1110, generateResourceInstances(new StringGenerator()));
        randomValues.put(1120, generateResourceInstances(new LongGenerator()));
        randomValues.put(1130, generateResourceInstances(new DoubleGenerator()));
        randomValues.put(1140, generateResourceInstances(new BooleanGenerator()));
        randomValues.put(1150, generateResourceInstances(new BytesGenerator()));
        randomValues.put(1160, generateResourceInstances(new DateGenerator()));
        randomValues.put(1170, generateResourceInstances(new ObjectLinkGenerator()));

        fireResourcesChange(applyValues(randomValues));
    }

    private void storeArguments(Arguments arguments) {
        // convert Arguments to Map
        Map<Integer, String> argValues = new LinkedHashMap<Integer, String>();
        for (Argument argument : arguments) {
            String value = argument.getValue() == null ? "" : argument.getValue();
            argValues.put(argument.getDigit(), value);
        }
        // put value in resource (4)
        Map<Integer, Object> newParams = new HashMap<>();
        newParams.put(4, argValues);
        fireResourcesChange(applyValues(newParams));
    }

    private Map<Integer, ?> generateResourceInstances(ValueGenerator<?> generator) {
        HashMap<Integer, Object> instances = new HashMap<>();
        for (int i = 0; i < random.nextInt(10); i++) {
            instances.put(i, generator.generate());
        }
        return instances;
    }

    @Override
    public ExecuteResponse execute(ServerIdentity identity, int resourceid, String arguments) {
        switch (resourceid) {
        case 0:
            resetValues();
            return ExecuteResponse.success();
        case 1:
            randomValues();
            return ExecuteResponse.success();
        case 2:
            clearValues();
            return ExecuteResponse.success();
        case 3:
            try {
                storeArguments(Arguments.parse(arguments));
            } catch (InvalidArgumentException e) {
                throw new InvalidRequestException(e);
            }
            return ExecuteResponse.success();
        default:
            return super.execute(identity, resourceid, arguments);
        }
    }

    /* ***************** Random Generator ****************** */

    static interface ValueGenerator<T> {
        T generate();
    }

    class StringGenerator implements ValueGenerator<String> {
        @Override
        public String generate() {
            return RandomStringUtils.randomAlphanumeric(random.nextInt(20));
        }
    }

    class LongGenerator implements ValueGenerator<Long> {
        @Override
        public Long generate() {
            return random.nextLong();
        }
    }

    class DoubleGenerator implements ValueGenerator<Double> {
        @Override
        public Double generate() {
            return random.nextDouble();
        }
    }

    class BooleanGenerator implements ValueGenerator<Boolean> {
        @Override
        public Boolean generate() {
            return random.nextInt(2) == 1;
        }
    }

    class BytesGenerator implements ValueGenerator<byte[]> {
        @Override
        public byte[] generate() {
            byte[] bytes = new byte[random.nextInt(20)];
            random.nextBytes(bytes);
            return bytes;
        }
    }

    class DateGenerator implements ValueGenerator<Date> {
        @Override
        public Date generate() {
            // try to generate random date which is not so out of date.
            long rd = System.currentTimeMillis();

            // remove year randomly
            rd -= (random.nextInt(20) - 10) * 31557600000l;

            // add some variance in the year
            rd += random.nextInt(3155760) * 1000l;

            return new Date(rd);
        }
    }

    class ObjectLinkGenerator implements ValueGenerator<ObjectLink> {
        @Override
        public ObjectLink generate() {
            return new ObjectLink(random.nextInt(ObjectLink.MAXID - 1), random.nextInt(ObjectLink.MAXID - 1));
        }
    }
}
