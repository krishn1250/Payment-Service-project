package com.cpt.payments.controller;

import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cpt.payments.adapter.StripeNotificationAdapter;
import com.cpt.payments.constants.Endpoints;
import com.cpt.payments.util.LogMessage;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(Endpoints.STRIPE)
public class StripeNotificationController {

	private static final Logger LOGGER = LogManager.getLogger(TrustlyNotificationController.class);

	@Autowired
	private HttpServletRequest httpServletRequest;
	
	@Autowired
	private StripeNotificationAdapter stripeNotificationAdapter;

	@PostMapping(value = Endpoints.NOTIFICATION)
	public ResponseEntity<String> processNotification() {
		LogMessage.setLogMessagePrefix("/STRIPE_NOTIFICATION");

		try {
			String notificationData = httpServletRequest.getReader().lines()
					.collect(Collectors.joining(System.lineSeparator()));
			LogMessage.log(LOGGER, " stripe webhook is  " + notificationData);
			stripeNotificationAdapter.processNotification(notificationData);
			return new ResponseEntity<String>("OK", HttpStatus.OK);
		} catch (Exception e) {
			LogMessage.log(LOGGER, "EXception in webhook " + e.getMessage());
			LogMessage.logException(LOGGER, e);
			return new ResponseEntity<String>("OK", HttpStatus.OK);
		}
	}
}
