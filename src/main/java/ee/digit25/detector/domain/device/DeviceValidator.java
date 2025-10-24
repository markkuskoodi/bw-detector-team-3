package ee.digit25.detector.domain.device;

import ee.digit25.detector.domain.device.external.DeviceRequester;
import ee.digit25.detector.domain.device.external.api.DeviceModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceValidator {

    private final DeviceRequester requester;

    public boolean isValid(String mac, Map<String, DeviceModel> deviceCache) {
        log.info("Validating device {}", mac);

        DeviceModel device = deviceCache.get(mac);
        if (device == null) {
            log.error("Device {} not found in cache", mac);
            return false;
        }

        return !device.getIsBlacklisted();
    }
}
