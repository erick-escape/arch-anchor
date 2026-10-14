import axios from 'axios';
import { ProblemDetail } from '../interface/Project';

/**
 * The explanation the API wrote for a failed request (the `detail` of its RFC 9457 body), or
 * undefined when it sent none, e.g. because it could not be reached.
 *
 * @example
 * message.error(problemDetailOf(error) ?? 'Could not save the weights.');
 */
export function problemDetailOf(error: unknown): string | undefined {
  if (axios.isAxiosError<ProblemDetail>(error) && error.response?.data?.detail) {
    return error.response.data.detail;
  }
  return undefined;
}
