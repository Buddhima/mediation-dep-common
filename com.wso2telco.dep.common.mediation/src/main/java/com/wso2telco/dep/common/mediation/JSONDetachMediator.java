package com.wso2telco.dep.common.mediation;

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;

public class JSONDetachMediator extends AbstractMediator {

    private final String JSON_DETACH_PATH_EXPRESSION = "JSON_DETACH_PATH_EXPRESSION";
    private final String JSON_DETACH_PATH_TYPE = "JSON_DETACH_PATH_TYPE";
    private final String JSON_PATH_TYPE_CUSTOM = "custom";

    private Configuration configuration = Configuration.defaultConfiguration();

    public boolean mediate(MessageContext synapseContext) {

        SynapseLog synLog = getLog(synapseContext);
        Object object = "";

        String jsonPath = (String) synapseContext.getProperty(JSON_DETACH_PATH_EXPRESSION);
        String type = (String) synapseContext.getProperty(JSON_DETACH_PATH_TYPE);
        org.apache.axis2.context.MessageContext context =
                ((Axis2MessageContext) synapseContext).getAxis2MessageContext();

        if (JSON_PATH_TYPE_CUSTOM.equalsIgnoreCase(type)) {
            assert jsonPath != null : "JSONPathExpression should be non null in case of CUSTOM";

            String jsonString = JsonUtil.jsonPayloadToString(context);

            String modifiedJsonString = JsonPath.using(configuration).parse(jsonString).delete(jsonPath).jsonString();

            try {
                JsonUtil.getNewJsonPayload(context, modifiedJsonString, true, true);
            } catch (Exception ex) {
                synLog.error("Error while setting json payload, when cloning is false");
            }
        } else {
            handleException("Path type is invalid : " + type, synapseContext);
        }

        return true;
    }
}
