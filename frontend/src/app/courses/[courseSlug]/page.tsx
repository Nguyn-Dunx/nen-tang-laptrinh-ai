import { CourseDetail } from "@/features/courses/course-detail";

export default async function CoursePage({
  params,
}: {
  params: Promise<{ courseSlug: string }>;
}) {
  const { courseSlug } = await params;
  return <CourseDetail courseSlug={courseSlug} />;
}
