package ee.digit25.detector.domain.transaction.feature;

import ee.digit25.detector.domain.transaction.common.Transaction;
import ee.digit25.detector.domain.transaction.common.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static ee.digit25.detector.domain.transaction.common.TransactionSpecification.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FindTransactionsFeature {

    private final TransactionRepository repository;

    public List<Transaction> bySenderAndTimestamp(String sender, LocalDateTime timestamp) {
        log.info("Fetching transaction history by sender: {} and timestamp: {}", sender, timestamp);

        return repository.findAll(
                senderEquals(sender)
                        .and(timestampIsAfter(timestamp))
        );
    }
}
