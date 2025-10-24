package ee.digit25.detector.domain.device;

import ee.digit25.detector.domain.device.external.DeviceRequester;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceValidator {

    private final DeviceRequester requester;

    public boolean isValid(String mac) {
        log.info("Validating device {}", mac);

        return !isBlacklisted(mac);
    }

    public boolean isBlacklisted(String mac) {
        log.info("Starting to check if device is blacklisted");

        return requester.get(mac).getIsBlacklisted();
    }
}
