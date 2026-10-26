package com.quizvault.model;
import java.io.Serializable;
import java.util.Objects;

public class Player implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;

    public Player(String name) {
        this.name = name != null ? name.trim() : "";
    }

    public String getName() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return Objects.equals(name, player.name);
    }

    @Override
    public int hashCode() { return Objects.hash(name); }
}
