import { App } from 'antd';
import { useMemo } from 'react';

export interface Toast {
  success: (text: string) => void;
  error: (text: string) => void;
}

/**
 * The one way the app reports the outcome of an action: a short toast, themed with the rest of
 * antd through ThemeProvider. A thin wrapper so screens do not depend on antd's message API.
 *
 * @example const toast = useToast(); toast.error('Could not rename bookstore/web.');
 */
export function useToast(): Toast {
  const { message } = App.useApp();
  return useMemo(
    () => ({
      success: (text: string) => void message.success(text),
      error: (text: string) => void message.error(text),
    }),
    [message]
  );
}
