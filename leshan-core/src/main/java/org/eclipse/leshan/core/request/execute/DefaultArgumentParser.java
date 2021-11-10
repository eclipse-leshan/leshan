package org.eclipse.leshan.core.request.execute;

import java.util.ArrayList;
import java.util.List;

public class DefaultArgumentParser implements ArgumentParser {

    @Override
    public Arguments parse(String content) {
        List<SingleArgument> argumentList = new ArrayList<>();

        if (content != null && !content.isEmpty()) {
            String[] arguments = content.split(",");
            for (String argument : arguments) {
                String[] keyValue = argument.split("=");
                String value = keyValue.length == 1 ? null : keyValue[1].substring(1, keyValue[1].length() - 1);
                argumentList.add(new SingleArgument(Integer.parseInt(keyValue[0]), value));
            }
        }

        return new Arguments(argumentList);
    }

}
