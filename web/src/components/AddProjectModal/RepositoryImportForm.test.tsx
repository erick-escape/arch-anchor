// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import axios, { AxiosRequestConfig } from 'axios';
import { message } from 'antd';
import RepositoryImportForm from './RepositoryImportForm';
import { FakeAxiosAdapter } from '../../test/fakes/FakeAxiosAdapter';

const IMPORT_URL = '/api/projects/import';
const PETCLINIC = 'https://github.com/spring-projects/spring-petclinic';

describe('RepositoryImportForm', () => {
  let fakeApi: FakeAxiosAdapter;
  let realAdapter: AxiosRequestConfig['adapter'];

  beforeEach(() => {
    fakeApi = new FakeAxiosAdapter().respondTo('POST', IMPORT_URL, {
      name: 'spring-petclinic',
      repositoryUrl: PETCLINIC,
      commitSha: '4f2c9e1d7b3a5c8e0f6d2b4a9c1e7f3d5b8a0c2e',
    });
    realAdapter = axios.defaults.adapter;
    axios.defaults.adapter = fakeApi.adapter;
  });

  afterEach(() => {
    axios.defaults.adapter = realAdapter;
    message.destroy();
  });

  function sentBodies(): unknown[] {
    return fakeApi
      .requestsTo('POST', IMPORT_URL)
      .map((request) => JSON.parse(String(request.body)));
  }

  async function openOptions() {
    await userEvent.click(screen.getByText('Options'));
  }

  it('imports the repository at the URL and reports success', async () => {
    const onSuccess = vi.fn();
    render(<RepositoryImportForm onSuccess={onSuccess} />);

    await userEvent.type(screen.getByLabelText('Repository URL'), PETCLINIC);
    await userEvent.click(screen.getByRole('button', { name: 'Import' }));

    await vi.waitFor(() => expect(onSuccess).toHaveBeenCalledOnce());
    expect(sentBodies()).toEqual([{ repositoryUrl: PETCLINIC }]);
    expect(await screen.findByText('Imported spring-petclinic at 4f2c9e1')).toBeInTheDocument();
  });

  it('sends the options that were filled in', async () => {
    const onSuccess = vi.fn();
    render(<RepositoryImportForm onSuccess={onSuccess} />);

    await userEvent.type(screen.getByLabelText('Repository URL'), PETCLINIC);
    await openOptions();
    await userEvent.type(screen.getByLabelText('Branch or tag'), 'main');
    await userEvent.type(screen.getByLabelText('Subdirectory'), 'backend');
    await userEvent.type(screen.getByLabelText('Project name'), 'petclinic-main');
    await userEvent.type(screen.getByLabelText('Access token'), 'github_pat_secret');
    await userEvent.click(screen.getByRole('button', { name: 'Import' }));

    await vi.waitFor(() => expect(onSuccess).toHaveBeenCalledOnce());
    expect(sentBodies()).toEqual([
      {
        repositoryUrl: PETCLINIC,
        ref: 'main',
        subdirectory: 'backend',
        projectName: 'petclinic-main',
        accessToken: 'github_pat_secret',
      },
    ]);
  });

  it('clears the form, token included, after a successful import', async () => {
    const onSuccess = vi.fn();
    render(<RepositoryImportForm onSuccess={onSuccess} />);

    await userEvent.type(screen.getByLabelText('Repository URL'), PETCLINIC);
    await openOptions();
    await userEvent.type(screen.getByLabelText('Access token'), 'github_pat_secret');
    await userEvent.click(screen.getByRole('button', { name: 'Import' }));

    await vi.waitFor(() => expect(onSuccess).toHaveBeenCalledOnce());
    expect(screen.getByLabelText('Repository URL')).toHaveValue('');
    expect(screen.getByLabelText('Access token')).toHaveValue('');
  });

  it('masks the access token', async () => {
    render(<RepositoryImportForm onSuccess={() => {}} />);

    await openOptions();

    expect(screen.getByLabelText('Access token')).toHaveAttribute('type', 'password');
    expect(screen.getByLabelText('Access token')).toHaveAttribute('autocomplete', 'off');
  });

  it('shows the reason the API gave when the import fails', async () => {
    const detail = "A project named 'spring-petclinic' already exists; delete it first";
    fakeApi.failWith('POST', IMPORT_URL, 409, { status: 409, detail });
    const onSuccess = vi.fn();
    render(<RepositoryImportForm onSuccess={onSuccess} />);

    await userEvent.type(screen.getByLabelText('Repository URL'), PETCLINIC);
    await userEvent.click(screen.getByRole('button', { name: 'Import' }));

    expect(await screen.findByText(detail)).toBeInTheDocument();
    expect(onSuccess).not.toHaveBeenCalled();
    expect(screen.getByLabelText('Repository URL')).toHaveValue(PETCLINIC);
  });

  it('does not call the API without a URL', async () => {
    render(<RepositoryImportForm onSuccess={() => {}} />);

    await userEvent.click(screen.getByRole('button', { name: 'Import' }));

    expect(await screen.findByText('Enter the repository URL')).toBeInTheDocument();
    expect(fakeApi.requests).toHaveLength(0);
  });
});
