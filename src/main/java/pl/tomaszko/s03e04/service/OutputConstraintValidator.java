package pl.tomaszko.s03e04.service;

import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import pl.tomaszko.s03e04.config.AppProperties;

@Component
public class OutputConstraintValidator {

    private final int byteMin;
    private final int byteMax;

    public OutputConstraintValidator(AppProperties appProperties) {
        this.byteMin = appProperties.getOutput().getByteMin();
        this.byteMax = appProperties.getOutput().getByteMax();
    }

    public String enforce(String candidate) {
        String value = candidate == null ? "" : candidate.trim();
        if (value.isEmpty()) {
            return clamp("Unable to fulfill command from available data");
        }
        int bytes = utf8Length(value);
        if (bytes >= byteMin && bytes <= byteMax) {
            return value;
        }
        if (bytes > byteMax) {
            return clamp("Result exceeds " + byteMax + " bytes limit");
        }
        return padToMin(value);
    }

    public String overflowNames(String entityLabel, int count) {
        return clamp("Found " + count + " " + entityLabel + " not fitting " + byteMax + " bytes limit");
    }

    public boolean fits(String value) {
        if (value == null) {
            return false;
        }
        int bytes = utf8Length(value);
        return bytes >= byteMin && bytes <= byteMax;
    }

    public int utf8Length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    private String clamp(String message) {
        if (utf8Length(message) > byteMax) {
            byte[] raw = message.getBytes(StandardCharsets.UTF_8);
            return new String(raw, 0, byteMax, StandardCharsets.UTF_8).trim();
        }
        if (utf8Length(message) < byteMin) {
            return padToMin(message);
        }
        return message;
    }

    private String padToMin(String value) {
        StringBuilder sb = new StringBuilder(value);
        while (utf8Length(sb.toString()) < byteMin) {
            sb.append('.');
        }
        return sb.toString();
    }
}
