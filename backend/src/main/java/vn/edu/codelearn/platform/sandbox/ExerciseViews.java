package vn.edu.codelearn.platform.sandbox;

import java.util.List;

final class ExerciseViews {

    private ExerciseViews() {}

    record ExerciseView(
            long id,
            long lessonId,
            String title,
            String language,
            String difficulty,
            String statementMarkdown,
            String starterCode,
            int maxScore,
            List<PublicTestCase> publicTests) {}

    record PublicTestCase(int position, String stdin, String expectedOutput) {}

    record ExerciseRow(
            long id,
            long lessonId,
            String title,
            String language,
            String difficulty,
            String statementMarkdown,
            String starterCode,
            int maxScore) {}
}
