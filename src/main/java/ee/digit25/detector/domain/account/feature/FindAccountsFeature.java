package ee.digit25.detector.domain.account.feature;

import ee.digit25.detector.domain.account.common.Account;
import ee.digit25.detector.domain.account.common.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static ee.digit25.detector.domain.account.common.AccountSpecification.numberIn;

@Slf4j
@Service
@RequiredArgsConstructor
public class FindAccountsFeature {

    private final AccountRepository accountRepository;

    public List<Account> byAccountNumbers(Set<String> accountNumbers) {
        return accountRepository.findAll(numberIn(accountNumbers));
    }
}
