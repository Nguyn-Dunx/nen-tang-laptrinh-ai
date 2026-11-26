import { AdminLessonEditor } from "@/features/admin/admin-lesson-editor";

export default async function AdminLessonPage({
  params,
}: {
  params: Promise<{ courseSlug: string; lessonSlug: string }>;
}) {
  const { courseSlug, lessonSlug } = await params;
  return <AdminLessonEditor courseSlug={courseSlug} lessonSlug={lessonSlug} />;
}
