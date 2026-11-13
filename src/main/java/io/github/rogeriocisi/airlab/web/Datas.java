package io.github.rogeriocisi.airlab.web;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * Date formatting for the templates, as {@code ${@datas.utc(instant)}}. Always UTC, and said so
 * on the page: the audit trail is in UTC, and a panel in another zone would not match it.
 */
@Component("datas")
class Datas {

    private static final DateTimeFormatter MINUTE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneOffset.UTC);

    public String utc(Instant instant) {
        return instant == null ? "—" : MINUTE.format(instant);
    }

    public String dia(Instant instant) {
        return instant == null ? "—" : DAY.format(instant);
    }
}
