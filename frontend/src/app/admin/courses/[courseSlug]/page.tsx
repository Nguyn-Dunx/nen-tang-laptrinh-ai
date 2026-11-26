import { AdminCourseEditor } from "@/features/admin/admin-course-editor";

export default async function AdminCoursePage({
  params,
}: {
  params: Promise<{ courseSlug: string }>;
}) {
  const { courseSlug } = await params;
  return <AdminCourseEditor courseSlug={courseSlug} />;
}
