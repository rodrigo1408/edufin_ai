package br.finedu.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.finedu.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;

class CourseIntegrationTest extends IntegrationTest {

    private long firstCourse(TestUser u) throws Exception {
        return readLong(getAs(u, "/api/courses").andExpect(status().isOk()), "$[0].id");
    }

    @Test
    void catalogShowsProgressiveContent() throws Exception {
        TestUser u = register();
        getAs(u, "/api/courses").andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].slug").value("primeiros-passos"))
                .andExpect(jsonPath("$[0].difficulty").value("BEGINNER"))
                .andExpect(jsonPath("$[5].difficulty").value("ADVANCED"))
                .andExpect(jsonPath("$[0].enrolled").value(false));
        getAs(u, "/api/courses?topic=INVESTING").andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void lessonsRequireEnrollment() throws Exception {
        TestUser u = register();
        long courseId = firstCourse(u);
        long lessonId = readLong(getAs(u, "/api/courses/" + courseId), "$.modules[0].lessons[0].id");
        getAs(u, "/api/lessons/" + lessonId).andExpect(status().isForbidden());
        postAs(u, "/api/lessons/" + lessonId + "/complete", null).andExpect(status().isForbidden());
        postAs(u, "/api/courses/" + courseId + "/enroll", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.course.enrolled").value(true))
                .andExpect(jsonPath("$.enrollment.nextLessonId").value(lessonId));
        getAs(u, "/api/lessons/" + lessonId).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isNotEmpty())
                .andExpect(jsonPath("$.previousLessonId").isEmpty());
    }

    @Test
    void completingAllLessonsCompletesCourseOnce() throws Exception {
        TestUser u = register();
        long courseId = firstCourse(u);
        postAs(u, "/api/courses/" + courseId + "/enroll", null);
        List<Integer> lessonIds = read(getAs(u, "/api/courses/" + courseId), "$.modules[*].lessons[*].id");
        int total = 0;
        for (int i = 0; i < lessonIds.size(); i++) {
            var r = postAs(u, "/api/lessons/" + lessonIds.get(i) + "/complete", null).andExpect(status().isOk());
            boolean last = i == lessonIds.size() - 1;
            r.andExpect(jsonPath("$.courseCompleted").value(last));
            total += ((Number) read(r, "$.reward.points")).intValue();
        }
        assertThat(total).isGreaterThanOrEqualTo(lessonIds.size() * 10 + 50);

        // Repetir não gera pontos nem "conclui" de novo
        postAs(u, "/api/lessons/" + lessonIds.get(0) + "/complete", null)
                .andExpect(jsonPath("$.reward.points").value(0))
                .andExpect(jsonPath("$.courseCompleted").value(false))
                .andExpect(jsonPath("$.progressPercent").value(100.0));

        getAs(u, "/api/courses/mine").andExpect(jsonPath("$[0].completedAt").isNotEmpty());
        getAs(u, "/api/notifications").andExpect(jsonPath("$.content[?(@.type=='COURSE_COMPLETED')]").exists());
        getAs(u, "/api/gamification/summary")
                .andExpect(jsonPath("$.achievements[?(@.code=='FIRST_COURSE')].unlocked").value(true));
    }

    @Test
    void continueFromWhereYouStopped() throws Exception {
        TestUser u = register();
        long courseId = firstCourse(u);
        postAs(u, "/api/courses/" + courseId + "/enroll", null);
        List<Integer> ids = read(getAs(u, "/api/courses/" + courseId), "$.modules[*].lessons[*].id");
        postAs(u, "/api/lessons/" + ids.get(0) + "/complete", null).andExpect(jsonPath("$.nextLessonId").value(ids.get(1)));
        postAs(u, "/api/lessons/" + ids.get(1) + "/visit", null).andExpect(status().isNoContent());
        getAs(u, "/api/courses/" + courseId).andExpect(jsonPath("$.enrollment.lastLessonId").value(ids.get(1)))
                .andExpect(jsonPath("$.enrollment.nextLessonId").value(ids.get(1)))
                .andExpect(jsonPath("$.course.completedLessons").value(1));
    }

    @Test
    void notesAndReviews() throws Exception {
        TestUser u = register();
        long courseId = firstCourse(u);
        putAs(u, "/api/courses/" + courseId + "/review", "{\"rating\":5,\"comment\":\"Ótimo\"}")
                .andExpect(status().isForbidden());
        postAs(u, "/api/courses/" + courseId + "/enroll", null);
        long lessonId = readLong(getAs(u, "/api/courses/" + courseId), "$.modules[0].lessons[0].id");
        putAs(u, "/api/lessons/" + lessonId + "/note", "{\"content\":\"Guardar 20% da mesada\"}")
                .andExpect(jsonPath("$.content").value("Guardar 20% da mesada"));
        getAs(u, "/api/lessons/" + lessonId).andExpect(jsonPath("$.note").value("Guardar 20% da mesada"));
        getAs(u, "/api/courses/notes").andExpect(jsonPath("$.length()").value(1));

        putAs(u, "/api/courses/" + courseId + "/review", "{\"rating\":6}").andExpect(status().isBadRequest());
        putAs(u, "/api/courses/" + courseId + "/review", "{\"rating\":4,\"comment\":\"Muito bom!\"}")
                .andExpect(jsonPath("$.rating").value(4));
        putAs(u, "/api/courses/" + courseId + "/review", "{\"rating\":5,\"comment\":\"Melhor ainda\"}")
                .andExpect(jsonPath("$.rating").value(5));
        getAs(u, "/api/courses/" + courseId + "/reviews").andExpect(jsonPath("$.content[?(@.comment=='Melhor ainda')]").exists());
        getAs(u, "/api/courses/" + courseId).andExpect(jsonPath("$.myReview.rating").value(5));
    }

    @Test
    void notesArePrivate() throws Exception {
        TestUser a = register();
        TestUser b = register();
        long courseId = firstCourse(a);
        postAs(a, "/api/courses/" + courseId + "/enroll", null);
        postAs(b, "/api/courses/" + courseId + "/enroll", null);
        long lessonId = readLong(getAs(a, "/api/courses/" + courseId), "$.modules[0].lessons[0].id");
        putAs(a, "/api/lessons/" + lessonId + "/note", "{\"content\":\"segredo\"}");
        getAs(b, "/api/lessons/" + lessonId).andExpect(jsonPath("$.note").isEmpty());
    }
}
