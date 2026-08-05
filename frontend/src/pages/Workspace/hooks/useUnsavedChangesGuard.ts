import { useEffect } from 'react';
import { useBlocker } from 'react-router-dom';

const DEFAULT_MESSAGE = 'Existem alterações não salvas. Deseja sair sem salvar?';

export interface UseUnsavedChangesGuardOptions {
  dirty: boolean;
  message?: string;
}

export function useUnsavedChangesGuard({
  dirty,
  message = DEFAULT_MESSAGE,
}: UseUnsavedChangesGuardOptions): void {
  const blocker = useBlocker(dirty);

  useEffect(() => {
    if (!dirty) {
      return;
    }
    const handler = (event: BeforeUnloadEvent) => {
      event.preventDefault();
      event.returnValue = '';
    };
    window.addEventListener('beforeunload', handler);
    return () => window.removeEventListener('beforeunload', handler);
  }, [dirty]);

  useEffect(() => {
    if (blocker.state !== 'blocked') {
      return;
    }
    const confirmed = window.confirm(message);
    if (confirmed) {
      blocker.proceed();
    } else {
      blocker.reset();
    }
  }, [blocker, message]);
}
