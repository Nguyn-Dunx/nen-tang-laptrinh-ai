"use client";

import { useMemo, useSyncExternalStore } from "react";

const DRAFT_EVENT = "edusky-draft-change";
const memoryDrafts = new Map<string, string>();

export function useDraftCode(
  exerciseId: number,
  userId: number | undefined,
  starterCode: string,
) {
  const draft = useMemo(() => {
    const guestKey = `edusky:exercise:${exerciseId}:guest`;
    const key = userId
      ? `edusky:exercise:${exerciseId}:user:${userId}`
      : guestKey;

    return {
      subscribe(listener: () => void) {
        window.addEventListener(DRAFT_EVENT, listener);
        return () => window.removeEventListener(DRAFT_EVENT, listener);
      },
      getSnapshot() {
        try {
          return (
            sessionStorage.getItem(key) ??
            memoryDrafts.get(key) ??
            sessionStorage.getItem(guestKey) ??
            starterCode
          );
        } catch {
          return memoryDrafts.get(key) ?? starterCode;
        }
      },
      getServerSnapshot: () => starterCode,
      set(value: string) {
        memoryDrafts.set(key, value);
        try {
          sessionStorage.setItem(key, value);
        } catch {
          // Editing remains available when the browser disables session storage.
        }
        window.dispatchEvent(new Event(DRAFT_EVENT));
      },
    };
  }, [exerciseId, userId, starterCode]);

  const code = useSyncExternalStore(
    draft.subscribe,
    draft.getSnapshot,
    draft.getServerSnapshot,
  );
  return [code, draft.set] as const;
}
