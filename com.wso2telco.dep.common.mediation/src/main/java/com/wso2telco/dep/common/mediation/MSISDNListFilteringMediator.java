package com.wso2telco.dep.common.mediation;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.util.MessageHelper;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import java.util.List;

/**
 * This mediator filters MSISDN according to a filtering value given
 *
 * Must to have following properties in MessageContext
 *
 * MSISDN_LIST_XPATH    XPath of the MSISDN list
 * OPERATOR_CODE        Operator Code to filter matching MSISDNs
 */
public class MSISDNListFilteringMediator extends AbstractMediator {

    private EndpointRetrieverMediator endpointRetrieverMediator = new EndpointRetrieverMediator();

    public boolean mediate(MessageContext synCtx) {
        String msisdnListXpath = (String)synCtx.getProperty("MSISDN_LIST_XPATH");
        String operatorCode = (String)synCtx.getProperty("OPERATOR_CODE");

        MessageContext clonedSynCtx = null;
        SynapseXPath expression = null;

        boolean returnState = true;

        try {
            clonedSynCtx = MessageHelper.cloneMessageContext(synCtx);
        } catch (AxisFault axisFault) {
            log.error("Failed cloning synapse-context in MSISDNListFilteringMediator");
            returnState = false;
        }

        try {
            expression = new SynapseXPath(msisdnListXpath);
        } catch (JaxenException e) {
            log.error("Failed to set SynapseXPath in MSISDNListFilteringMediator");
            returnState = false;
        }

        if (null != expression && null != clonedSynCtx) {
            Object o = expression.evaluate(synCtx.getEnvelope(), synCtx);

            // When there's a list of MSISDN elements
            if (o instanceof List) {
                List oList = (List) o;

                filterListElements(oList, operatorCode, clonedSynCtx);
            }
        }

        return returnState;
    }

    /**
     * This method is to validate each MSISDN in the list
     *
     * @param oList             List of MSISDNs
     * @param operatorCode      OperatorCode to filter
     * @param clonedSynCtx      A MessageContext to invoke endpointRetrieverMediator
     */
    private void filterListElements(List oList, String operatorCode, MessageContext clonedSynCtx) {

        // Validate per each MSISDN in the list
        for (Object elem : oList) {
            if (elem instanceof OMElement) {
                String msisdnValue = ((OMElement) elem).getText();

                // Use cloned synCtx for getting OPERATOR_NAME via endpointRetrieverMediator
                clonedSynCtx.setProperty("MSISDN", msisdnValue);
                endpointRetrieverMediator.mediate(clonedSynCtx);

                Object operatorNameObj = clonedSynCtx.getProperty("OPERATOR_NAME");

                // Detach address element when operatorName can't be found or mismatch with given operatorCode
                if (null == operatorNameObj || !(operatorNameObj instanceof String)) {
                    ((OMElement) elem).detach();
                } else {
                    String operatorName = (String) operatorNameObj;

                    if (!operatorName.equalsIgnoreCase(operatorCode)) {
                        ((OMElement) elem).detach();
                    }
                }

                // Clear OPERATOR_NAME value for the next iteration
                clonedSynCtx.setProperty("OPERATOR_NAME", null);
            }
        }
    }
}
