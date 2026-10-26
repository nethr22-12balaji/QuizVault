package com.quizvault.collections;

import java.util.ArrayList;
import java.util.List;

/**
 * A generic CRUD repository.
 *
 * JAVA TOPICS: Generics, Collections
 *
 * @param <T> The type of the entity
 */
public class GenericRepository<T> {
    private List<T> items;

    public GenericRepository() {
        this.items = new ArrayList<>();
    }

    public void add(T item) {
        if (!items.contains(item)) {
            items.add(item);
        }
    }

    public void remove(T item) {
        items.remove(item);
    }

    public List<T> getAll() {
        return new ArrayList<>(items);
    }

    public void clear() {
        items.clear();
    }
}
