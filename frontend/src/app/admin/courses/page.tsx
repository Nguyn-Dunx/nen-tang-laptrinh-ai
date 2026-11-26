import type { Metadata } from "next";
import { AdminCourses } from "@/features/admin/admin-courses";

export const metadata: Metadata = { title: "Quản trị khóa học" };

export default function AdminCoursesPage() {
  return <AdminCourses />;
}
