import { AxiosAdapter, AxiosResponse } from 'axios';

export interface RecordedAxiosRequest {
  method: string;
  url: string;
  params: unknown;
  body: unknown;
}

/**
 * Stand-in for axios' HTTP adapter: answers registered routes with canned bodies and records
 * every request. Install it on the shared instance the components import.
 *
 * @example
 * const fakeApi = new FakeAxiosAdapter().respondTo('GET', '/api/projects', { projects: [] });
 * axios.defaults.adapter = fakeApi.adapter;
 */
export class FakeAxiosAdapter {
  readonly requests: RecordedAxiosRequest[] = [];
  private readonly responseBodies = new Map<string, unknown>();

  respondTo(method: string, url: string, responseBody: unknown): this {
    this.responseBodies.set(routeKey(method, url), responseBody);
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
