import { api } from '../services/api';
import { AxiosPromise } from 'axios';
import { useMutation } from '@tanstack/react-query';
import React from 'react';

const postData = async ({
  data,
  url,
}: {
  data: React.RefObject<HTMLInputElement>;
  url: string;
}): AxiosPromise<never> => {
  if (data.current && data.current.files) {
    const formData = new FormData();
    Array.from(data.current.files).forEach((file) => {
      formData.append('files', file);
    });

    const response = await api.post(url, formData, {
      headers: {
        Accept: 'application/json',
        'Content-Type': 'multipart/form-data',
      },
    });

    return response.data;
  }

  throw new Error('No files selected.');
};

export function usePostData() {
  return useMutation({
    mutationFn: postData,
    retry: 2,
  });
}
