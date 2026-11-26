import { api } from "@/lib/api/client";
import type { components } from "@/lib/api/schema";

type Schemas = components["schemas"];

export type AdminCourse = Schemas["AdminCourseDetail"];
export type AdminLesson = Schemas["AdminLessonDetail"];
export type CourseInput = Schemas["CreateCourse"];
export type LessonInput = Schemas["CreateLesson"];
export type CourseUpdate = Schemas["UpdateCourse"];
export type LessonUpdate = Schemas["UpdateLesson"];

export const adminKeys = {
  all: ["admin-courses"] as const,
  list: (page: number) => ["admin-courses", "list", page] as const,
  course: (slug: string) => ["admin-courses", slug] as const,
  lesson: (course: string, lesson: string) =>
    ["admin-courses", course, "lessons", lesson] as const,
};

export function coursePath(courseSlug: string) {
  return `/admin/courses/${encodeURIComponent(courseSlug)}`;
}

export function lessonPath(courseSlug: string, lessonSlug: string) {
  return `${coursePath(courseSlug)}/lessons/${encodeURIComponent(lessonSlug)}`;
}

export const adminApi = {
  list: (page: number) =>
    api.get<Schemas["AdminCoursePage"]>(`/admin/courses?page=${page}&size=12`),
  course: (slug: string) => api.get<AdminCourse>(coursePath(slug)),
  createCourse: (body: CourseInput) =>
    api.post<AdminCourse>("/admin/courses", body),
  updateCourse: (slug: string, body: CourseUpdate) =>
    api.put<AdminCourse>(coursePath(slug), body),
  publishCourse: (slug: string, published: boolean) =>
    api.put<AdminCourse>(`${coursePath(slug)}/publication`, { published }),
  orderLessons: (slug: string, lessonSlugs: string[]) =>
    api.put<AdminCourse>(`${coursePath(slug)}/lesson-order`, { lessonSlugs }),
  createLesson: (slug: string, body: LessonInput) =>
    api.post<AdminLesson>(`${coursePath(slug)}/lessons`, body),
  lesson: (course: string, lesson: string) =>
    api.get<AdminLesson>(lessonPath(course, lesson)),
  updateLesson: (course: string, lesson: string, body: LessonUpdate) =>
    api.put<AdminLesson>(lessonPath(course, lesson), body),
  publishLesson: (course: string, lesson: string, published: boolean) =>
    api.put<AdminLesson>(`${lessonPath(course, lesson)}/publication`, {
      published,
    }),
};
