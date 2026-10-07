import 'react';

// Non-standard attributes that turn a file input into a directory picker. Every current browser
// supports `webkitdirectory`, but React's typings only list standard attributes.
declare module 'react' {
  // Declaration merging requires the original type parameter name, even though it is unused here.
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  interface InputHTMLAttributes<T> {
    webkitdirectory?: string;
    directory?: string;
  }
}
