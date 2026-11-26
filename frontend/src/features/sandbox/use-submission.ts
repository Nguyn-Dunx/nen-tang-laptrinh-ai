"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef, useState } from "react";
import { ApiError, api } from "@/lib/api/client";
import type { SubmissionReceipt, SubmissionView } from "@/lib/api/types";
import { isSubmissionPending } from "./submission-status";

type SelectedSubmission = {
  id: number;
  ownerId: number;
  source?: string;
  watchStartedAt: number;
};

const WATCH_DURATION_MS = 90_000;

export function useSubmission(exerciseId: number, userId: number | undefined) {
  const queryClient = useQueryClient();
  const [selected, setSelected] = useState<SelectedSubmission | null>(null);
  const submitLock = useRef(false);
  const active = selected?.ownerId === userId ? selected : null;
  const submission = useQuery({
    queryKey: ["submission", userId, active?.id],
    queryFn: ({ signal }) =>
      api.get<SubmissionView>(`/submissions/${active!.id}`, { signal }),
    enabled: Boolean(userId && active),
    refetchOnWindowFocus: false,
    retry: false,
    refetchInterval: (query) => {
      if (
        !active ||
        query.state.error ||
        Date.now() - active.watchStartedAt >= WATCH_DURATION_MS
      )
        return false;
      if (query.state.data && !isSubmissionPending(query.state.data.status))
        return false;
      return Date.now() - active.watchStartedAt < 15_000 ? 1500 : 4000;
    },
  });

  const create = useMutation({
    mutationFn: (source: string) =>
      api.post<SubmissionReceipt>(`/exercises/${exerciseId}/submissions`, {
        language: "python",
        source_code: source,
      }),
    onSuccess: (receipt, source) => {
      if (!userId) return;
      setSelected({
        id: receipt.submission_id,
        ownerId: userId,
        source,
        watchStartedAt: Date.now(),
      });
      void queryClient.invalidateQueries({
        queryKey: ["submissions", userId, exerciseId],
      });
    },
    onError: (error) => {
      if (error instanceof ApiError && error.status === 401) {
        void queryClient.invalidateQueries({ queryKey: ["auth", "me"] });
      }
    },
  });

  useEffect(() => {
    if (submission.data && !isSubmissionPending(submission.data.status)) {
      void queryClient.invalidateQueries({
        queryKey: ["submissions", userId, exerciseId],
      });
    }
  }, [submission.data, userId, exerciseId, queryClient]);

  async function submit(source: string) {
    if (
      !userId ||
      submitLock.current ||
      create.isPending ||
      (active && submission.isPending) ||
      (submission.data && isSubmissionPending(submission.data.status))
    )
      return;
    submitLock.current = true;
    try {
      await create.mutateAsync(source);
    } catch {
      // The mutation's error state is shown next to the submit control.
    } finally {
      submitLock.current = false;
    }
  }

  function select(id: number) {
    if (!userId) return;
    create.reset();
    setSelected({ id, ownerId: userId, watchStartedAt: Date.now() });
  }

  function resume() {
    if (!active) return;
    setSelected({ ...active, watchStartedAt: Date.now() });
    void submission.refetch();
  }

  return {
    active,
    submission,
    create,
    submit,
    select,
    resume,
    pending:
      create.isPending ||
      Boolean(active && submission.isPending) ||
      Boolean(submission.data && isSubmissionPending(submission.data.status)),
    watchPaused: Boolean(
      active &&
      submission.data &&
      isSubmissionPending(submission.data.status) &&
      submission.dataUpdatedAt - active.watchStartedAt >= WATCH_DURATION_MS,
    ),
  };
}
