package io.github.thebusybiscuit.sensibletoolbox.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import io.github.thebusybiscuit.sensibletoolbox.core.guide.GuideBook;
import io.github.thebusybiscuit.sensibletoolbox.core.guide.GuideBook.Language;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

class GuideCommandTest {

    private ServerMock server;
    private GuideCommand command;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        command = new GuideCommand();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void registeredUnderStbGuideWithPublicPermission() {
        assertEquals(List.of("stb guide"), command.getLabels());
        assertTrue(command.matchesSubCommand("stb", new String[] { "guide" }));
        assertFalse(command.matchesSubCommand("stb", new String[] { "give" }));
    }

    @Test
    void indexFollowsTheClientLocale() {
        PlayerMock player = server.addPlayer();
        player.setLocale(Locale.forLanguageTag("es-MX"));

        command.execute(null, player, new String[0]);

        List<String> said = drain(player);
        assertTrue(said.get(0).contains(GuideBook.get(Language.SPANISH).ui("index_header")), said.get(0));
        assertTrue(said.stream().anyMatch(s -> s.contains("Primeros pasos")));
    }

    @Test
    void explicitLanguageOverridesTheLocale() {
        PlayerMock player = server.addPlayer();
        player.setLocale(Locale.forLanguageTag("es-ES"));

        command.execute(null, player, new String[] { "en" });

        assertTrue(drain(player).stream().anyMatch(s -> s.contains("Getting started")));
    }

    @Test
    void everyIndexEntryOpensItsTopicWhenClicked() {
        PlayerMock player = server.addPlayer();
        command.execute(null, player, new String[] { "es" });

        List<ClickEvent> clicks = new ArrayList<>();
        Component message;
        while ((message = player.nextComponentMessage()) != null) {
            if (message.clickEvent() != null) {
                clicks.add(message.clickEvent());
            }
        }

        for (GuideBook.Topic topic : GuideBook.get(Language.SPANISH).getTopics()) {
            assertTrue(clicks.contains(ClickEvent.runCommand("/stb guide " + topic.id() + " es")), "No clickable entry for " + topic.id());
        }
    }

    @Test
    void topicPagesShowTheirLinesAndClampOutOfRangePages() {
        PlayerMock player = server.addPlayer();
        GuideBook.Topic router = GuideBook.get(Language.ENGLISH).findTopic("router").orElseThrow();

        command.execute(null, player, new String[] { "router", "999", "en" });

        List<String> said = drain(player);
        String lastLine = GuideBook.stripColors(router.page(router.pageCount()).get(router.page(router.pageCount()).size() - 1));
        assertTrue(said.stream().anyMatch(s -> s.equals(lastLine)), "Last page was not shown");
        assertTrue(said.stream().anyMatch(s -> s.contains(router.pageCount() + "/" + router.pageCount())));
    }

    @Test
    void unknownTopicFallsBackToTheIndex() {
        PlayerMock player = server.addPlayer();
        command.execute(null, player, new String[] { "nope", "en" });

        List<String> said = drain(player);
        assertTrue(said.get(0).contains("nope"));
        assertTrue(said.stream().anyMatch(s -> s.contains("Getting started")));
    }

    @Test
    void searchListsMatchingTopics() {
        PlayerMock player = server.addPlayer();
        command.execute(null, player, new String[] { "search", "diamante", "es" });

        assertTrue(drain(player).stream().anyMatch(s -> s.contains("Almacenamiento Ender")));
    }

    @Test
    void consoleGetsTheEnglishGuide() {
        command.execute(null, server.getConsoleSender(), new String[0]);
        // The console has no locale; reaching this point without errors is enough.
        assertEquals(Language.ENGLISH, GuideCommand.detectLanguage(server.getConsoleSender()));
    }

    @Test
    void tabCompletesTopicsLanguagesAndPages() {
        PlayerMock player = server.addPlayer();

        List<String> first = command.onTabComplete(null, player, new String[] { "" });
        assertTrue(first.containsAll(List.of("start", "router", "search", "en", "es")));

        List<String> pages = command.onTabComplete(null, player, new String[] { "router", "" });
        assertTrue(pages.contains("1"));
        assertFalse(pages.contains("start"));
    }

    private static List<String> drain(PlayerMock player) {
        List<String> messages = new ArrayList<>();
        Component message;
        while ((message = player.nextComponentMessage()) != null) {
            messages.add(PlainTextComponentSerializer.plainText().serialize(message));
        }
        return messages;
    }
}
