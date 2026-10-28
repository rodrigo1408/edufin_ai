package br.finedu.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.finedu.support.IntegrationTest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotificationIntegrationTest extends IntegrationTest {

    @Autowired
    ReminderJob reminderJob;

    @Test
    void listMarkReadAndDelete() throws Exception {
        TestUser u = register();
        long id = readLong(getAs(u, "/api/notifications"), "$.content[0].id");
        getAs(u, "/api/notifications/unread-count").andExpect(jsonPath("$.count").value(1));
        postAs(u, "/api/notifications/" + id + "/read", null).andExpect(jsonPath("$.read").value(true));
        getAs(u, "/api/notifications?unreadOnly=true").andExpect(jsonPath("$.totalElements").value(0));
        deleteAs(u, "/api/notifications/" + id).andExpect(status().isNoContent());
        getAs(u, "/api/notifications").andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void notificationsBelongToTheirOwner() throws Exception {
        TestUser owner = register();
        TestUser other = register();
        long id = readLong(getAs(owner, "/api/notifications"), "$.content[0].id");
        postAs(other, "/api/notifications/" + id + "/read", null).andExpect(status().isNotFound());
        deleteAs(other, "/api/notifications/" + id).andExpect(status().isNotFound());
        getAs(owner, "/api/notifications/unread-count").andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void preferencesSilenceCategories() throws Exception {
        TestUser u = register();
        putAs(u, "/api/me/preferences", """
                {"theme":"SYSTEM","notifyGoals":false,"notifyLearning":true,"notifyAchievements":false,
                 "notifyCommunity":true,"weeklyLessonTarget":3}
                """);
        long goal = readLong(postAs(u, "/api/goals", "{\"title\":\"Rápida\",\"category\":\"OTHER\",\"targetAmount\":10}"),
                "$.goal.id");
        postAs(u, "/api/goals/" + goal + "/movements", "{\"type\":\"CONTRIBUTION\",\"amount\":10}")
                .andExpect(jsonPath("$.goalCompleted").value(true));
        getAs(u, "/api/notifications").andExpect(jsonPath("$.content[?(@.type=='GOAL_COMPLETED')]").isEmpty())
                .andExpect(jsonPath("$.content[?(@.type=='ACHIEVEMENT_UNLOCKED')]").isEmpty());
    }

    @Test
    void reminderJobCreatesDeadlineRemindersOnlyOnce() throws Exception {
        TestUser u = register();
        long goal = readLong(postAs(u, "/api/goals", """
                {"title":"Presente","category":"GIFT","targetAmount":100,"deadline":"%s"}
                """.formatted(LocalDate.now().plusDays(3))), "$.goal.id");
        reminderJob.run();
        reminderJob.run();
        List<?> reminders = read(getAs(u, "/api/notifications"), "$.content[?(@.type=='GOAL_DEADLINE')]");
        assertThat(reminders).hasSize(1);
        getAs(u, "/api/notifications").andExpect(jsonPath("$.content[0].link").value("/metas/" + goal));
    }
}
