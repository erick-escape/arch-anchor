package com.archanchor.services.parsers.php;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The classes PHP itself provides, and the package each is filed under (#85): {@code php}
 * for the core, SPL and the standard extension, which are to a PHP project what
 * {@code java.lang} is to a Java one, and {@code ext-pdo}, {@code ext-date} and so on for
 * the other extensions PHP ships with, under the names Composer gives them. Without it
 * every built-in class would be one dependency, and a class that queries the database
 * would look like one that only throws exceptions.
 * <p>
 * The table is {@code php/platform-classes.tsv}, generated from JetBrains' phpstorm-stubs
 * by {@code src/main/scripts/PhpPlatformClassesGenerator.java}. It is data about the
 * platform, as the JDK's classes are to the Java parser, not knowledge of any library.
 *
 * <pre>
 * platformClasses.find("pdo"); // PlatformClass[qualifiedName=PDO, packageName=ext-pdo]
 * platformClasses.find("App\\Models\\Order"); // empty
 * </pre>
 */
@Component
public class PhpPlatformClasses {

	private static final String TABLE_RESOURCE = "/php/platform-classes.tsv";

	private static final String COMMENT_PREFIX = "#";

	// By name in lower case: PHP compares class names without regard to case.
	private final Map<String, PlatformClass> classesByName = new HashMap<>();

	public PhpPlatformClasses() {
		try (InputStream table = PhpPlatformClasses.class.getResourceAsStream(TABLE_RESOURCE)) {
			if (table == null) {
				throw new IllegalStateException(
						"Expected the table " + TABLE_RESOURCE + " on the classpath, found none");
			}
			new BufferedReader(new InputStreamReader(table, StandardCharsets.UTF_8)).lines().forEach(this::readLine);
		}
		catch (IOException unreadable) {
			throw new IllegalStateException("Could not read the table " + TABLE_RESOURCE, unreadable);
		}
	}

	/** The built-in class of that name, as PHP spells it, if PHP provides one. */
	public Optional<PlatformClass> find(String qualifiedName) {
		return Optional.ofNullable(classesByName.get(qualifiedName.toLowerCase(Locale.ROOT)));
	}

	private void readLine(String line) {
		if (line.isBlank() || line.startsWith(COMMENT_PREFIX)) {
			return;
		}
		String[] columns = line.split("\t");
		if (columns.length != 2) {
			throw new IllegalStateException(
					"Expected '<class><tab><package>' in " + TABLE_RESOURCE + ", got '" + line + "'");
		}
		classesByName.put(columns[0].toLowerCase(Locale.ROOT), new PlatformClass(columns[0], columns[1]));
	}

	/**
	 * A class PHP provides, spelled as PHP spells it, and the package it is filed under.
	 */
	public record PlatformClass(String qualifiedName, String packageName) {
	}

}
