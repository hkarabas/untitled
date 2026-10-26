package com.example.untitled.service;

import com.example.untitled.entity.Book;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Random but realistic book data, shared by database generation and sample-file generation.
 */
public final class RandomBooks {

    private static final String[] FIRST_NAMES = {"John", "Jane", "Michael", "Emily", "David", "Sarah", "Robert", "Lisa",
            "James", "Mary", "William", "Patricia", "Richard", "Jennifer", "Charles"};
    private static final String[] LAST_NAMES = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller",
            "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson"};
    private static final String[] ADJECTIVES = {"Great", "Amazing", "Wonderful", "Fantastic", "Incredible", "Mysterious",
            "Hidden", "Lost", "Ancient", "Modern", "Secret", "Dark", "Bright", "Silent"};
    private static final String[] NOUNS = {"Adventure", "Journey", "Story", "Tale", "Mystery", "Dream", "Legend", "Quest",
            "Chronicle", "Saga", "Odyssey", "Treasure", "Kingdom", "Empire", "World"};

    private RandomBooks() {
    }

    /**
     * @param number appended to the title so every generated title is unique (1-based)
     * @return a book with a random title, author and a price between 5.99 and 99.99
     */
    public static Book create(long number) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String author = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " "
                + LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        String title = "The " + ADJECTIVES[random.nextInt(ADJECTIVES.length)] + " "
                + NOUNS[random.nextInt(NOUNS.length)] + " " + number;
        BigDecimal price = BigDecimal.valueOf(5.99 + random.nextDouble() * 94.00).setScale(2, RoundingMode.HALF_UP);
        return new Book(title, author, price);
    }
}
