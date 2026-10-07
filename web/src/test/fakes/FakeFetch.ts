export interface RecordedFetchCall {
  url: string;
  init: RequestInit | undefined;
}

/**
 * Stand-in for the global `fetch`: records every call and answers with a fixed status.
 *
 * @example
 * const fakeFetch = new FakeFetch(200);
 * vi.stubGlobal('fetch', fakeFetch.fetch);
 */
export class FakeFetch {
  readonly calls: RecordedFetchCall[] = [];

  constructor(private readonly responseStatus: number = 200) {}

  readonly fetch = async (input: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
    this.calls.push({ url: String(input), init });
    return new Response(null, { status: this.responseStatus });
  };
}
