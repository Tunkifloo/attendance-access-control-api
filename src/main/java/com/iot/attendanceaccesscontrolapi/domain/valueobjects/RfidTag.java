package com.iot.attendanceaccesscontrolapi.domain.valueobjects;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public class RfidTag {

    private final String uid;

    public RfidTag(String uid) {
        if (uid == null || uid.isBlank()) {
            throw new IllegalArgumentException("RFID UID cannot be empty");
        }
        this.uid = uid.toUpperCase().trim();
    }

    public static RfidTag of(String uid) {
        return new RfidTag(uid);
    }

    public static RfidTag fromBytes(String hexString) {
        return new RfidTag(hexString.toUpperCase().trim());
    }
}