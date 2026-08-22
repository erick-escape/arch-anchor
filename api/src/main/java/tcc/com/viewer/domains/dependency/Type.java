package tcc.com.viewer.domains.dependency;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Type {

	private String fullyQualifiedName;

	private String className;

	public Type(String fullyQualifiedName) {
		this.fullyQualifiedName = fullyQualifiedName;
		this.className = extractClassName(fullyQualifiedName);
	}

	private String extractClassName(String fullyQualifiedName) {
		if (fullyQualifiedName == null || fullyQualifiedName.isEmpty()) {
			return "";
		}

		int lastDotIndex = fullyQualifiedName.lastIndexOf('.');
		if (lastDotIndex == -1) {
			return fullyQualifiedName;
		}

		return fullyQualifiedName.substring(lastDotIndex + 1);
	}

}