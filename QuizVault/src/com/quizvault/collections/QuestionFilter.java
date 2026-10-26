package com.quizvault.collections;

import com.quizvault.model.Question;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Utility for filtering questions using an Iterator.
 *
 * JAVA TOPICS: Iterator, Set, Collections
 */
public class QuestionFilter {

    /**
     * Filters a set of questions by difficulty.
     */
    public static List<Question> filterByDifficulty(Set<Question> questions, String difficulty) {
        List<Question> result = new ArrayList<>();
        Iterator<Question> iterator = questions.iterator();
        while (iterator.hasNext()) {
            Question q = iterator.next();
            if (q.getDifficulty().equalsIgnoreCase(difficulty)) {
                result.add(q);
            }
        }
        return result;
    }
}
