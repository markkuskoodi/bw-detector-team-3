package ee.digit25.detector.domain.person.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PersonSpecification {

    public static Specification<Person> personCodeEquals(String personCode) {
        return ((root, query, builder) -> builder.equal(root.get(Person_.personCode), personCode));
    }

    public static Specification<Person> personCodeIn(Set<String> personCodes) {
        return ((root, query, builder) -> root.get(Person_.personCode).in(personCodes));
    }
}
