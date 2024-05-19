package com.cpt.payments.service;

import com.cpt.payments.pojo.Transaction;
import com.cpt.payments.pojo.request.StripeProviderRequest;
import com.cpt.payments.pojo.response.StripeProviderResponse;

public interface PaymentService {

	StripeProviderResponse initiatePayment(StripeProviderRequest stripeProviderRequest);

	Transaction paymentDetails(Transaction transaction);

}
