package com.archanchor.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the package a Java source file declares, without parsing it: comments are
 * removed, then the {@code package} statement is matched (#43).
 *
 * <pre>
 * String packageName = PackageDeclaration.of(Path.of("src/main/java/shop/web/CatalogController.java")); // "shop.web"
 * </pre>
 */
public final class PackageDeclaration {

	private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

	private static final Pattern LINE_COMMENT = Pattern.compile("//[^\\n]*");

	private static final Pattern PACKAGE = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);

	private PackageDeclaration() {
	}

	/**
	 * The declared package, or null for the default package or a file that cannot be
	 * read.
	 */
	public static String of(Path javaSource) {
		try {
			String code = LINE_COMMENT.matcher(BLOCK_COMMENT.matcher(Files.readString(javaSource)).replaceAll(""))
				.replaceAll("");
			Matcher declaration = PACKAGE.matcher(code);
			return declaration.find() ? declaration.group(1) : null;
		}
		catch (IOException e) {
			return null;
		}
	}

}
