package org.eclipse.leshan.core.request.execute;

import java.nio.charset.StandardCharsets;

public class DefaultArgumentSerializer implements ArgumentSerializer {

    @Override
    public byte[] serialize(Arguments arguments) {
        StringBuilder sb = new StringBuilder();

        for (SingleArgument singleArgument: arguments) {
            if (sb.length() > 0) {
                sb.append(",");
            }

            sb.append(singleArgument.getDigit());

            if (singleArgument.getValue() != null) {
                sb.append("='");
                sb.append(singleArgument.getValue());
                sb.append("'");
            }
        }

        if (sb.toString().length() == 0) {
            return null;
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

}
