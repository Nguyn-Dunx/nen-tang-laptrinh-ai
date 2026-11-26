import type { components } from "@/lib/api/schema";
import { api } from "@/lib/api/client";

export type CourseSummary = components["schemas"]["CourseSummary"];
export type CourseDetail = components["schemas"]["CourseDetail"];
export type CoursePage = components["schemas"]["CoursePage"];
export type LessonDetail = components["schemas"]["LessonDetail"];
export type LessonExercise = components["schemas"]["LessonExerciseSummary"];

export const coursePath = (slug: string) =>
  `/courses/${encodeURIComponent(slug)}`;
export const lessonPath = (courseSlug: string, lessonSlug: string) =>
  `${coursePath(courseSlug)}/lessons/${encodeURIComponent(lessonSlug)}`;
export const exercisePath = (
  exerciseId: number,
  courseSlug: string,
  lessonSlug: string,
) =>
  `/exercises/${exerciseId}?${new URLSearchParams({ course: courseSlug, lesson: lessonSlug })}`;

export const catalogQueries = {
  courses: (page = 0) => ({
    queryKey: ["courses", page],
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      api.get<CoursePage>(`/courses?page=${page}&size=12`, { signal }),
  }),
  course: (slug: string) => ({
    queryKey: ["course", slug],
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      api.get<CourseDetail>(coursePath(slug), { signal }),
  }),
  lesson: (courseSlug: string, lessonSlug: string) => ({
    queryKey: ["lesson", courseSlug, lessonSlug],
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      api.get<LessonDetail>(lessonPath(courseSlug, lessonSlug), { signal }),
  }),
  exercises: (courseSlug: string, lessonSlug: string) => ({
    queryKey: ["lesson-exercises", courseSlug, lessonSlug],
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      api.get<LessonExercise[]>(
        `${lessonPath(courseSlug, lessonSlug)}/exercises`,
        { signal },
      ),
  }),
};
