import { describe, expect, it } from 'vitest';
import { act, renderHook } from '@testing-library/react';
import { useNotification } from './useNotification';

describe('useNotification', () => {
  it('shows notification with default severity', () => {
    const { result } = renderHook(() => useNotification());

    act(() => result.current.showNotification('Hello'));

    expect(result.current.notification).toEqual({
      open: true,
      message: 'Hello',
      severity: 'info',
    });
  });

  it('hides notification', () => {
    const { result } = renderHook(() => useNotification());

    act(() => result.current.showNotification('Error message', 'error'));
    act(() => result.current.hideNotification());

    expect(result.current.notification.open).toBe(false);
    expect(result.current.notification.message).toBe('Error message');
  });
});
