package com.cpt.payments.service.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.cpt.payments.constants.ErrorCodeEnum;
import com.cpt.payments.dao.ProviderRequestResponseDao;
import com.cpt.payments.dto.ProviderRequestResponseDto;
import com.cpt.payments.exception.PaymentProcessException;
import com.cpt.payments.http.HttpRequest;
import com.cpt.payments.http.HttpRestTemplateEngine;
import com.cpt.payments.pojo.Transaction;
import com.cpt.payments.service.formatter.request.ExpireSessionRequestHandler;
import com.cpt.payments.service.formatter.response.ExpireSessionResponseHandler;
import com.cpt.payments.util.LogMessage;
import com.google.gson.Gson;

@Component
public class PaymentServiceHelper {

	private static final Logger LOGGER = LogManager.getLogger(PaymentServiceHelper.class);

	@Autowired
	private ProviderRequestResponseDao providerRequestResponseDao;

	@Autowired
	private Gson gson;
	
	@Autowired
	private ExpireSessionRequestHandler expireSessionRequestHandler;
	
	@Autowired
	private ExpireSessionResponseHandler expireSessionResponseHandler;
	
	@Autowired
	private HttpRestTemplateEngine httpRestTemplateEngine;
	
	public void saveProviderRequestResponse(String transactionReference, HttpRequest httpRequest,
			ResponseEntity<String> response) {
		LogMessage.log(LOGGER, ":: writing request and response log ::"+transactionReference);
		ProviderRequestResponseDto providerRequestResponseDto = ProviderRequestResponseDto.builder()
				.transactionReference(transactionReference).request(gson.toJson(httpRequest))
				.response(response.getBody()).status(response.getStatusCode().value()).build();
		providerRequestResponseDao.saveProviderRequestResponse(providerRequestResponseDto);
	}

	public void expireStripeSession(Transaction transaction) {
		HttpRequest httpRequest = expireSessionRequestHandler.prepareRequest(transaction);
		ResponseEntity<String> response = httpRestTemplateEngine.execute(httpRequest);
		if (null == response) {
			LogMessage.log(LOGGER, " failed to connect to trustly provider -> " + response);
			throw new PaymentProcessException(HttpStatus.BAD_REQUEST,
					ErrorCodeEnum.FAILED_TO_CONNECT_TO_STRIPE.getErrorCode(),
					ErrorCodeEnum.FAILED_TO_CONNECT_TO_STRIPE.getErrorMessage());
		}
		saveProviderRequestResponse(transaction.getTxnReference(), httpRequest, response);
		
		expireSessionResponseHandler.processResponse(response, transaction);
		LogMessage.log(LOGGER, " :: expired transaction data is -> "+transaction);
	}

}
