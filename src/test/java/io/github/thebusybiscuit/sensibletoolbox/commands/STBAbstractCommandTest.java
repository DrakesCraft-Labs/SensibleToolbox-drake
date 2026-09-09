package io.github.thebusybiscuit.sensibletoolbox.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import me.desht.dhutils.DHUtilsException;

class STBAbstractCommandTest {

    private ServerMock server;
    private TestCommand command;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        command = new TestCommand();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void resolvesRegisteredOnlinePlayerByExactName() {
        Player player = server.addPlayer("KnownPlayer");

        assertEquals(player.getUniqueId(), command.requireKnownPlayerID("KnownPlayer"));
    }

    @Test
    void rejectsUnknownPlayerWithCommandError() {
        DHUtilsException error = assertThrows(
            DHUtilsException.class,
            () -> command.requireKnownPlayerID("NeverJoinedPlayer")
        );

        assertEquals("Unknown player: NeverJoinedPlayer", error.getMessage());
    }

    @Test
    void rejectsUnregisteredUuid() {
        UUID unknownId = UUID.randomUUID();

        assertThrows(DHUtilsException.class, () -> command.requireKnownPlayerID(unknownId.toString()));
    }

    private static final class TestCommand extends STBAbstractCommand {

        private TestCommand() {
            super("test");
        }

        @Override
        public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
            return true;
        }
    }
}
