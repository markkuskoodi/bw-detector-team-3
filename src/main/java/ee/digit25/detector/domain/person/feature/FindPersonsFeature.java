package ee.digit25.detector.domain.person.feature;

import ee.digit25.detector.domain.person.common.Person;
import ee.digit25.detector.domain.person.common.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static ee.digit25.detector.domain.person.common.PersonSpecification.personCodeIn;

@Slf4j
@Service
@RequiredArgsConstructor
public class FindPersonsFeature {

    private final PersonRepository personRepository;

    public List<Person> byPersonCodes(Set<String> personCodes) {
        return personRepository.findAll(personCodeIn(personCodes));
    }
}
