package com.example.rankinggame.mapper;

import com.example.rankinggame.engine.Question;
import com.example.rankinggame.engine.QuestionId;
import com.example.rankinggame.entities.QuestionEntity;
import org.springframework.stereotype.Service;

@Service
public class QuestionMapper {
    public Question toDomain(QuestionEntity questionEntity) {
        return new Question(
                new QuestionId(questionEntity.getId()),
                questionEntity.getText(),
                questionEntity.getCategory()
        );
    }

    public QuestionEntity toEntity(Question question) {
        QuestionEntity questionEntity = new QuestionEntity();
        questionEntity.setId(question.questionId() == null ? null : question.questionId().value());
        questionEntity.setText(question.text());
        questionEntity.setCategory(question.category());
        questionEntity.setActive(true);
        return questionEntity;
    }
}
