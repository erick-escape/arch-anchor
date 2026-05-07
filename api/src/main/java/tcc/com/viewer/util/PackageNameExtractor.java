package tcc.com.viewer.util;

import org.springframework.util.StringUtils;

public class PackageNameExtractor {

	/**
	 * Extracts package name from fully qualified class name Example:
	 * "tcc.com.viewer.domains.dependency.Dependency" ->
	 * "tcc.com.viewer.domains.dependency"
	 * @param fullyQualifiedClassName the fully qualified class name
	 * @return the package name without the class name
	 */
	public String extractPackageName(String fullyQualifiedClassName) {
		if (!StringUtils.hasText(fullyQualifiedClassName)) {
			return "";
		}

		// Find the last dot which separates package from class name
		int lastDotIndex = fullyQualifiedClassName.lastIndexOf('.');

		// If no dot found, it means there's no package (class in default package)
		if (lastDotIndex == -1) {
			return "";
		}

		// Return everything before the last dot
		return fullyQualifiedClassName.substring(0, lastDotIndex);
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