package com.example.taxassistant.common.logging;

import ch.qos.logback.classic.pattern.ExtendedThrowableProxyConverter;
import ch.qos.logback.classic.spi.IThrowableProxy;

public class SensitiveThrowableProxyConverter extends ExtendedThrowableProxyConverter {

    @Override
    protected String throwableProxyToString(IThrowableProxy tp) {
        return SensitiveDataSanitizer.sanitize(super.throwableProxyToString(tp));
    }
}
