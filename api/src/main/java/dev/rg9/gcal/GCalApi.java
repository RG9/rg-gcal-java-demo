package dev.rg9.gcal;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public interface GCalApi {

	static GCalApi primary() {
		return forCalendarId("primary");
	}

	static RemoteGCalApi forCalendarId(String calendarId) {
		return new RemoteGCalApi(calendarId);
	}

	List<GCalEvent> listEvents(ZonedDateTime from, ZonedDateTime to);

	default List<GCalEvent> listEvents(LocalDate fromInclusive, LocalDate toExclusive) {
		ZoneId zone = ZoneId.systemDefault();
		return listEvents(fromInclusive.atStartOfDay(zone), toExclusive.atStartOfDay(zone));
	}

	default List<GCalEvent> listEvents(LocalDate date) {
		return listEvents(date, date.plusDays(1));
	}

	void update(GCalEvent gCalEvent);

	void delete(GCalEvent gCalEvent);
}
