import { describe, expect, it } from 'vitest';
import { AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import { problemDetailOf } from './problemDetail';

function failedWith(status: number, data: unknown): AxiosError {
  const config = { headers: {} } as InternalAxiosRequestConfig;
  const response: AxiosResponse = { data, status, statusText: '', headers: {}, config };
  return new AxiosError('Request failed', AxiosError.ERR_BAD_REQUEST, config, null, response);
}

describe('problemDetailOf', () => {
  it('returns the detail of an RFC 9457 response', () => {
    expect(problemDetailOf(failedWith(400, { status: 400, detail: 'Expected 1' }))).toBe(
      'Expected 1'
    );
  });

  it('returns nothing for a response without a detail', () => {
    expect(problemDetailOf(failedWith(500, { status: 500 }))).toBeUndefined();
  });

  it('returns nothing for an error that is not an HTTP response', () => {
    expect(problemDetailOf(new Error('Network Error'))).toBeUndefined();
  });
});
