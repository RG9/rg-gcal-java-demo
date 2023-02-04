package dev.rg9.gcal;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

interface Mappings {

	static GCalEvent gCalEvent(Event event) {
		return ImmutableGCalEvent.builder()
			.id(event.getId())
			.summary(event.getSummary())
			.start(zonedDateTime(event.getStart()))
			.end(zonedDateTime(event.getEnd()))
			.recurring(event.getRecurringEventId() != null)
			.build();
	}

	static ZonedDateTime zonedDateTime(EventDateTime date) {
		if (date.getDate() != null) {
			var localDate = LocalDate.parse(date.getDate().toStringRfc3339());
			return ZonedDateTime.of(localDate, LocalTime.MIN, ZoneOffset.UTC);
		}
		return ZonedDateTime.parse(date.getDateTime().toStringRfc3339());
	}

	static Event apply(GCalEvent gCalEvent, Event apiEvent) {
		apiEvent.setSummary(gCalEvent.summary());
		apiEvent.setStart(eventDateTime(gCalEvent.start()));
		apiEvent.setEnd(eventDateTime(gCalEvent.end()));
		return apiEvent;
	}

	static EventDateTime eventDateTime(ZonedDateTime dateTime) {
		EventDateTime eventDateTime = new EventDateTime();
		eventDateTime.setDateTime(
			DateTime.parseRfc3339(
				dateTime.format(ISO_DATE_TIME)));
		return eventDateTime;
	}

}
