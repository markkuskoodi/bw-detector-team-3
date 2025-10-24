package ee.digit25.detector.domain.device.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceSpecification {

    public static Specification<Device> macEquals(String mac) {
        return ((root, query, builder) -> builder.equal(root.get(Device_.mac), mac));
    }

    public static Specification<Device> macIn(Set<String> macs) {
        return (root, query, criteriaBuilder) -> root.get(Device_.mac).in(macs);
    }
}
