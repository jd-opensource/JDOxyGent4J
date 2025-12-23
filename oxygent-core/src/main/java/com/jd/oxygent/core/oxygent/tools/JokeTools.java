/*
 * Copyright 2025 JD.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this project except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.oxygent.core.oxygent.tools;

import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;
import lombok.experimental.SuperBuilder;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Joke generation tool class providing random joke generation functionality.
 * <p>
 * This tool class has a built-in collection of carefully selected English jokes
 * and can randomly return one of them. Mainly used for entertainment scenarios,
 * providing users with relaxed and pleasant interactive experiences. The joke
 * content is healthy and positive, suitable for users of all ages.
 * </p>
 *
 * <p><strong>Features:</strong></p>
 * <ul>
 *   <li>Built-in 10 carefully selected English jokes</li>
 *   <li>Uses secure random number generator to ensure randomness</li>
 *   <li>Thread-safe implementation</li>
 *   <li>Comprehensive error handling mechanism</li>
 * </ul>
 *
 * <p><strong>Joke Themes:</strong></p>
 * <ul>
 *   <li>Science Humor - Atom and scientist related jokes</li>
 *   <li>Workplace Humor - Work and award related jokes</li>
 *   <li>Life Humor - Daily life scenario jokes</li>
 *   <li>Word Play - Puns and homophone jokes</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * JokeTools jokeTools = new JokeTools();
 *
 * // Get random joke
 * String joke = (String) jokeTools.call("joke_tool");
 * System.out.println(joke);
 * }</pre>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @see FunctionHub Tool execution framework base class
 * @see SecureRandom Secure random number generator
 * @since 1.0.0
 */
@SuperBuilder
public class JokeTools extends FunctionHub {

    public JokeTools() {
        super("joke_tools");
        setDesc("Entertainment tool providing random joke generation functionality with built-in curated English joke collection");
    }

    /**
     * Curated joke collection.
     * <p>
     * Contains 10 carefully selected English jokes covering different themes such as
     * science, workplace, and daily life. All joke content is healthy and positive,
     * suitable for various occasions.
     * </p>
     */
    private static final List<String> JOKES = List.of(
            "Why don't scientists trust atoms? Because they make up everything!",
            "Why did the scarecrow win an award? He was outstanding in his field!",
            "Why don't eggs tell jokes? They'd crack each other up!",
            "What do you call a fake noodle? An impasta!",
            "Why did the math book look so sad? Because it had too many problems!",
            "What do you call a bear with no teeth? A gummy bear!",
            "Why don't skeletons fight each other? They don't have the guts!",
            "What's orange and sounds like a parrot? A carrot!",
            "Why did the bicycle fall over? Because it was two tired!",
            "What do you call a sleeping bull? A bulldozer!"
    );

    /**
     * Secure random number generator for selecting random jokes.
     * <p>
     * Uses SecureRandom to ensure better randomness and thread safety.
     * </p>
     */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Initialization method that sets tool name and registers joke generation functionality.
     * <p>
     * Automatically called after object creation to complete tool registration and configuration.
     * Sets tool name to "joke_tools" and registers the joke_tool function.
     * </p>
     */
    @Override
    public void init() {
        super.init();
        setName("joke_tools");
        setDesc("Entertainment tool providing random joke generation functionality with built-in curated English joke collection");

        // Register joke generation tool
        this.registerTool(
                "joke_tool",
                "Randomly generate an interesting English joke for entertainment and atmosphere enhancement",
                this::generateRandomJoke,
                Collections.emptyList() // No parameters needed
        );
    }

    /**
     * Generate random joke.
     * <p>
     * Randomly selects and returns a joke from the built-in joke collection. Uses secure
     * random number generator to ensure different jokes are obtained with each call
     * (statistically speaking).
     * </p>
     *
     * <p><strong>Implementation Details:</strong></p>
     * <ul>
     *   <li>Uses SecureRandom to generate random index</li>
     *   <li>Provides protection against empty collections</li>
     *   <li>Provides detailed error information feedback</li>
     *   <li>Thread-safe implementation</li>
     * </ul>
     *
     * @param args Parameter array (this method does not use any parameters)
     * @return Randomly selected joke string, or error message if an error occurs
     */
    private String generateRandomJoke(Object... args) {
        try {
            Objects.requireNonNull(JOKES, "Joke collection cannot be null");

            if (JOKES.isEmpty()) {
                return "Error: Joke collection is empty, cannot generate joke";
            }

            var randomIndex = secureRandom.nextInt(JOKES.size());
            var selectedJoke = JOKES.get(randomIndex);

            Objects.requireNonNull(selectedJoke, "Selected joke cannot be null");

            return selectedJoke;

        } catch (IllegalArgumentException e) {
            return "Joke generation failed: Parameter error - " + e.getMessage();
        } catch (Exception e) {
            return "Unknown error occurred during joke generation: " + e.getMessage();
        }
    }

    // ========== Test Methods ==========

    /**
     * Test method demonstrating basic functionality of JokeTools.
     * <p>
     * Generates multiple jokes consecutively to verify tool correctness and randomness.
     * Also tests error handling and tool robustness.
     * </p>
     *
     * @param args Command line arguments (unused)
     */
    public static void main(String[] args) {
        var jokeTools = new JokeTools();
        jokeTools.init(); // Manual initialization for testing

        System.out.println("=== Joke Tool Test ===");

        // Generate multiple jokes to test randomness
        System.out.println("1. Random joke generation test:");
        for (int i = 1; i <= 5; i++) {
            var joke = jokeTools.call("joke_tool");
            System.out.println("   Joke " + i + ": " + joke);
        }

        // Test tool information
        System.out.println("\n2. Tool information:");
        System.out.println("   Tool name: " + jokeTools.getName());
        System.out.println("   Tool description: " + jokeTools.getDesc());
        System.out.println("   Total jokes: " + JOKES.size());

        // Test joke collection
        System.out.println("\n3. All jokes preview:");
        for (int i = 0; i < JOKES.size(); i++) {
            System.out.println("   " + (i + 1) + ". " + JOKES.get(i));
        }
    }
}