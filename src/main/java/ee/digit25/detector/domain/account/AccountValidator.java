package ee.digit25.detector.domain.account;

import ee.digit25.detector.domain.account.external.AccountRequester;
import ee.digit25.detector.domain.account.external.api.AccountModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountValidator {

    private final AccountRequester requester;

    public boolean isValidSenderAccount(String accountNumber, BigDecimal amount, String senderPersonCode) {
        log.info("Checking if account {} is valid sender account", accountNumber);
        boolean isValid = true;

        AccountModel account = requester.get(accountNumber);

        isValid &= !isClosed(account);
        isValid &= isOwner(account, senderPersonCode);
        isValid &= hasBalance(account, amount);

        return isValid;
    }

    public boolean isValidRecipientAccount(String accountNumber, String recipientPersonCode) {
        log.info("Checking if account {} is valid recipient account", accountNumber);
        boolean isValid = true;

        AccountModel account = requester.get(accountNumber);

        isValid &= !isClosed(account);
        isValid &= isOwner(account, recipientPersonCode);

        return isValid;
    }

    private boolean isOwner(AccountModel account, String senderPersonCode) {
        return senderPersonCode.equals(account.getOwner());
    }

    private boolean hasBalance(AccountModel account, BigDecimal amount) {
        return account.getBalance().compareTo(amount) >= 0;
    }

    private boolean isClosed(AccountModel account) {
        return account.getClosed();
    }
}
