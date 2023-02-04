package org.example;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.stream.Collectors;

import de.cronn.assertions.validationfile.util.MarkdownTable;
import dev.rg9.gcal.GCalApi;
import dev.rg9.gcal.GCalEvent;
import dev.rg9.gcal.ImmutableGCalEvent;

public class ReviewLastMonth {

	static final LocalDate START_OF_LAST_MONTH = YearMonth.now().minusMonths(1).atDay(1);

	List<GCalEvent> events;

	GCalApi gcal = GCalApi.primary();

	public static void main(String[] args) {
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG");

		ReviewLastMonth review = new ReviewLastMonth();

		System.out.println("Options:");
		System.out.println("p - print tasks list");
		System.out.println("u{taskIndex1,..} - updates task by adding '+ ' at start");
		System.out.println("d{taskIndex1,..} - deletes task and copies summary to clipboard");

		Scanner scanner = new Scanner(System.in);
		try {
			while (true) {
				System.out.println("Waiting for input ...");
				String line = scanner.nextLine();
				if (line.startsWith("p")) {
					review.readAndPrintEvents();
				} else if (line.startsWith("d")) {
					List<Integer> indexes = extractIndexes(line);
					if (review.deleteWithPrompt(indexes, scanner)) {
						review.removeAll(indexes);
					}
				} else if (line.startsWith("u")) {
					List<Integer> indexes = extractIndexes(line);
					if (review.updateWithPrompt(indexes, scanner)) {
						review.removeAll(indexes);
					}
				} else {
					System.err.println("Unknown option: " + line);
				}
			}
		} catch (IllegalStateException | NoSuchElementException e) {
			System.out.println("System.in was closed; exiting");
		}
	}

	private static List<Integer> extractIndexes(String line) {
		return Arrays.stream(line.substring(1).split(","))
			.map(Integer::parseInt)
			.collect(Collectors.toList());
	}

	private static void addToClipboard(String theString) {
		StringSelection selection = new StringSelection(theString);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(selection, selection);
	}

	private List<GCalEvent> collectEvents(List<Integer> indexes) {
		List<GCalEvent> actionable = new ArrayList<>();
		for (Integer integer : indexes) {
			actionable.add(events.get(integer));
		}
		return actionable;
	}

	private void readAndPrintEvents() {
		events = gcal.listEvents(START_OF_LAST_MONTH, LocalDate.now()).stream()
			.filter(e -> !e.recurring()) // TODO handle recurring
			.filter(e -> !e.summary().startsWith("+ "))
			.sorted(Comparator.comparing(GCalEvent::start))
			.collect(Collectors.toList());

		printEvents();
	}

	private void printEvents() {
		MarkdownTable table = new MarkdownTable(Arrays.asList("index", "date", "summary"));
		for (int i = 0; i < events.size(); i++) {
			GCalEvent event = events.get(i);
			table.addCells(i, event.start().toLocalDate(), event.summary());
			table.nextRow();
		}
		System.out.println(table);
	}

	private void removeAll(List<Integer> indexes) {
		events.removeAll(collectEvents(indexes));
		printEvents();
	}

	private boolean deleteWithPrompt(List<Integer> indexes, Scanner scanner) {
		List<GCalEvent> actionable = collectEvents(indexes);
		if (prompt(actionable, scanner, "Deleting")) {

			for (GCalEvent event : actionable) {
				gcal.delete(event);
			}

			System.out.println("Deleted and copied to clipboard.");
			return true;
		}
		return false;
	}

	private boolean updateWithPrompt(List<Integer> indexes, Scanner scanner) {
		List<GCalEvent> actionable = collectEvents(indexes);
		if (prompt(actionable, scanner, "Updating")) {
			for (GCalEvent event : actionable) {
				gcal.update(ImmutableGCalEvent.copyOf(event)
					.withSummary("+ " + event.summary()));
			}

			System.out.println("Updated and copied to clipboard.");
			return true;
		}
		return false;
	}

	private boolean prompt(List<GCalEvent> actionable, Scanner scanner, String actionName) {
		System.out.println(actionName + "..");
		String forUpdateString = actionable.stream()
			.map(event -> event.start().toLocalDate() + ": " + event.summary())
			.collect(Collectors.joining("\n"));
		System.out.println(forUpdateString);
		System.out.println("Are you sure? [y|n]");
		String yesNoLine = scanner.nextLine();
		boolean y = yesNoLine.equals("y");
		if (y) {
			addToClipboard(forUpdateString);
		} else {
			System.out.println(actionName + " aborted.");
		}
		return y;
	}

}
