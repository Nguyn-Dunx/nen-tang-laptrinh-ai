package vn.edu.codelearn.platform.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import org.hibernate.validator.constraints.CodePointLength;

final class CourseAdminDtos {

    private static final String SLUG = "[a-z0-9]+(?:-[a-z0-9]+)*";

    private CourseAdminDtos() {}

    record CreateCourse(
            @NotBlank @Pattern(regexp = SLUG) @Size(max = 100) String slug,
            @NotBlank @CodePointLength(max = 200) String title,
            @NotBlank @CodePointLength(max = 500) String summary) {}

    record UpdateCourse(
            @NotBlank @CodePointLength(max = 200) String title,
            @NotBlank @CodePointLength(max = 500) String summary) {}

    record CreateLesson(
            @NotBlank @Pattern(regexp = SLUG) @Size(max = 100) String slug,
            @NotBlank @CodePointLength(max = 200) String title,
            @NotBlank @CodePointLength(max = 50000) String bodyMarkdown) {}

    record UpdateLesson(
            @NotBlank @CodePointLength(max = 200) String title,
            @NotBlank @CodePointLength(max = 50000) String bodyMarkdown) {}

    record Publication(@NotNull Boolean published) {}

    record LessonOrder(@NotEmpty List<@NotBlank String> lessonSlugs) {}

    record CourseRow(
            long id, String slug, String title, String summary, OffsetDateTime publishedAt) {}

    record CourseItem(String slug, String title, String summary, OffsetDateTime publishedAt) {
        static CourseItem from(CourseRow row) {
            return new CourseItem(row.slug(), row.title(), row.summary(), row.publishedAt());
        }
    }

    record CoursePage(List<CourseItem> items, int page, int size, long totalElements) {}

    record LessonItem(String slug, String title, int position, OffsetDateTime publishedAt) {}

    record CourseDetail(
            String slug,
            String title,
            String summary,
            OffsetDateTime publishedAt,
            List<LessonItem> lessons) {}

    record LessonDetail(
            String slug,
            String title,
            int position,
            String bodyMarkdown,
            OffsetDateTime publishedAt) {}
}
