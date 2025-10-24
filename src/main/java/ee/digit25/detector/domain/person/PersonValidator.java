package ee.digit25.detector.domain.person;

import ee.digit25.detector.domain.person.external.PersonRequester;
import ee.digit25.detector.domain.person.external.api.PersonModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonValidator {

    private final PersonRequester requester;

    private boolean hasWarrantIssued(PersonModel person) {
        return person.getWarrantIssued();
    }

    private boolean hasContract(PersonModel person) {
        return person.getHasContract();
    }

    private boolean isBlacklisted(PersonModel person) {
        return person.getBlacklisted();
    }

    private PersonModel getPerson(String personCode) {
        return requester.get(personCode);
    }

    public boolean isValid(String personCode) {
        log.info("Validating person with code ({})", personCode);
        PersonModel person = getPerson(personCode);
        return !hasWarrantIssued(person) && hasContract(person) && !isBlacklisted(person);
    }
}