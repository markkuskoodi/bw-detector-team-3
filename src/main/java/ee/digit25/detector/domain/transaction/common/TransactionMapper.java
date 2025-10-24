package ee.digit25.detector.domain.transaction.common;

import ee.digit25.detector.domain.account.common.Account;
import ee.digit25.detector.domain.account.feature.GetOrCreateAccountFeature;
import ee.digit25.detector.domain.device.common.Device;
import ee.digit25.detector.domain.device.feature.GetOrCreateDeviceFeature;
import ee.digit25.detector.domain.person.common.Person;
import ee.digit25.detector.domain.person.feature.GetOrCreatePersonFeature;
import ee.digit25.detector.domain.transaction.external.api.TransactionModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TransactionMapper {

    private final GetOrCreatePersonFeature getOrCreatePersonFeature;
    private final GetOrCreateAccountFeature getOrCreateAccountFeature;
    private final GetOrCreateDeviceFeature getOrCreateDeviceFeature;

    public Transaction toEntity(TransactionModel model, boolean legitimate) {
        Transaction transaction = new Transaction();

        transaction.setLegitimate(legitimate);
        transaction.setAmount(model.getAmount());
        transaction.setSender(getOrCreatePersonFeature.byPersonCode(model.getSender()));
        transaction.setSenderAccount(getOrCreateAccountFeature.byNumber(model.getSenderAccount()));
        transaction.setRecipient(getOrCreatePersonFeature.byPersonCode(model.getRecipient()));
        transaction.setRecipientAccount(getOrCreateAccountFeature.byNumber(model.getRecipientAccount()));
        transaction.setDevice(getOrCreateDeviceFeature.byMac(model.getDeviceMac()));
        transaction.setTimestamp(model.getTimestamp());
        transaction.setDeadline(model.getDeadline());

        return transaction;
    }

    public Transaction toEntity(TransactionModel model, boolean legitimate,
                                Map<String, Person> personCache,
                                Map<String, Account> accountCache,
                                Map<String, Device> deviceCache) {

        Person sender = personCache.get(model.getSender());
        Account senderAccount = accountCache.get(model.getSenderAccount());
        Person recipient = personCache.get(model.getRecipient());
        Account recipientAccount = accountCache.get(model.getRecipientAccount());
        Device device = deviceCache.get(model.getDeviceMac());

        Transaction transaction = new Transaction();
        transaction.setLegitimate(legitimate);
        transaction.setAmount(model.getAmount());
        transaction.setSender(sender);
        transaction.setSenderAccount(senderAccount);
        transaction.setRecipient(recipient);
        transaction.setRecipientAccount(recipientAccount);
        transaction.setDevice(device);
        transaction.setTimestamp(model.getTimestamp());
        transaction.setDeadline(model.getDeadline());

        return transaction;
    }
}
