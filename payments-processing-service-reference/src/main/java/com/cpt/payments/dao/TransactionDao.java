package com.cpt.payments.dao;

import java.util.List;

import com.cpt.payments.dto.Transaction;

public interface TransactionDao {
	public Transaction createTransaction(Transaction transaction);

	public boolean updateTransaction(Transaction transaction);

	public Transaction getTransactionById(long transactionId);

	public void updateProviderReference(Transaction transaction);

	public Transaction getTransactionByProviderReference(String paymentId);

	public void updateProviderCodeAndMessage(Transaction transaction);

	public List<Transaction> fetchAllTransactionsForReconcilation();

	public void updateRetryCount(Transaction transaction);
}
