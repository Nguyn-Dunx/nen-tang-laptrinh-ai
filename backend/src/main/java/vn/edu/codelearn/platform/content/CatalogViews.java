package vn.edu.codelearn.platform.content;

import java.util.List;

final class CatalogViews {

    private CatalogViews() {}

    record CourseSummary(String slug, String title, String summary) {}

    record CoursePage(List<CourseSummary> items, int page, int size, long totalElements) {}

    record CourseDetail(String slug, String title, String summary, List<LessonSummary> lessons) {}

    record LessonSummary(String slug, String title, int position) {}

    record LessonDetail(
            String courseSlug, String slug, String title, int position, String bodyMarkdown) {}
}
