"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { QueryClient } from "@tanstack/react-query";
import { ApiError, api } from "@/lib/api/client";
import type { Account } from "@/lib/api/types";

export const authQueryKey = ["auth", "me"] as const;

export async function updateSessionCache(
  queryClient: QueryClient,
  user: Account | null,
) {
  await queryClient.cancelQueries();
  queryClient.setQueryData(authQueryKey, user);
  await queryClient.resetQueries({
    predicate: (query) => query.queryKey[0] !== "auth",
  });
}

export function useAuth() {
  const queryClient = useQueryClient();
  const profile = useQuery({
    queryKey: authQueryKey,
    queryFn: async ({ signal }) => {
      try {
        return await api.get<Account>("/auth/me", { signal });
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) return null;
        throw error;
      }
    },
    retry: false,
  });

  const logout = useMutation({
    mutationFn: async () => {
      try {
        await api.post<void>("/auth/logout");
      } catch (error) {
        if (!(error instanceof ApiError && error.status === 401)) throw error;
      }
      await updateSessionCache(queryClient, null);
    },
  });

  return {
    user: profile.data,
    isPending: profile.isPending,
    error: profile.error,
    refetch: profile.refetch,
    logout: logout.mutateAsync,
    isLoggingOut: logout.isPending,
    logoutError: logout.error,
  };
}
