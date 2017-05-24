package com.wso2telco.dep.common.mediation;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

public class JSONEnrichMediator extends AbstractMediator {

    final String JSON_PATH_TYPE_CUSTOM = "custom";
    final String JSON_PATH_TYPE_PAYLOAD = "payload";
    final String JSON_PATH_TYPE_PROPERTY = "property";

    final String JSON_ENRICH_SOURCE_JSONPATH = "JSON_ENRICH_SOURCE_JSONPATH";
    final String JSON_ENRICH_SOURCE_PROPERTY = "JSON_ENRICH_SOURCE_PROPERTY";
    final String JSON_ENRICH_SOURCE_TYPE = "JSON_ENRICH_SOURCE_TYPE";
    final String JSON_ENRICH_SOURCE_CLONE = "JSON_ENRICH_SOURCE_CLONE";
    final String JSON_ENRICH_SOURCE_JSON_NODE = "JSON_ENRICH_SOURCE_JSON_NODE";

    final String JSON_ENRICH_TARGET_JSONPATH = "JSON_ENRICH_TARGET_JSONPATH";
    final String JSON_ENRICH_TARGET_PROPERTY = "JSON_ENRICH_TARGET_PROPERTY";
    final String JSON_ENRICH_TARGET_TYPE = "JSON_ENRICH_TARGET_TYPE";
    final String JSON_ENRICH_TARGET_ACTION = "JSON_ENRICH_TARGET_ACTION";

    Configuration configuration = Configuration.defaultConfiguration();

    public boolean mediate(MessageContext context) {

        String sourceJsonPath = context.getProperty(JSON_ENRICH_SOURCE_JSONPATH) != null ? (String) context.getProperty(JSON_ENRICH_SOURCE_JSONPATH) : null;
        String sourceProperty = context.getProperty(JSON_ENRICH_SOURCE_PROPERTY) != null ? (String) context.getProperty(JSON_ENRICH_SOURCE_PROPERTY) : null;
        String sourceType = context.getProperty(JSON_ENRICH_SOURCE_TYPE) != null ? (String) context.getProperty(JSON_ENRICH_SOURCE_TYPE) : JSON_PATH_TYPE_CUSTOM;
        String sourceClone = context.getProperty(JSON_ENRICH_SOURCE_CLONE) != null ? (String) context.getProperty(JSON_ENRICH_SOURCE_CLONE) : null;
        String sourceInlineJSONNode = context.getProperty(JSON_ENRICH_SOURCE_JSON_NODE) != null ? (String) context.getProperty(JSON_ENRICH_SOURCE_JSON_NODE) : null;

        String targetJsonPath = context.getProperty(JSON_ENRICH_TARGET_JSONPATH) != null ? (String) context.getProperty(JSON_ENRICH_TARGET_JSONPATH) : null;
        String targetProperty = context.getProperty(JSON_ENRICH_TARGET_PROPERTY) != null ? (String) context.getProperty(JSON_ENRICH_TARGET_PROPERTY) : null;
        String targetType = context.getProperty(JSON_ENRICH_TARGET_TYPE) != null ? (String) context.getProperty(JSON_ENRICH_TARGET_TYPE) : JSON_PATH_TYPE_CUSTOM;
        String targetAction = context.getProperty(JSON_ENRICH_TARGET_ACTION) != null ? (String) context.getProperty(JSON_ENRICH_TARGET_ACTION) : null;

        try {
            Object sourceNode;
            SynapseLog synLog = getLog(context);

            JsonSource source = new JsonSource(sourceJsonPath, sourceProperty, sourceType, sourceClone, sourceInlineJSONNode);
            sourceNode = source.evaluate(context, synLog);
            if (sourceNode == null) {
                handleException("Failed to get the source for Enriching : ", context);
            } else {
                JsonTarget target = new JsonTarget(targetJsonPath, targetProperty, targetType, targetAction);
                target.insert(context, sourceNode, synLog);
            }
        } catch (Exception e) {
            handleException("Failed to get the source for Enriching", e, context);
        }

        return true;
    }


    class JsonSource {
        private String jsonPath = null;
        private String property = null;
        private String sourceType = JSON_PATH_TYPE_CUSTOM;
        private boolean clone = true;
        private String inlineJSONNode = null;
        private String inlineKey = null;

        public JsonSource (String jsonPath, String property, String sourceType, String clone, String inlineJSONNode) {
            this.jsonPath = jsonPath;
            this.property = property;
            this.sourceType = (sourceType != null) ? sourceType : JSON_PATH_TYPE_CUSTOM;
            this.clone = clone == null || Boolean.parseBoolean(clone);
            this.inlineJSONNode = inlineJSONNode;
        }

