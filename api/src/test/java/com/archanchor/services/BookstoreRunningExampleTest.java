package com.archanchor.services;

/**
 * {@code examples/bookstore} analysed end to end, with the real parser. Its README and
 * the ICSE 2027 demo paper quote these results, so a failure here means both are stale,
 * not just this test. What is asserted is in {@link BookstoreRunningExample}, which holds
 * the bookstore to the same numbers in every language (#85).
 */
class BookstoreRunningExampleTest extends BookstoreRunningExample {

	BookstoreRunningExampleTest() {
		super("bookstore",
				new Layers("bookstore/model", "bookstore/persistence", "bookstore/service", "bookstore/web"));
	}

}
