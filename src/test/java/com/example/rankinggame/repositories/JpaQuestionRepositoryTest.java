package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.Question;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaQuestionRepositoryTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JpaQuestionRepository questionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void loadsRandomActiveQuestion() {
        jdbcTemplate.update("UPDATE questions SET active = FALSE");

        Question inactiveQuestion = new Question();
        inactiveQuestion.setText("Inactive question");
        inactiveQuestion.setCategory("test");
        inactiveQuestion.setActive(false);
        questionRepository.saveAndFlush(inactiveQuestion);

        Question activeQuestion = new Question();
        activeQuestion.setText("Active question");
        activeQuestion.setCategory("test");
        activeQuestion.setActive(true);
        Question savedActiveQuestion = questionRepository.saveAndFlush(activeQuestion);

        assertThat(questionRepository.findRandomActive())
                .isPresent()
                .get()
                .satisfies(question -> {
                    assertThat(question.getId()).isEqualTo(savedActiveQuestion.getId());
                    assertThat(question.getText()).isEqualTo("Active question");
                    assertThat(question.isActive()).isTrue();
                });
    }
}
