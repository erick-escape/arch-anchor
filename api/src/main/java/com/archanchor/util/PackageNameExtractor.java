package com.archanchor.util;

import org.springframework.util.StringUtils;

import java.util.Arrays;

public class PackageNameExtractor {

	/**
	 * The package of a qualified type name by the Java naming convention: package names
	 * are lower case and type names start with a capital, so the package ends before the
	 * first capitalised segment. A nested type's enclosing class is therefore not taken
	 * for a package (#35). A name with no capitalised segment keeps everything before its
	 * last dot.
	 *
	 * <pre>
	 * extractPackageName("java.util.Map.Entry"); // "java.util"
	 * extractPackageName("com.archanchor.domains.dependency.Dependency"); // "com.archanchor.domains.dependency"
	 * </pre>
	 * @param fullyQualifiedClassName the fully qualified class name
	 * @return the package name without the class name
	 */
	public String extractPackageName(String fullyQualifiedClassName) {
		if (!StringUtils.hasText(fullyQualifiedClassName)) {
			return "";
		}

		String[] segments = fullyQualifiedClassName.split("\\.");
		for (int index = 0; index < segments.length - 1; index++) {
			if (!segments[index].isEmpty() && Character.isUpperCase(segments[index].charAt(0))) {
				return String.join(".", Arrays.copyOfRange(segments, 0, index));
			}
		}

		// No capitalised segment before the last one: the last segment is the type
		int lastDotIndex = fullyQualifiedClassName.lastIndexOf('.');
		return lastDotIndex == -1 ? "" : fullyQualifiedClassName.substring(0, lastDotIndex);
	}

	/**
	 * Alternative implementation using split for consistency with previous approach
	 */
	public String extractPackageNameAlternative(String fullyQualifiedClassName) {
		if (!StringUtils.hasText(fullyQualifiedClassName)) {
			return "";
		}

		String[] packageParts = fullyQualifiedClassName.split("\\.");

		if (packageParts.length <= 1) {
			return "";
		}

		// Create array without the last element (class name)
		String[] packagePartsOnly = new String[packageParts.length - 1];
		System.arraycopy(packageParts, 0, packagePartsOnly, 0, packageParts.length - 1);

		return String.join(".", packagePartsOnly);
	}

}