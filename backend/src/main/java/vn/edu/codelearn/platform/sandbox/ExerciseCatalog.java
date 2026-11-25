package vn.edu.codelearn.platform.sandbox;

import java.util.List;

public interface ExerciseCatalog {

    List<ExerciseSummary> listForPublishedLesson(long lessonId);

    record ExerciseSummary(long id, String title, String language) {}
}
