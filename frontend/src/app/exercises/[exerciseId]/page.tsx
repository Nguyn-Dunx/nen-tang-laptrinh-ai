import { notFound } from "next/navigation";
import { ExerciseWorkspace } from "@/features/sandbox/exercise-workspace";

type ExercisePageProps = {
  params: Promise<{ exerciseId: string }>;
  searchParams: Promise<{
    course?: string | string[];
    lesson?: string | string[];
  }>;
};

export default async function ExercisePage({
  params,
  searchParams,
}: ExercisePageProps) {
  const { exerciseId: rawId } = await params;
  const query = await searchParams;
  const exerciseId = Number(rawId);
  if (
    !/^\d+$/.test(rawId) ||
    !Number.isSafeInteger(exerciseId) ||
    exerciseId < 1
  )
    notFound();

  return (
    <ExerciseWorkspace
      exerciseId={exerciseId}
      courseSlug={typeof query.course === "string" ? query.course : undefined}
      lessonSlug={typeof query.lesson === "string" ? query.lesson : undefined}
    />
  );
}
