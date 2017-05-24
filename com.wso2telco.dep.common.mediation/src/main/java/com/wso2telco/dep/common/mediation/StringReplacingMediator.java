package com.wso2telco.dep.common.mediation;

import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

public class StringReplacingMediator extends AbstractMediator {

    private final String STRING_REPLACE_SEARCH_VALUE = "STRING_REPLACE_SEARCH_VALUE";
    private final String STRING_REPLACE_NEW_VALUE = "STRING_REPLACE_NEW_VALUE";
    private final String STRING_REPLACE_INPUT_VALUE = "STRING_REPLACE_INPUT_VALUE";
    private final String STRING_REPLACE_OUTPUT_PROPERTY = "STRING_REPLACE_OUTPUT_PROPERTY";

    public boolean mediate(MessageContext context) {

        String searchValue = context.getProperty(STRING_REPLACE_SEARCH_VALUE) != null ? (String) context.getProperty(STRING_REPLACE_SEARCH_VALUE) : null;
        String newValue = context.getProperty(STRING_REPLACE_NEW_VALUE) != null ? (String) context.getProperty(STRING_REPLACE_NEW_VALUE) : null;
        String inputValue = context.getProperty(STRING_REPLACE_INPUT_VALUE) != null ? (String) context.getProperty(STRING_REPLACE_INPUT_VALUE) : null;
        String outputProperty = context.getProperty(STRING_REPLACE_OUTPUT_PROPERTY) != null ? (String) context.getProperty(STRING_REPLACE_OUTPUT_PROPERTY) : null;

        assert searchValue != null : "searchValue is empty";
        assert newValue != null : "newValue is empty";
        assert inputValue != null : "inputValue is empty";
        assert outputProperty != null : "outputProperty is empty";

        String outputValue = inputValue.replaceFirst(searchValue, newValue);

        context.setProperty(outputProperty, outputValue);

        return true;
    }
}