        public Object evaluate(MessageContext synapseContext, SynapseLog synLog) {

            Object object = "";

            org.apache.axis2.context.MessageContext context = ((Axis2MessageContext) synapseContext).getAxis2MessageContext();

            if(!JsonUtil.hasAJsonPayload(context)) {
                synLog.error("JSON payload not found in message context");
            }

            if (JSON_PATH_TYPE_CUSTOM.equalsIgnoreCase(sourceType)) {

                assert jsonPath != null : "JSONPathExpression should be non null in case of CUSTOM";

                String jsonString = JsonUtil.jsonPayloadToString(context);

                object = JsonPath.using(configuration).parse(jsonString).read(jsonPath);

                if (!clone) {

                    // when cloning is false, remove the element in JSON path from payload
                    String modifiedJsonString = JsonPath.using(configuration).parse(jsonString).delete(jsonPath).jsonString();
                    try {
                        JsonUtil.getNewJsonPayload(context, modifiedJsonString, true, true);
                    } catch (Exception ex) {
                        synLog.error("Error while setting json payload, when cloning is false");
                    }
                }

            } else if (JSON_PATH_TYPE_PAYLOAD.equalsIgnoreCase(sourceType)) {
                object = JsonUtil.jsonPayloadToString(context);

                if (!clone)
                    JsonUtil.removeJsonPayload(context);

            } else if (JSON_PATH_TYPE_PROPERTY.equalsIgnoreCase(sourceType)) {

                assert property != null : "property shouldn't be null when type is PROPERTY";

                Object o = synapseContext.getProperty(property);

                if (o instanceof String) {
                    String sourceStr = (String) o;

                    object = sourceStr;
                } else {
                    synLog.error("Invalid source property type");
                }

                if (!clone)
                    synapseContext.getPropertyKeySet().remove(property);

            } else {
                handleException("Source type is invalid : " + sourceType, synapseContext);
            }

            return object;
        }
    }

    class JsonTarget {

        private String jsonPath = null;
        private String property = null;
        private String targetType = JSON_PATH_TYPE_CUSTOM;

        // constants
        public static final String ACTION_SET = "set";
        public static final String ACTION_ADD = "add";
        public static final String ACTION_PUT = "put";
        private String action = ACTION_SET;

        public JsonTarget (String jsonPath, String property, String targetType, String action) {
            this.jsonPath = jsonPath;
            this.property = property;
            this.targetType =  (targetType != null) ? targetType : JSON_PATH_TYPE_CUSTOM;
            this.action = action;
        }

        public void insert(MessageContext synapseContext, Object sourceNode, SynapseLog synLog) {

            if (JSON_PATH_TYPE_CUSTOM.equalsIgnoreCase(targetType)) {

                assert jsonPath != null : "JSONPathExpression should be non null in case of CUSTOM";

                setValueInPath(synapseContext, jsonPath, sourceNode);

            } else if (JSON_PATH_TYPE_PAYLOAD.equalsIgnoreCase(targetType)) {

                org.apache.axis2.context.MessageContext context = ((Axis2MessageContext) synapseContext).getAxis2MessageContext();
                try {
                    String jsonString = JsonPath.using(configuration).parse(sourceNode).jsonString();
                    JsonUtil.getNewJsonPayload(context, jsonString, true, true);
                } catch (Exception ex) {
                    synLog.error("Error while setting json payload in BODY");
                }

            } else if (JSON_PATH_TYPE_PROPERTY.equalsIgnoreCase(targetType)) {

                synapseContext.setProperty(property, sourceNode);

            } else {
                handleException("Target type is invalid : " + targetType, synapseContext);
            }

        }

        private void setValueInPath(MessageContext synapseContext, String jsonPath, Object sourceNode) {

            String expression = jsonPath;

            // Though SynapseJsonPath support "$.", the JSONPath implementation does not support it
            if (expression.endsWith(".")) {
                expression = expression.substring(0, expression.length()-1);
            }

            org.apache.axis2.context.MessageContext context = ((Axis2MessageContext) synapseContext).getAxis2MessageContext();

            assert JsonUtil.hasAJsonPayload(context) : "Message Context does not contain a JSON payload";

            String jsonString = JsonUtil.jsonPayloadToString(context);
            String newJsonString = "";

            if (action.equalsIgnoreCase(ACTION_SET)) {
                // replaces an existing value in json
                newJsonString = JsonPath.using(configuration).parse(jsonString).set(expression, sourceNode).jsonString();

            } else if (action.equalsIgnoreCase(ACTION_ADD)) {
                // adds a value to a json array
                newJsonString = JsonPath.using(configuration).parse(jsonString).add(expression, sourceNode).jsonString();

            } else if (action.equalsIgnoreCase(ACTION_PUT)) {
                // adds a new property to a json object
                assert property != null : "new property name should be specified";
                newJsonString = JsonPath.using(configuration).parse(jsonString).put(expression, property, sourceNode).jsonString();

            } else {
                // invalid action
                log.error("Invalid action set: " + action);
            }

            try {
                if (!newJsonString.trim().isEmpty()) {
                    JsonUtil.getNewJsonPayload(context, newJsonString, true, true);
                }
            } catch (Exception ex) {
                log.error(ex);
            }
        }

    }
}
