package ee.digit25.detector.domain.device.feature;

import ee.digit25.detector.domain.device.common.Device;
import ee.digit25.detector.domain.device.common.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static ee.digit25.detector.domain.device.common.DeviceSpecification.macIn;

@Slf4j
@Service
@RequiredArgsConstructor
public class FindDevicesFeature {

    private final DeviceRepository deviceRepository;

    public List<Device> byDeviceMacs(Set<String> macs) {
        return deviceRepository.findAll(macIn(macs));
    }
}
