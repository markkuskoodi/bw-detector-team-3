package ee.digit25.detector.domain.transaction.feature;

import ee.digit25.detector.domain.transaction.common.Transaction;
import ee.digit25.detector.domain.transaction.common.TransactionRepository;
import ee.digit25.detector.domain.transaction.common.Transaction_;
import ee.digit25.detector.domain.person.common.Person_;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FindTransactionsFeature {

    private final TransactionRepository repository;

    public List<Transaction> bySender(String sender) {
        log.info("Fetching transaction history by sender: {}", sender);

        Specification<Transaction> spec = (root, query, builder) -> {
            // Eager fetch relationships to avoid N+1 queries
            root.fetch(Transaction_.sender, JoinType.LEFT);
            root.fetch(Transaction_.device, JoinType.LEFT);

            // WHERE clause: filter by sender's person code
            return builder.equal(
                root.get(Transaction_.sender).get(Person_.personCode),
                sender
            );
        };

        return repository.findAll(spec);
    }
}
