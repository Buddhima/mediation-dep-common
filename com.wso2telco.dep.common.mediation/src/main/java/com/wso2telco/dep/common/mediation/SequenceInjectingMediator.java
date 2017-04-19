package com.wso2telco.dep.common.mediation;

import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.util.MessageHelper;

public class SequenceInjectingMediator extends AbstractMediator {

	private String injectingSequence;

	public String getInjectingSequence() {
		return injectingSequence;
	}

	public void setInjectingSequence(String injectingSequence) {
		this.injectingSequence = injectingSequence;
	}

	private void setErrorInContext(MessageContext synContext, String messageId,
								   String errorText, String errorVariable, String httpStatusCode,
								   String exceptionType) {

		synContext.setProperty("messageId", messageId);
		synContext.setProperty("errorText", errorText);
		synContext.setProperty("errorVariable", errorVariable);
		synContext.setProperty("httpStatusCode", httpStatusCode);
		synContext.setProperty("exceptionType", exceptionType);
	}

	public boolean mediate(MessageContext synContext) {

		try {

			if (getInjectingSequence() == null || getInjectingSequence().equalsIgnoreCase("")) {
				throw new Exception("Injecting sequence name is missing");
			}

			SequenceMediator injectingSequence = (SequenceMediator) synContext.getSequence(getInjectingSequence());

			if (injectingSequence == null) {
				throw new Exception("Injecting sequence cannot found");
			}

			MessageContext newContxt = MessageHelper.cloneMessageContext(synContext);

			boolean result = injectingSequence.mediate(newContxt);

			// Adds keys which are newly added to cloned message context
			for (Object keyObj : newContxt.getPropertyKeySet()) {
				String key = (String) keyObj;

				if (!synContext.getPropertyKeySet().contains(keyObj)) {
					synContext.setProperty(key, newContxt.getProperty(key));
				}
			}

			return result;

		} catch (Exception e) {

			log.error("error in SequenceInjectingMediator mediate : "
					+ e.getMessage());
			setErrorInContext(
					synContext,
					"SVC0001",
					"A service error occurred. Error code is %1",
					"An internal service error has occured. Please try again later.",
					"500", "SERVICE_EXCEPTION");
			synContext.setProperty("INTERNAL_ERROR", "true");
		}

		return true;
	}
}
