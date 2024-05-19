package com.cpt.payments.consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cpt.payments.service.PaymentService;

@Component
public class SQSlistener {

	private static final Logger LOGGER = LogManager.getLogger(SQSlistener.class);

	@Autowired
	private PaymentService paymentService;

//	@SqsListener(value = "testQueue")
//	public void messageListener(String message) {
//		LogMessage.log(LOGGER, ":: get payment status from SQSlistener channel ::" );
//		paymentService.processGetPaymentDetails();
//	}
}
