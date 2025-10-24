package ee.digit25.detector.domain.transaction;

import ee.digit25.detector.domain.account.AccountValidator;
import ee.digit25.detector.domain.account.external.api.AccountModel;
import ee.digit25.detector.domain.device.DeviceValidator;
import ee.digit25.detector.domain.device.external.api.DeviceModel;
import ee.digit25.detector.domain.person.PersonValidator;
import ee.digit25.detector.domain.person.external.api.PersonModel;
import ee.digit25.detector.domain.transaction.common.Transaction;
import ee.digit25.detector.domain.transaction.external.api.TransactionModel;
import ee.digit25.detector.domain.transaction.feature.FindTransactionsFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionValidator {

    private final PersonValidator personValidator;
    private final DeviceValidator deviceValidator;
    private final AccountValidator accountValidator;
    private final FindTransactionsFeature findTransactionsFeature;

    public boolean isLegitimate(TransactionModel transaction,
                                Map<String, PersonModel> personCache,
                                Map<String, AccountModel> accountCache,
                                Map<String, DeviceModel> deviceCache) {
        boolean isLegitimate = true;

        isLegitimate &= personValidator.isValid(transaction.getRecipient(), personCache);
        isLegitimate &= personValidator.isValid(transaction.getSender(), personCache);
        isLegitimate &= deviceValidator.isValid(transaction.getDeviceMac(), deviceCache);
        isLegitimate &= accountValidator.isValidSenderAccount(transaction.getSenderAccount(), transaction.getAmount(), transaction.getSender(), accountCache);
        isLegitimate &= accountValidator.isValidRecipientAccount(transaction.getRecipientAccount(), transaction.getRecipient(), accountCache);

        // Fetch transaction history ONCE
        List<Transaction> senderHistory = findTransactionsFeature.bySender(transaction.getSender());

        isLegitimate &= validateNoBurstTransaction(transaction, senderHistory);
        isLegitimate &= validateNoMultideviceTransactions(transaction, senderHistory);
        isLegitimate &= validateValidHistory(transaction, senderHistory);

        return isLegitimate;
    }

    private boolean validateNoBurstTransaction(TransactionModel transaction, List<Transaction> senderHistory) {
        LocalDateTime since = LocalDateTime.now().minusSeconds(30);

        long transactionCountSince = senderHistory.stream()
                .filter(t -> t.getTimestamp().isAfter(since))
                .count();

        return countBelowThreshold(transactionCountSince, 10);
    }

    private boolean validateNoMultideviceTransactions(TransactionModel transaction, List<Transaction> senderHistory) {
        LocalDateTime since = LocalDateTime.now().minusSeconds(10);

        long differentDeviceCountSince = senderHistory.stream()
                .filter(t -> t.getTimestamp().isAfter(since))
                .map(t -> t.getDevice().getMac())
                .distinct()
                .count();

        return countBelowThreshold(differentDeviceCountSince, 2);
    }

    private boolean validateValidHistory(TransactionModel transaction, List<Transaction> senderHistory) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(1);

        return senderHistory.stream()
                .filter(t -> t.getTimestamp().isAfter(since))
                .allMatch(Transaction::isLegitimate);
    }

    private boolean countBelowThreshold(long count, int threshold) {
        return count < threshold;
    }
}
