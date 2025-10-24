package ee.digit25.detector.domain.account.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AccountSpecification {

    public static Specification<Account> numberEquals(String number) {
        return ((root, query, builder) -> builder.equal(root.get(Account_.number), number));
    }

    public static Specification<Account> numberIn(Set<String> numbers) {
        return (root, query, builder) -> root.get(Account_.number).in(numbers);
    }
}
