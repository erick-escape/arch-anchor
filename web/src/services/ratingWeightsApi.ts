import axios from 'axios';
import { RatingWeights } from '../interface/RatingWeights';
import { problemDetailOf } from './problemDetail';

function ratingWeightsUrl(projectName: string): string {
  return `/api/projects/${encodeURIComponent(projectName)}/rating-weights`;
}

/**
 * Reads the weights of a project's election and recommendation rate. Uses a relative URL like
 * the rest of the page, so the request goes through the Vite proxy.
 *
 * @example
 * const weights = await getRatingWeights('bookstore');
 */
export const getRatingWeights = async (projectName: string): Promise<RatingWeights> => {
  const response = await axios.get<RatingWeights>(ratingWeightsUrl(projectName));
  return response.data;
};

/**
 * Replaces a project's weights. For the analysed project the API re-elects the saved modules
 * and re-rates the recommendations before it answers.
 *
 * @example
 * await updateRatingWeights('bookstore', { election, recommendation });
 */
export const updateRatingWeights = async (
  projectName: string,
  weights: RatingWeights
): Promise<RatingWeights> => {
  const response = await axios.put<RatingWeights>(ratingWeightsUrl(projectName), weights);
  return response.data;
};

/**
 * Turns a failed load or save into a sentence for the user: the API's own explanation when it
 * sent one, a generic hint otherwise.
 *
 * @example
 * setError(describeRatingWeightsFailure(error));
 */
export function describeRatingWeightsFailure(
  error: unknown,
  action: 'load' | 'save' = 'save'
): string {
  return (
    problemDetailOf(error) ??
    `Could not ${action} the weights. Check that the API is running and try again.`
  );
}
