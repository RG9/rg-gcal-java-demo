package dev.rg9.gcal;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZonedDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;

class RemoteGCalApi implements GCalApi {

	private static final Logger log = LoggerFactory.getLogger(RemoteGCalApi.class);

	private static final int MAX_RESULTS = 100;

	private final String calendarId;

	RemoteGCalApi(String calendarId) {
		this.calendarId = calendarId;
	}

	@Override
	public List<GCalEvent> listEvents(ZonedDateTime from, ZonedDateTime to) {
		log.debug("Listing events from: {}, to: {}", from, to);
		try {
			return calendar().events().list(calendarId)
				.setMaxResults(MAX_RESULTS)
				.setTimeMin(new DateTime(from.format(ISO_INSTANT)))
				.setTimeMax(new DateTime(to.format(ISO_INSTANT)))
				.setOrderBy("startTime")
				.setSingleEvents(true)
				.execute()
				.getItems()
				.stream()
				.map(Mappings::gCalEvent)
				.toList();
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot list items from: " + from + ", to: " + to, e);
		}
	}

	@Override
	public void update(GCalEvent gCalEvent) {
		log.debug("Updating {} ...", gCalEvent.id());
		try {
			calendar().events()
				.update(calendarId, gCalEvent.id(), apiEvent(gCalEvent))
				.execute();
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot delete GCalEvent: " + gCalEvent.id(), e);
		}
	}

	Event apiEvent(GCalEvent gCalEvent) {
		var apiEvent = read(gCalEvent);
		return Mappings.apply(gCalEvent, apiEvent);
	}

	Event read(GCalEvent gCalEvent) {
		log.debug("Reading {} ...", gCalEvent.id());
		try {
			return calendar().events()
				.get(calendarId, gCalEvent.id())
				.execute();
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot read GCalEvent: " + gCalEvent.id(), e);
		}
	}

	@Override
	public void delete(GCalEvent gCalEvent) {
		log.debug("Deleting {} ...", gCalEvent);
		try {
			calendar().events()
				.delete(calendarId, gCalEvent.id())
				.execute();
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot delete GCalEvent: " + gCalEvent.id(), e);
		}
	}

	private static Calendar calendar() {
		return CalendarQuickstart.getCalendarService();
	}

}
