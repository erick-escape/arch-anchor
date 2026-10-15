import axios from 'axios';
import { AnalysisSettings } from '../interface/AnalysisSettings';
import { problemDetailOf } from './problemDetail';

function analysisSettingsUrl(projectName: string): string {
  return `/api/projects/${encodeURIComponent(projectName)}/analysis-settings`;
}

/**
 * Reads what the analyses recommend and show for a project (#36). Uses a relative URL like the
 * rest of the page, so the request goes through the Vite proxy.
 *
 * @example
 * const settings = await getAnalysisSettings('bookstore');
 */
export const getAnalysisSettings = async (projectName: string): Promise<AnalysisSettings> => {
  const response = await axios.get<AnalysisSettings>(analysisSettingsUrl(projectName));
  return response.data;
};

/**
 * Replaces a project's analysis settings. For the analysed project the API refreshes the
 * recommendations before it answers.
 *
 * @example
 * await updateAnalysisSettings('bookstore', { ...settings, minRecommendationRate: 0.2 });
 */
export const updateAnalysisSettings = async (
  projectName: string,
  settings: AnalysisSettings
): Promise<AnalysisSettings> => {
  const response = await axios.put<AnalysisSettings>(analysisSettingsUrl(projectName), settings);
  return response.data;
};

/**
 * Turns a failed load or save into a sentence for the user: the API's own explanation when it
 * sent one, a generic hint otherwise.
 *
 * @example
 * setError(describeAnalysisSettingsFailure(error));
 */
export function describeAnalysisSettingsFailure(
  error: unknown,
  action: 'load' | 'save' = 'save'
): string {
  return (
    problemDetailOf(error) ??
    `Could not ${action} the settings. Check that the API is running and try again.`
  );
}
