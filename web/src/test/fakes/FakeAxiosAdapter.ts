import { AxiosAdapter, AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios';

export interface RecordedAxiosRequest {
  method: string;
  url: string;
  params: unknown;
  body: unknown;
}

interface CannedFailure {
  status: number;
  responseBody: unknown;
}

/**
 * Stand-in for axios' HTTP adapter: answers registered routes with canned bodies, or fails
 * them with a canned status, and records every request. Install it on the shared instance the
 * components import.
 *
 * @example
 * const fakeApi = new FakeAxiosAdapter()
 *   .respondTo('GET', '/api/projects', { projects: [] })
 *   .failWith('POST', '/api/projects/import', 409, { detail: 'Already exists' });
 * axios.defaults.adapter = fakeApi.adapter;
 */
export class FakeAxiosAdapter {
  readonly requests: RecordedAxiosRequest[] = [];
  private readonly responseBodies = new Map<string, unknown>();
  private readonly failures = new Map<string, CannedFailure>();

  respondTo(method: string, url: string, responseBody: unknown): this {
    this.responseBodies.set(routeKey(method, url), responseBody);
    return this;
  }

  failWith(method: string, url: string, status: number, responseBody: unknown): this {
    this.failures.set(routeKey(method, url), { status, responseBody });
    return this;
  }

  requestsTo(method: string, url: string): RecordedAxiosRequest[] {
    return this.requests.filter(
      (request) => routeKey(request.method, request.url) === routeKey(method, url)
    );
  }

  readonly adapter: AxiosAdapter = async (config) => {
    const method = (config.method ?? 'get').toUpperCase();
    const url = config.url ?? '';
    this.requests.push({ method, url, params: config.params, body: config.data });
    const key = routeKey(method, url);
    const failure = this.failures.get(key);
    if (failure) throw rejection(failure, config);
    if (!this.responseBodies.has(key)) {
      throw new Error(
        `FakeAxiosAdapter has no response for "${key}"; register it with respondTo()`
      );
    }
    const response: AxiosResponse = {
      data: this.responseBodies.get(key),
      status: 200,
      statusText: 'OK',
      headers: {},
      config,
    };
    return response;
  };
}

function routeKey(method: string, url: string): string {
  return `${method.toUpperCase()} ${url}`;
}

// Mirrors what axios' real adapters throw for a status outside 2xx, so callers can read
// `error.response.data` exactly as they would in production.
function rejection(failure: CannedFailure, config: InternalAxiosRequestConfig): AxiosError {
  const response: AxiosResponse = {
    data: failure.responseBody,
    status: failure.status,
    statusText: '',
    headers: {},
    config,
  };
  const code = failure.status >= 500 ? AxiosError.ERR_BAD_RESPONSE : AxiosError.ERR_BAD_REQUEST;
  return new AxiosError(
    `Request failed with status code ${failure.status}`,
    code,
    config,
    null,
    response
  );
}
