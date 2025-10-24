package ee.digit25.detector.process;

import ee.digit25.detector.domain.account.common.Account;
import ee.digit25.detector.domain.account.common.AccountRepository;
import ee.digit25.detector.domain.account.external.AccountRequester;
import ee.digit25.detector.domain.account.external.api.AccountModel;
import ee.digit25.detector.domain.account.feature.GetOrCreateAccountFeature;
import ee.digit25.detector.domain.device.common.Device;
import ee.digit25.detector.domain.device.common.DeviceRepository;
import ee.digit25.detector.domain.device.external.DeviceRequester;
import ee.digit25.detector.domain.device.external.api.DeviceModel;
import ee.digit25.detector.domain.device.feature.GetOrCreateDeviceFeature;
import ee.digit25.detector.domain.person.common.Person;
import ee.digit25.detector.domain.person.common.PersonRepository;
import ee.digit25.detector.domain.person.external.PersonRequester;
import ee.digit25.detector.domain.person.external.api.PersonModel;
import ee.digit25.detector.domain.person.feature.GetOrCreatePersonFeature;
import ee.digit25.detector.domain.transaction.TransactionValidator;
import ee.digit25.detector.domain.transaction.common.Transaction;
import ee.digit25.detector.domain.transaction.common.TransactionMapper;
import ee.digit25.detector.domain.transaction.external.TransactionRequester;
import ee.digit25.detector.domain.transaction.external.TransactionVerifier;
import ee.digit25.detector.domain.transaction.external.api.TransactionModel;
import ee.digit25.detector.domain.transaction.feature.PersistTransactionFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class Processor {

    private final int TRANSACTION_BATCH_SIZE = 50;
    private final TransactionRequester requester;
    private final TransactionValidator validator;
    private final TransactionVerifier verifier;
    private final PersistTransactionFeature persistTransactionFeature;
    private final TransactionMapper transactionMapper;
    private final PersonRequester personRequester;
    private final AccountRequester accountRequester;
    private final DeviceRequester deviceRequester;
    private final PersonRepository personRepository;
    private final AccountRepository accountRepository;
    private final DeviceRepository deviceRepository;
    private final GetOrCreatePersonFeature getOrCreatePersonFeature;
    private final GetOrCreateAccountFeature getOrCreateAccountFeature;
    private final GetOrCreateDeviceFeature getOrCreateDeviceFeature;
    private final Executor transactionValidationExecutor;


    @Scheduled(fixedDelay = 1000) //Runs every 1000 ms after the last run
    public void process() {
        log.info("Starting to process a batch of transactions of size {}", TRANSACTION_BATCH_SIZE);

        List<TransactionModel> transactions = requester.getUnverified(TRANSACTION_BATCH_SIZE);

        if (transactions.isEmpty()) {
            log.info("No transactions to process");
            return;
        }

        // Step 1: Extract all unique identifiers from the batch
        Set<String> personCodes = new HashSet<>();
        Set<String> accountNumbers = new HashSet<>();
        Set<String> deviceMacs = new HashSet<>();

        for (TransactionModel transaction : transactions) {
            personCodes.add(transaction.getSender());
            personCodes.add(transaction.getRecipient());
            accountNumbers.add(transaction.getSenderAccount());
            accountNumbers.add(transaction.getRecipientAccount());
            deviceMacs.add(transaction.getDeviceMac());
        }

        // Step 2: Bulk fetch all entities using existing bulk API endpoints
        log.info("Bulk fetching {} persons, {} accounts, {} devices",
                personCodes.size(), accountNumbers.size(), deviceMacs.size());

        List<PersonModel> persons = personRequester.get(new ArrayList<>(personCodes));
        List<AccountModel> accounts = accountRequester.get(new ArrayList<>(accountNumbers));
        List<DeviceModel> devices = deviceRequester.get(new ArrayList<>(deviceMacs));

        // Step 3: Create lookup maps for O(1) access
        Map<String, PersonModel> personCache = persons.stream()
                .collect(Collectors.toMap(PersonModel::getPersonCode, p -> p));
        Map<String, AccountModel> accountCache = accounts.stream()
                .collect(Collectors.toMap(AccountModel::getNumber, a -> a));
        Map<String, DeviceModel> deviceCache = devices.stream()
                .collect(Collectors.toMap(DeviceModel::getMac, d -> d));

        // Step 3.5: Resolve or create database entities
        Map<String, Person> personEntityCache = resolvePersonEntities(personCodes);
        Map<String, Account> accountEntityCache = resolveAccountEntities(accountNumbers);
        Map<String, Device> deviceEntityCache = resolveDeviceEntities(deviceMacs);

        // Step 4: Validate all transactions in parallel using async executor
        // Create CompletableFuture for each transaction validation
        log.info("Starting parallel validation of {} transactions", transactions.size());
        List<CompletableFuture<ValidationResult>> validationFutures = new ArrayList<>();

        for (TransactionModel transaction : transactions) {
            // Submit validation task to thread pool
            CompletableFuture<ValidationResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // Perform validation (includes I/O-bound database query)
                    boolean valid = validator.isLegitimate(transaction, personCache, accountCache, deviceCache);

                    // Map to database entity
                    Transaction entity = transactionMapper.toEntity(
                            transaction, valid, personEntityCache, accountEntityCache, deviceEntityCache
                    );

                    // Return successful result
                    return ValidationResult.success(transaction, valid, entity);

                } catch (Exception e) {
                    // Catch any validation errors and return failure result
                    // This prevents one failed validation from breaking the entire batch
                    log.error("Error validating transaction {}: {}", transaction.getId(), e.getMessage(), e);
                    return ValidationResult.failure(transaction, e);
                }
            }, transactionValidationExecutor);

            validationFutures.add(future);
        }

        // Wait for all validations to complete
        // allOf() creates a future that completes when all input futures complete
        CompletableFuture<Void> allValidations = CompletableFuture.allOf(
                validationFutures.toArray(new CompletableFuture[0])
        );

        // Block until all validations are done
        allValidations.join();
        log.info("Parallel validation completed for {} transactions", transactions.size());

        // Collect results from all futures
        // Results are collected in original order to maintain consistency
        List<TransactionModel> validTransactions = new ArrayList<>();
        List<TransactionModel> invalidTransactions = new ArrayList<>();
        List<Transaction> transactionsToSave = new ArrayList<>();

        for (CompletableFuture<ValidationResult> future : validationFutures) {
            ValidationResult result = future.join(); // Safe to call - all futures are complete

            if (result.hasError()) {
                // Validation threw an exception - treat as invalid and skip entity save
                log.warn("Marking transaction {} as invalid due to validation error",
                        result.getTransactionModel().getId());
                invalidTransactions.add(result.getTransactionModel());
                // Don't add to transactionsToSave if entity mapping failed
                continue;
            }

            // Process successful validation
            if (result.isValid()) {
                log.info("Legitimate transaction {}", result.getTransactionModel().getId());
                validTransactions.add(result.getTransactionModel());
            } else {
                log.info("Not legitimate transaction {}", result.getTransactionModel().getId());
                invalidTransactions.add(result.getTransactionModel());
            }

            transactionsToSave.add(result.getTransactionEntity());
        }

        // Step 5: Batch verify/reject calls
        if (!validTransactions.isEmpty()) {
            verifier.verify(validTransactions);
        }
        if (!invalidTransactions.isEmpty()) {
            verifier.reject(invalidTransactions);
        }

        // Step 6: Batch save all transactions
        if (!transactionsToSave.isEmpty()) {
            persistTransactionFeature.saveAll(transactionsToSave);
        }

        log.info("Finished processing a batch of transactions of size {}", transactions.size());
    }

    private Map<String, Person> resolvePersonEntities(Set<String> personCodes) {
        // Query existing persons from database
        List<Person> existingPersons = personRepository.findAll().stream()
                .filter(p -> personCodes.contains(p.getPersonCode()))
                .toList();

        Map<String, Person> result = new HashMap<>();
        for (Person person : existingPersons) {
            result.put(person.getPersonCode(), person);
        }

        // Create missing persons
        for (String personCode : personCodes) {
            if (!result.containsKey(personCode)) {
                Person newPerson = getOrCreatePersonFeature.byPersonCode(personCode);
                result.put(personCode, newPerson);
            }
        }

        return result;
    }

    private Map<String, Account> resolveAccountEntities(Set<String> accountNumbers) {
        List<Account> existingAccounts = accountRepository.findAll().stream()
                .filter(a -> accountNumbers.contains(a.getNumber()))
                .toList();

        Map<String, Account> result = new HashMap<>();
        for (Account account : existingAccounts) {
            result.put(account.getNumber(), account);
        }

        for (String accountNumber : accountNumbers) {
            if (!result.containsKey(accountNumber)) {
                Account newAccount = getOrCreateAccountFeature.byNumber(accountNumber);
                result.put(accountNumber, newAccount);
            }
        }

        return result;
    }

    private Map<String, Device> resolveDeviceEntities(Set<String> deviceMacs) {
        List<Device> existingDevices = deviceRepository.findAll().stream()
                .filter(d -> deviceMacs.contains(d.getMac()))
                .toList();

        Map<String, Device> result = new HashMap<>();
        for (Device device : existingDevices) {
            result.put(device.getMac(), device);
        }

        for (String mac : deviceMacs) {
            if (!result.containsKey(mac)) {
                Device newDevice = getOrCreateDeviceFeature.byMac(mac);
                result.put(mac, newDevice);
            }
        }

        return result;
    }
}
