package ee.digit25.detector.domain.transaction;

import ee.digit25.detector.domain.account.AccountValidator;
import ee.digit25.detector.domain.device.DeviceValidator;
import ee.digit25.detector.domain.person.PersonValidator;
import ee.digit25.detector.domain.transaction.common.Transaction;
import ee.digit25.detector.domain.transaction.external.api.TransactionModel;
import ee.digit25.detector.domain.transaction.feature.FindTransactionsFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionValidator {

    private final PersonValidator personValidator;
    private final DeviceValidator deviceValidator;
    private final AccountValidator accountValidator;
    private final FindTransactionsFeature findTransactionsFeature;

    public boolean isLegitimate(TransactionModel transaction) {
        boolean isLegitimate = true;

        isLegitimate &= personValidator.isValid(transaction.getRecipient());
        isLegitimate &= personValidator.isValid(transaction.getSender());
        isLegitimate &= deviceValidator.isValid(transaction.getDeviceMac());
        isLegitimate &= accountValidator.isValidSenderAccount(transaction.getSenderAccount(), transaction.getAmount(), transaction.getSender());
        isLegitimate &= accountValidator.isValidRecipientAccount(transaction.getRecipientAccount(), transaction.getRecipient());
        isLegitimate &= validateNoBurstTransaction(transaction);
        isLegitimate &= validateNoMultideviceTransactions(transaction);
        isLegitimate &= validateValidHistory(transaction);

        return isLegitimate;
    }

    private boolean validateNoBurstTransaction(TransactionModel transaction) {
        LocalDateTime since = LocalDateTime.now().minusSeconds(30);

        long transactionCountSince = findTransactionsFeature.bySenderAndTimestamp(
                transaction.getSender(),
                since
        ).size();

        return countBelowThreshold(transactionCountSince, 10);
    }

    private boolean validateNoMultideviceTransactions(TransactionModel transaction) {
        LocalDateTime since = LocalDateTime.now().minusSeconds(10);

        long differentDeviceCountSince = findTransactionsFeature.bySenderAndTimestamp(
                transaction.getSender(),
                since
        ).stream().map(t -> t.getDevice().getMac()).distinct().count();

        return countBelowThreshold(differentDeviceCountSince, 2);
    }

    private boolean validateValidHistory(TransactionModel transaction) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(1);

        return findTransactionsFeature.bySenderAndTimestamp(
                transaction.getSender(),
                since
        ).stream().allMatch(Transaction::isLegitimate);
    }

    private boolean countBelowThreshold(long count, int threshold) {
        return count < threshold;
    }
}
