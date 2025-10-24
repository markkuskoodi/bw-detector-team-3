package ee.digit25.detector.domain.account;

import ee.digit25.detector.domain.account.external.AccountRequester;
import ee.digit25.detector.domain.account.external.api.AccountModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountValidator {

    private final AccountRequester requester;

    public boolean isValidSenderAccount(String accountNumber, BigDecimal amount, String senderPersonCode, Map<String, AccountModel> accountCache) {
        log.info("Checking if account {} is valid sender account", accountNumber);

        AccountModel account = accountCache.get(accountNumber);
        if (account == null) {
            log.error("Account {} not found in cache", accountNumber);
            return false;
        }

        boolean isValid = true;
        isValid &= !account.getClosed();
        isValid &= senderPersonCode.equals(account.getOwner());
        isValid &= account.getBalance().compareTo(amount) >= 0;

        return isValid;
    }

    public boolean isValidRecipientAccount(String accountNumber, String recipientPersonCode, Map<String, AccountModel> accountCache) {
        log.info("Checking if account {} is valid recipient account", accountNumber);

        AccountModel account = accountCache.get(accountNumber);
        if (account == null) {
            log.error("Account {} not found in cache", accountNumber);
            return false;
        }

        boolean isValid = true;
        isValid &= !account.getClosed();
        isValid &= recipientPersonCode.equals(account.getOwner());

        return isValid;
    }
}
