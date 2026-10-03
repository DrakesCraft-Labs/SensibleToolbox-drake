package io.github.thebusybiscuit.sensibletoolbox.core.guide;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * The in-game guide of SensibleToolbox. Every language is a YAML file bundled inside the
 * plugin jar ({@code guide_<code>.yml}); the guide is never copied to the data folder so it
 * always matches the running version.
 * <p>
 * This class has no dependency on a running server so its content can be validated by tests.
 */
public final class GuideBook {

    /**
     * Number of content lines shown on one chat page.
     */
    public static final int PAGE_SIZE = 10;

    private static final Map<Language, GuideBook> CACHE = new EnumMap<>(Language.class);

    private final Language language;
    private final String title;
    private final Map<String, String> ui;
    private final List<Topic> topics;

    private GuideBook(Language language, String title, Map<String, String> ui, List<Topic> topics) {
        this.language = language;
        this.title = title;
        this.ui = ui;
        this.topics = topics;
    }

    /**
     * Returns the (cached) guide for the given language.
     *
     * @param language
     *            The language
     *
     * @return The guide
     */
    @Nonnull
    public static synchronized GuideBook get(@Nonnull Language language) {
        return CACHE.computeIfAbsent(language, GuideBook::load);
    }

    @Nonnull
    static GuideBook load(@Nonnull Language language) {
        String resource = language.getResourceName();

        try (InputStream in = GuideBook.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing guide resource: " + resource);
            }

            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return parse(language, YamlConfiguration.loadConfiguration(reader));
            }
        } catch (IOException x) {
            throw new IllegalStateException("Could not read guide resource: " + resource, x);
        }
    }

    @Nonnull
    @ParametersAreNonnullByDefault
    static GuideBook parse(Language language, YamlConfiguration yaml) {
        String title = yaml.getString("title", "SensibleToolbox");

        Map<String, String> ui = new java.util.LinkedHashMap<>();
        ConfigurationSection uiSection = yaml.getConfigurationSection("ui");
        if (uiSection != null) {
            for (String key : uiSection.getKeys(false)) {
                ui.put(key, uiSection.getString(key, ""));
            }
        }

        List<Topic> topics = new ArrayList<>();
        ConfigurationSection topicSection = yaml.getConfigurationSection("topics");
        if (topicSection != null) {
            for (String id : topicSection.getKeys(false)) {
                ConfigurationSection t = topicSection.getConfigurationSection(id);
                if (t != null) {
                    topics.add(new Topic(id.toLowerCase(Locale.ROOT), t.getString("title", id), t.getString("summary", ""), List.copyOf(t.getStringList("lines"))));
                }
            }
        }

        return new GuideBook(language, title, Collections.unmodifiableMap(ui), List.copyOf(topics));
    }

    @Nonnull
    public Language getLanguage() {
        return language;
    }

    @Nonnull
    public String getTitle() {
        return title;
    }

    /**
     * Returns a translated user interface string, or the key itself if it is missing.
     *
     * @param key
     *            The key below {@code ui:} in the guide file
     *
     * @return The translated string
     */
    @Nonnull
    public String ui(@Nonnull String key) {
        return ui.getOrDefault(key, key);
    }

    @Nonnull
    public Map<String, String> getUiStrings() {
        return ui;
    }

    @Nonnull
    public List<Topic> getTopics() {
        return topics;
    }

    /**
     * Finds a topic by its exact id, or by a unique id prefix.
     *
     * @param query
     *            The id or prefix
     *
     * @return The topic, if exactly one matched
     */
    @Nonnull
    public Optional<Topic> findTopic(@Nullable String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }

        String q = query.toLowerCase(Locale.ROOT);
        Topic prefixMatch = null;
        int prefixMatches = 0;

        for (Topic topic : topics) {
            if (topic.id().equals(q)) {
                return Optional.of(topic);
            }

            if (topic.id().startsWith(q)) {
                prefixMatch = topic;
                prefixMatches++;
            }
        }

        return prefixMatches == 1 ? Optional.of(prefixMatch) : Optional.empty();
    }

    /**
     * Returns every topic whose title, summary or content contains the given term
     * (case-insensitive, color codes ignored).
     *
     * @param term
     *            The search term
     *
     * @return The matching topics, in guide order
     */
    @Nonnull
    public List<Topic> search(@Nonnull String term) {
        String needle = term.trim().toLowerCase(Locale.ROOT);
        List<Topic> result = new ArrayList<>();

        if (needle.isEmpty()) {
            return result;
        }

        for (Topic topic : topics) {
            if (topic.searchableText().contains(needle)) {
                result.add(topic);
            }
        }

        return result;
    }

    /**
     * Strips legacy {@code &} color codes from a guide line.
     *
     * @param line
     *            The line
     *
     * @return The plain text
     */
    @Nonnull
    public static String stripColors(@Nonnull String line) {
        return line.replaceAll("(?i)&[0-9a-fk-or]", "");
    }

    /**
     * A single chapter of the guide.
     *
     * @param id
     *            The id used in commands, e.g. {@literal "energy"}
     * @param title
     *            The translated title
     * @param summary
     *            A one-line description shown in the index
     * @param lines
     *            The content lines (legacy {@code &} color codes allowed)
     */
    public record Topic(@Nonnull String id, @Nonnull String title, @Nonnull String summary, @Nonnull List<String> lines) {

        /**
         * @return The number of chat pages this topic needs (at least one)
         */
        public int pageCount() {
            return Math.max(1, (lines.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        }

        /**
         * Returns the lines of one page; the page number is clamped to the valid range.
         *
         * @param page
         *            The 1-based page number
         *
         * @return The lines on that page
         */
        @Nonnull
        public List<String> page(int page) {
            int p = clampPage(page);
            int from = (p - 1) * PAGE_SIZE;
            int to = Math.min(lines.size(), from + PAGE_SIZE);
            return from >= to ? List.of() : lines.subList(from, to);
        }

        /**
         * @param page
         *            A requested page number
         *
         * @return The page number clamped to {@code 1..pageCount()}
         */
        public int clampPage(int page) {
            return Math.max(1, Math.min(page, pageCount()));
        }

        @Nonnull
        String searchableText() {
            StringBuilder sb = new StringBuilder(id).append('\n').append(title).append('\n').append(summary);
            for (String line : lines) {
                sb.append('\n').append(line);
            }
            return stripColors(sb.toString()).toLowerCase(Locale.ROOT);
        }
    }

    /**
     * The languages the guide is available in.
     */
    public enum Language {

        ENGLISH("en", "English", "en", "eng", "english", "ingles", "inglés"),
        SPANISH("es", "Español", "es", "esp", "spanish", "espanol", "español", "castellano");

        private final String code;
        private final String displayName;
        private final List<String> tokens;

        Language(String code, String displayName, String... tokens) {
            this.code = code;
            this.displayName = displayName;
            this.tokens = List.of(tokens);
        }

        @Nonnull
        public String getCode() {
            return code;
        }

        @Nonnull
        public String getDisplayName() {
            return displayName;
        }

        @Nonnull
        public String getResourceName() {
            return "guide_" + code + ".yml";
        }

        /**
         * @return The other language, used for the "switch language" button
         */
        @Nonnull
        public Language other() {
            return this == ENGLISH ? SPANISH : ENGLISH;
        }

        /**
         * Resolves a command argument such as {@literal "es"} or {@literal "english"}.
         *
         * @param token
         *            The argument
         *
         * @return The language, if the argument names one
         */
        @Nonnull
        public static Optional<Language> fromToken(@Nullable String token) {
            if (token == null) {
                return Optional.empty();
            }

            String t = token.toLowerCase(Locale.ROOT);
            for (Language language : values()) {
                if (language.tokens.contains(t)) {
                    return Optional.of(language);
                }
            }

            return Optional.empty();
        }

        /**
         * Picks the guide language for a client locale: Spanish for any {@code es*} locale,
         * English otherwise.
         *
         * @param locale
         *            The client locale, may be null (e.g. the console)
         *
         * @return The language to use
         */
        @Nonnull
        public static Language fromLocale(@Nullable Locale locale) {
            if (locale != null && "es".equalsIgnoreCase(locale.getLanguage())) {
                return SPANISH;
            }

            return ENGLISH;
        }
    }
}
