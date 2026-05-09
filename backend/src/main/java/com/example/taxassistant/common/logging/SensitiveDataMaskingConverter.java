package com.example.taxassistant.common.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class SensitiveDataMaskingConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return SensitiveDataSanitizer.sanitize(event.getFormattedMessage());
    }
}
