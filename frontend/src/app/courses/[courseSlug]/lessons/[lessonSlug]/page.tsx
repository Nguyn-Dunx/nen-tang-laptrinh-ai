import { LessonReader } from "@/features/courses/lesson-reader";

export default async function LessonPage({
  params,
}: {
  params: Promise<{ courseSlug: string; lessonSlug: string }>;
}) {
  const { courseSlug, lessonSlug } = await params;
  return (
    <LessonReader
      key={`${courseSlug}/${lessonSlug}`}
      courseSlug={courseSlug}
      lessonSlug={lessonSlug}
    />
  );
}
