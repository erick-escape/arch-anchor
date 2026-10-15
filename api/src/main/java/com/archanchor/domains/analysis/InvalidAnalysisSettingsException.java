package com.archanchor.domains.analysis;

/**
 * Analysis settings that cannot be applied: a minimum below its range, a missing value, a
 * rate outside [0, 1]. The message is shown to the architect as is, so it names the
 * setting, the offending value and the expected range (#36).
 */
public class InvalidAnalysisSettingsException extends IllegalArgumentException {

	public InvalidAnalysisSettingsException(String message) {
		super(message);
	}

}
