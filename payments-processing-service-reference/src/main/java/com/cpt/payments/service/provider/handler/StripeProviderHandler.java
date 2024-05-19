package com.cpt.payments.service.provider.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.cpt.payments.constants.ErrorCodeEnum;
import com.cpt.payments.constants.TransactionStatusEnum;
import com.cpt.payments.dao.TransactionDao;
import com.cpt.payments.dto.Transaction;
import com.cpt.payments.exception.PaymentProcessingException;
import com.cpt.payments.http.HttpRequest;
import com.cpt.payments.http.HttpRestTemplateEngine;
import com.cpt.payments.pojo.PaymentResponse;
import com.cpt.payments.pojo.ProcessingServiceRequest;
import com.cpt.payments.pojo.provider.request.StripeProviderRequest;
import com.cpt.payments.pojo.provider.request.TrustlyProviderRequest;
import com.cpt.payments.pojo.provider.response.StripeProviderResponse;
import com.cpt.payments.pojo.provider.response.ProviderServiceErrorResponse;
import com.cpt.payments.pojo.provider.response.TrustlyProviderResponse;
import com.cpt.payments.service.PaymentStatusService;
import com.cpt.payments.service.ProviderHandler;
import com.cpt.payments.util.LogMessage;
import com.google.gson.Gson;

@Component
public class StripeProviderHandler implements ProviderHandler {

	private static final Logger LOGGER = LogManager.getLogger(StripeProviderHandler.class);

	@Value("${stripe.provider.service.process.payment}")
	private String processPaymentUrl;
	
	@Value("${stripe.provider.service.get.payment.details}")
	private String getPaymentDetailsUrl;

	@Autowired
	private HttpRestTemplateEngine httpRestTemplateEngine;

	@Autowired
	private PaymentStatusService paymentStatusServiceImpl;

	@Autowired
	private TransactionDao transactionDaoImpl;
	
	@Override
	public PaymentResponse processPayment(Transaction transaction, ProcessingServiceRequest processingServiceRequest) {
		LogMessage.log(LOGGER, " processing trustly payment ");
		Gson gson = new Gson();

		StripeProviderRequest stripeProviderRequest = StripeProviderRequest.builder()
				.amount(transaction.getAmount())
				.currency(transaction.getCurrency())
				.productDescription(processingServiceRequest.getProductDescription())
				.quantity(1L)
				.successUrl(processingServiceRequest.getSuccessUrl())
				.transactionReference(transaction.getTxnReference())
				.build();

		LogMessage.log(LOGGER, " stripe provider request -> " + stripeProviderRequest);

		HttpRequest httpRequest = HttpRequest.builder().httpMethod(HttpMethod.POST)
				.request(gson.toJson(stripeProviderRequest)).url(processPaymentUrl).build();

		LogMessage.log(LOGGER, " updating transaction status to pending ");
		transaction.setTxnStatusId(TransactionStatusEnum.PENDING.getId());
		transaction = paymentStatusServiceImpl.updatePaymentStatus(transaction);

		ResponseEntity<String> response = httpRestTemplateEngine.execute(httpRequest);
		if (null == response || null == response.getBody()) {//Failed to make API call
			LogMessage.log(LOGGER, " payment creation at provider failed -> " + response);
			throw new PaymentProcessingException(HttpStatus.INTERNAL_SERVER_ERROR,
					ErrorCodeEnum.FAILED_TO_CREATE_TRANSACTION.getErrorCode(),
					ErrorCodeEnum.FAILED_TO_CREATE_TRANSACTION.getErrorMessage());
		}

		if(200 != response.getStatusCodeValue()) {// TODO For 3rdParty error record value into DB.
			handleNon200Response(gson, response); // The method throws Exception & flow would stop
		}

		// SUCCESS processing
		StripeProviderResponse stripeProviderResponse = gson.fromJson(response.getBody(),
				StripeProviderResponse.class);
		transaction.setProviderReference(stripeProviderResponse.getPaymentId());
		transactionDaoImpl.updateProviderReference(transaction);

		PaymentResponse paymentResponse = PaymentResponse.builder().paymentReference(transaction.getTxnReference())
				.redirectUrl(stripeProviderResponse.getRedirectUrl()).build();
		LogMessage.log(LOGGER, " payment response is -> " + paymentResponse);

		return paymentResponse;
	}

	private void handleNon200Response(Gson gson, ResponseEntity<String> response) {
		// Error response
		ProviderServiceErrorResponse errorResponse = gson.fromJson(response.getBody(),
				ProviderServiceErrorResponse.class);

		if(errorResponse.isTpProviderError()) { // ThirdParty Trustly Error
			LogMessage.log(LOGGER, " INTERNAL payment creation at provider failed -> " + response);

			// TODO store the Trustly errorcode & Error Message in DB

			//3rdParty error needs to be mapped with our system error & returned.
			throw new PaymentProcessingException(HttpStatus.BAD_GATEWAY,
					ErrorCodeEnum.TP_TRUSTLY_ERROR.getErrorCode(),
					ErrorCodeEnum.TP_TRUSTLY_ERROR.getErrorMessage());

		} else {// Trustly Provider Service error, should be returned back to invoker
			LogMessage.log(LOGGER, " INTERNAL payment creation at provider failed -> " + response);
			throw new PaymentProcessingException(HttpStatus.BAD_GATEWAY,
					errorResponse.getErrorCode(),
					errorResponse.getErrorMessage());
		}
	}

	@Override
	public void processGetPaymentDetails(Transaction transaction) {
		LogMessage.log(LOGGER, " stripe provider request -> " + transaction);
		Gson gson = new Gson();
		
		if(null == transaction.getProviderReference() || transaction.getProviderReference().isBlank()) {
			LogMessage.log(LOGGER, "Failing transaction beacuse we do not have provider reference --> "+transaction.getTxnReference());
			transaction.setTxnStatusId(TransactionStatusEnum.FAILED.getId());
			paymentStatusServiceImpl.updatePaymentStatus(transaction);
		}
		
		HttpRequest httpRequest = HttpRequest.builder().httpMethod(HttpMethod.POST)
				.request(gson.toJson(transaction)).url(getPaymentDetailsUrl).build();

		LogMessage.log(LOGGER, " updating transaction status to pending ");

		transactionDaoImpl.updateRetryCount(transaction);
		ResponseEntity<String> response = httpRestTemplateEngine.execute(httpRequest);
		if (null == response || null == response.getBody()) {//Failed to make API call
			LogMessage.log(LOGGER, " payment creation at provider failed -> " + response);
			throw new PaymentProcessingException(HttpStatus.INTERNAL_SERVER_ERROR,
					ErrorCodeEnum.FAILED_TO_CREATE_TRANSACTION.getErrorCode(),
					ErrorCodeEnum.FAILED_TO_CREATE_TRANSACTION.getErrorMessage());
		}
		
		if(200 == response.getStatusCode().value()) {
			transaction = paymentStatusServiceImpl.updatePaymentStatus(
					gson.fromJson(response.getBody(), Transaction.class));
		}
		
		
	}

}
