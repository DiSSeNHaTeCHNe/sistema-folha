import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { createMemoryRouter, RouterProvider, useNavigate } from 'react-router-dom';
import { useUnsavedChangesGuard } from './useUnsavedChangesGuard';

function NavigationHarness({ dirty }: { dirty: boolean }) {
  useUnsavedChangesGuard({ dirty });
  const navigate = useNavigate();

  return (
    <button type="button" onClick={() => navigate('/other')}>
      Sair da página
    </button>
  );
}

function renderHarness(dirty: boolean) {
  const router = createMemoryRouter(
    [
      { path: '/', element: <NavigationHarness dirty={dirty} /> },
      { path: '/other', element: <div>Outra página</div> },
    ],
    { initialEntries: ['/'] },
  );

  return render(<RouterProvider router={router} />);
}

describe('useUnsavedChangesGuard', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('allows navigation when dirty is false (WKS2-09)', () => {
    renderHarness(false);

    fireEvent.click(screen.getByRole('button', { name: 'Sair da página' }));

    expect(screen.getByText('Outra página')).toBeInTheDocument();
  });

  it('registers beforeunload listener when dirty is true', () => {
    const addListener = vi.spyOn(window, 'addEventListener');

    renderHarness(true);

    expect(addListener).toHaveBeenCalledWith('beforeunload', expect.any(Function));
  });

  it('prompts confirm dialog before in-app navigation when dirty', () => {
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false);
    renderHarness(true);

    fireEvent.click(screen.getByRole('button', { name: 'Sair da página' }));

    expect(confirmSpy).toHaveBeenCalledWith('Existem alterações não salvas. Deseja sair sem salvar?');
    expect(screen.queryByText('Outra página')).not.toBeInTheDocument();
  });

  it('proceeds with navigation when user confirms discard', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    renderHarness(true);

    fireEvent.click(screen.getByRole('button', { name: 'Sair da página' }));

    expect(screen.getByText('Outra página')).toBeInTheDocument();
  });
});
