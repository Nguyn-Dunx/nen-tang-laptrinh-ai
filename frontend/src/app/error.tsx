"use client";

import { ErrorState } from "@/components/feedback";

export default function ErrorPage({
  error,
  reset,
}: {
  error: Error;
  reset: () => void;
}) {
  return (
    <main id="main-content" className="page-container content-page">
      <ErrorState error={error} retry={reset} />
    </main>
  );
}
