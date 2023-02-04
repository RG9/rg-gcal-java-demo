package dev.rg9.gcal;

import java.time.ZonedDateTime;

import org.immutables.value.Value;

@Value.Immutable
public interface GCalEvent {

	String id();

	String summary();

	ZonedDateTime start();

	ZonedDateTime end();

	boolean recurring();

}
