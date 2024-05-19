package com.cpt.payments.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HttpRequest {
	private String url;
	private String request;
	private Map<String, String> formRequestPayload;
	private HttpMethod httpMethod;
	private HttpHeaders headers;
}
