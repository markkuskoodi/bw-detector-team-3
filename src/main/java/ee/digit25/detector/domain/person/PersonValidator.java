package ee.digit25.detector.domain.person;

import ee.digit25.detector.domain.person.external.PersonRequester;
import ee.digit25.detector.domain.person.external.api.PersonModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonValidator {

    private final PersonRequester requester;

    public boolean isValid(String personCode, Map<String, PersonModel> personCache) {
        log.info("Checking if person {} is valid", personCode);

        PersonModel person = personCache.get(personCode);
        if (person == null) {
            log.error("Person {} not found in cache", personCode);
            return false;
        }

        boolean isValid = true;
        isValid &= !person.getWarrantIssued();
        isValid &= person.getHasContract();
        isValid &= !person.getBlacklisted();

        return isValid;
    }
}
