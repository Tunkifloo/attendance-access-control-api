package com.iot.attendance.domain.valueobjects;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public class FingerprintId {

    private final Integer value;

    public FingerprintId(Integer value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Fingerprint ID must be positive");
        }
        this.value = value;
    }

    public static FingerprintId of(Integer value) {
        return new FingerprintId(value);
    }
}
