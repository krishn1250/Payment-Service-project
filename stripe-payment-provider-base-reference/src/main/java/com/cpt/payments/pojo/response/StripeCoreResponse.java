package com.cpt.payments.pojo.response;

import com.cpt.payments.pojo.response.error.ErrorDetails;

import lombok.Data;

@Data
public class StripeCoreResponse {

	private String id;
	private String amount_total;
	private String mode;
	private String payment_method_collection;
	private String payment_status;
	private String status;
	private String url;
	private ErrorDetails error; 
	
}
