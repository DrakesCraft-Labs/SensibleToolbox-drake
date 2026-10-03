package io.github.thebusybiscuit.sensibletoolbox.core.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.github.thebusybiscuit.sensibletoolbox.SensibleToolboxPlugin;
import io.github.thebusybiscuit.sensibletoolbox.core.guide.GuideBook.Language;
import io.github.thebusybiscuit.sensibletoolbox.core.guide.GuideBook.Topic;
import me.desht.dhutils.commands.AbstractCommand;

/**
 * Validates the content of the bundled guide files so a broken or untranslated guide can
 * never be packaged.
 */
class GuideBookTest {

    private static final Pattern COLOR_CODE = Pattern.compile("&[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);
    private static final Pattern BAD_COLOR_CODE = Pattern.compile("&(?![0-9a-fk-or])", Pattern.CASE_INSENSITIVE);
    private static final Set<String> REQUIRED_UI_KEYS = Set.of("index_header", "index_intro", "index_hint", "topic_hover", "page", "prev", "prev_hover", "next", "next_hover", "back_to_index", "back_to_index_hover", "switch_language", "switch_language_hover", "unknown_topic", "search_header", "search_none", "search_usage");
    private static final Set<String> REQUIRED_TOPICS = Set.of("start", "energy", "generators", "machines", "upgrades", "farming", "router", "storage", "ender", "tools", "redstone", "components", "security", "commands", "admin", "compat");

    @ParameterizedTest
    @EnumSource(Language.class)
    void everyLanguageLoadsAllRequiredTopics(Language language) {
        GuideBook guide = GuideBook.load(language);

        assertEquals(language, guide.getLanguage());
        assertFalse(guide.getTitle().isBlank());

        List<String> ids = guide.getTopics().stream().map(Topic::id).toList();
        assertTrue(ids.containsAll(REQUIRED_TOPICS), language + " guide is missing topics: " + ids);
    }

    @Test
    void englishAndSpanishHaveTheSameTopicsInTheSameOrder() {
        List<String> english = GuideBook.load(Language.ENGLISH).getTopics().stream().map(Topic::id).toList();
        List<String> spanish = GuideBook.load(Language.SPANISH).getTopics().stream().map(Topic::id).toList();

        assertEquals(english, spanish, "Every topic must be translated");
    }

    @Test
    void translationsHaveComparableLength() {
        GuideBook english = GuideBook.load(Language.ENGLISH);
        GuideBook spanish = GuideBook.load(Language.SPANISH);

        for (Topic en : english.getTopics()) {
            Topic es = spanish.findTopic(en.id()).orElseThrow();
            int diff = Math.abs(en.lines().size() - es.lines().size());
            assertTrue(diff <= 3, "Topic '" + en.id() + "' looks partially translated (" + en.lines().size() + " vs " + es.lines().size() + " lines)");
        }
    }

    @ParameterizedTest
    @EnumSource(Language.class)
    void everyTopicHasRealContent(Language language) {
        for (Topic topic : GuideBook.load(language).getTopics()) {
            assertFalse(topic.title().isBlank(), "Empty title: " + topic.id());
            assertFalse(topic.summary().isBlank(), "Empty summary: " + topic.id());
            assertTrue(topic.lines().size() >= 8, "Topic too short to be useful: " + topic.id());

            for (String line : topic.lines()) {
                assertFalse(GuideBook.stripColors(line).isBlank(), "Blank line in " + topic.id());
                assertTrue(GuideBook.stripColors(line).length() <= 64, "Line too long for chat in " + topic.id() + ": " + line);
                assertFalse(BAD_COLOR_CODE.matcher(line).find(), "Invalid color code in " + topic.id() + ": " + line);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Language.class)
    void noUnresolvedBuildPlaceholders(Language language) {
        GuideBook guide = GuideBook.load(language);
        List<String> all = new ArrayList<>(guide.getUiStrings().values());
        guide.getTopics().forEach(t -> {
            all.add(t.title());
            all.add(t.summary());
            all.addAll(t.lines());
        });

        for (String text : all) {
            assertFalse(text.contains("${"), "Unresolved Maven placeholder: " + text);
        }
    }

    @ParameterizedTest
    @EnumSource(Language.class)
    void allUserInterfaceStringsAreTranslated(Language language) {
        GuideBook guide = GuideBook.load(language);

        for (String key : REQUIRED_UI_KEYS) {
            assertTrue(guide.getUiStrings().containsKey(key), language + " is missing ui." + key);
            assertFalse(guide.ui(key).isBlank(), language + " has an empty ui." + key);
        }

        assertTrue(guide.ui("page").contains("{page}") && guide.ui("page").contains("{pages}"));
        assertTrue(guide.ui("unknown_topic").contains("{topic}"));
        assertTrue(guide.ui("search_none").contains("{term}"));
    }

    @ParameterizedTest
    @EnumSource(Language.class)
    void commandsTopicDocumentsEveryRegisteredCommand(Language language) {
        Topic commands = GuideBook.load(language).findTopic("commands").orElseThrow();
        String text = String.join("\n", commands.lines());

        for (AbstractCommand command : SensibleToolboxPlugin.createCommands()) {
            for (String label : command.getLabels()) {
                assertTrue(text.contains("/" + label), language + " guide does not document /" + label);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Language.class)
    void compatibilityTopicListsEverySupportedVersion(Language language) {
        String text = String.join("\n", GuideBook.load(language).findTopic("compat").orElseThrow().lines());

        for (String version : List.of("1.21.11", "26.1", "26.2")) {
            assertTrue(text.contains(version), language + " compat topic misses " + version);
        }
    }

    @Test
    void topicsAreFoundByIdOrUniquePrefix() {
        GuideBook guide = GuideBook.load(Language.ENGLISH);

        assertEquals("energy", guide.findTopic("ENERGY").orElseThrow().id());
        assertEquals("router", guide.findTopic("rou").orElseThrow().id());
        assertTrue(guide.findTopic("s").isEmpty(), "Ambiguous prefixes must not pick a topic");
        assertTrue(guide.findTopic("does-not-exist").isEmpty());
        assertTrue(guide.findTopic(null).isEmpty());
        assertTrue(guide.findTopic(" ").isEmpty());
    }

    @Test
    void searchIgnoresCaseAndColorCodes() {
        assertTrue(GuideBook.load(Language.ENGLISH).search("HYPERSENDER").stream().anyMatch(t -> t.id().equals("router")));
        assertTrue(GuideBook.load(Language.SPANISH).search("diamante").stream().anyMatch(t -> t.id().equals("ender")));
        assertTrue(GuideBook.load(Language.ENGLISH).search("&e").isEmpty(), "Color codes must not be searchable");
        assertTrue(GuideBook.load(Language.ENGLISH).search("   ").isEmpty());
    }

    @Test
    void paginationCoversEveryLineExactlyOnce() {
        for (Topic topic : GuideBook.load(Language.SPANISH).getTopics()) {
            List<String> joined = new ArrayList<>();
            for (int page = 1; page <= topic.pageCount(); page++) {
                List<String> lines = topic.page(page);
                assertTrue(lines.size() <= GuideBook.PAGE_SIZE);
                joined.addAll(lines);
            }
            assertEquals(topic.lines(), joined, "Pagination lost lines in " + topic.id());
        }
    }

    @Test
    void pageNumbersAreClamped() {
        Topic topic = GuideBook.load(Language.ENGLISH).findTopic("router").orElseThrow();

        assertEquals(topic.page(1), topic.page(-5));
        assertEquals(topic.page(topic.pageCount()), topic.page(999));
        assertEquals(1, new Topic("x", "x", "x", List.of()).pageCount());
        assertTrue(new Topic("x", "x", "x", List.of()).page(1).isEmpty());
    }

    @Test
    void languageTokensAndLocalesResolve() {
        assertEquals(Language.SPANISH, Language.fromToken("ES").orElseThrow());
        assertEquals(Language.SPANISH, Language.fromToken("español").orElseThrow());
        assertEquals(Language.SPANISH, Language.fromToken("espanol").orElseThrow());
        assertEquals(Language.ENGLISH, Language.fromToken("english").orElseThrow());
        assertTrue(Language.fromToken("fr").isEmpty());
        assertTrue(Language.fromToken(null).isEmpty());

        assertEquals(Language.SPANISH, Language.fromLocale(Locale.forLanguageTag("es-ES")));
        assertEquals(Language.SPANISH, Language.fromLocale(Locale.forLanguageTag("es-MX")));
        assertEquals(Language.SPANISH, Language.fromLocale(Locale.forLanguageTag("es-CL")));
        assertEquals(Language.ENGLISH, Language.fromLocale(Locale.US));
        assertEquals(Language.ENGLISH, Language.fromLocale(Locale.FRANCE));
        assertEquals(Language.ENGLISH, Language.fromLocale(null));
        assertEquals(Language.ENGLISH, Language.SPANISH.other());
        assertEquals(Language.SPANISH, Language.ENGLISH.other());
    }

    @Test
    void stripColorsRemovesLegacyCodes() {
        assertEquals("Hello world", GuideBook.stripColors("&6Hello &l&fworld"));
        assertFalse(COLOR_CODE.matcher(GuideBook.stripColors("&aA&bB&cC")).find());
    }
}
