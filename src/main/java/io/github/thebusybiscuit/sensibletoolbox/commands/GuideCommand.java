package io.github.thebusybiscuit.sensibletoolbox.commands;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import io.github.thebusybiscuit.sensibletoolbox.core.guide.GuideBook;
import io.github.thebusybiscuit.sensibletoolbox.core.guide.GuideBook.Language;
import io.github.thebusybiscuit.sensibletoolbox.core.guide.GuideBook.Topic;
import me.desht.dhutils.commands.AbstractCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * {@code /stb guide} shows the bilingual (English / Spanish) in-game guide.
 * <ul>
 * <li>{@code /stb guide} - topic index</li>
 * <li>{@code /stb guide <topic> [page]} - read a topic</li>
 * <li>{@code /stb guide search <term>} - find topics mentioning a term</li>
 * </ul>
 * A language argument ({@code en} / {@code es}) may be added anywhere; without one the
 * language follows the player's client locale.
 */
public class GuideCommand extends AbstractCommand {

    private static final String COMMAND = "/stb guide";
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    public GuideCommand() {
        super("stb guide");
        setPermissionNode("stb.commands.guide");
        setUsage(new String[] { "/<command> guide [en|es]", "/<command> guide <topic> [page] [en|es]", "/<command> guide search <term> [en|es]" });
    }

    @Override
    public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
        Language language = detectLanguage(sender);
        List<String> rest = new ArrayList<>();

        for (String arg : args) {
            Optional<Language> explicit = Language.fromToken(arg);
            if (explicit.isPresent()) {
                language = explicit.get();
            } else {
                rest.add(arg);
            }
        }

        GuideBook guide = GuideBook.get(language);

        if (rest.isEmpty() || isIndexKeyword(rest.get(0))) {
            showIndex(sender, guide);
        } else if (isSearchKeyword(rest.get(0))) {
            showSearch(sender, guide, String.join(" ", rest.subList(1, rest.size())));
        } else {
            Optional<Topic> topic = guide.findTopic(rest.get(0));

            if (topic.isPresent()) {
                int page = rest.size() > 1 ? parsePage(rest.get(1)) : 1;
                showTopic(sender, guide, topic.get(), page);
            } else {
                sender.sendMessage(LEGACY.deserialize(guide.ui("unknown_topic").replace("{topic}", rest.get(0))));
                showIndex(sender, guide);
            }
        }

        return true;
    }

    @Nonnull
    static Language detectLanguage(@Nonnull CommandSender sender) {
        if (sender instanceof Player player) {
            try {
                return Language.fromLocale(player.locale());
            } catch (RuntimeException | LinkageError x) {
                return Language.ENGLISH;
            }
        }

        return Language.ENGLISH;
    }

    private static boolean isIndexKeyword(@Nonnull String arg) {
        String a = arg.toLowerCase(Locale.ROOT);
        return a.equals("index") || a.equals("indice") || a.equals("índice") || a.equals("list") || a.equals("lista");
    }

    private static boolean isSearchKeyword(@Nonnull String arg) {
        String a = arg.toLowerCase(Locale.ROOT);
        return a.equals("search") || a.equals("buscar");
    }

    private static int parsePage(@Nonnull String arg) {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException x) {
            return 1;
        }
    }

    @ParametersAreNonnullByDefault
    private void showIndex(CommandSender sender, GuideBook guide) {
        sender.sendMessage(header(guide, guide.ui("index_header")));
        sender.sendMessage(LEGACY.deserialize(guide.ui("index_intro")));

        for (Topic topic : guide.getTopics()) {
            String cmd = COMMAND + " " + topic.id() + " " + guide.getLanguage().getCode();
            Component line = LEGACY.deserialize("&d• &e" + topic.title() + " &8- &7" + topic.summary())
                .clickEvent(ClickEvent.runCommand(cmd))
                .hoverEvent(HoverEvent.showText(LEGACY.deserialize(guide.ui("topic_hover") + "\n&8" + cmd)));
            sender.sendMessage(line);
        }

        sender.sendMessage(LEGACY.deserialize(guide.ui("index_hint")));
        sender.sendMessage(languageButton(guide, ""));
    }

    @ParametersAreNonnullByDefault
    private void showTopic(CommandSender sender, GuideBook guide, Topic topic, int requestedPage) {
        int page = topic.clampPage(requestedPage);
        int pages = topic.pageCount();
        String lang = guide.getLanguage().getCode();

        sender.sendMessage(header(guide, topic.title()));

        for (String line : topic.page(page)) {
            sender.sendMessage(LEGACY.deserialize(line));
        }

        Component nav = Component.empty();

        if (page > 1) {
            nav = nav.append(button(guide.ui("prev"), guide.ui("prev_hover"), COMMAND + " " + topic.id() + " " + (page - 1) + " " + lang)).append(Component.text(" "));
        }

        nav = nav.append(LEGACY.deserialize(guide.ui("page").replace("{page}", String.valueOf(page)).replace("{pages}", String.valueOf(pages))));

        if (page < pages) {
            nav = nav.append(Component.text(" ")).append(button(guide.ui("next"), guide.ui("next_hover"), COMMAND + " " + topic.id() + " " + (page + 1) + " " + lang));
        }

        nav = nav.append(Component.text(" ")).append(button(guide.ui("back_to_index"), guide.ui("back_to_index_hover"), COMMAND + " " + lang));
        sender.sendMessage(nav);
        sender.sendMessage(languageButton(guide, " " + topic.id() + " " + page));
    }

    @ParametersAreNonnullByDefault
    private void showSearch(CommandSender sender, GuideBook guide, String term) {
        if (term.isBlank()) {
            sender.sendMessage(LEGACY.deserialize(guide.ui("search_usage")));
            return;
        }

        List<Topic> results = guide.search(term);
        sender.sendMessage(header(guide, guide.ui("search_header").replace("{term}", term)));

        if (results.isEmpty()) {
            sender.sendMessage(LEGACY.deserialize(guide.ui("search_none").replace("{term}", term)));
        }

        for (Topic topic : results) {
            String cmd = COMMAND + " " + topic.id() + " " + guide.getLanguage().getCode();
            sender.sendMessage(LEGACY.deserialize("&d• &e" + topic.title() + " &8- &7" + topic.summary())
                .clickEvent(ClickEvent.runCommand(cmd))
                .hoverEvent(HoverEvent.showText(LEGACY.deserialize(guide.ui("topic_hover")))));
        }

        sender.sendMessage(button(guide.ui("back_to_index"), guide.ui("back_to_index_hover"), COMMAND + " " + guide.getLanguage().getCode()));
    }

    @Nonnull
    @ParametersAreNonnullByDefault
    private static Component header(GuideBook guide, String subtitle) {
        return LEGACY.deserialize("&8&m-----&r &6&l" + guide.getTitle() + " &8» &e" + subtitle + " &8&m-----");
    }

    @Nonnull
    @ParametersAreNonnullByDefault
    private static Component languageButton(GuideBook guide, String location) {
        Language other = guide.getLanguage().other();
        return button(guide.ui("switch_language"), guide.ui("switch_language_hover"), COMMAND + location + " " + other.getCode());
    }

    @Nonnull
    @ParametersAreNonnullByDefault
    private static Component button(String label, String hover, String command) {
        return LEGACY.deserialize(label).clickEvent(ClickEvent.runCommand(command)).hoverEvent(HoverEvent.showText(LEGACY.deserialize(hover).append(Component.newline()).append(Component.text(command, NamedTextColor.DARK_GRAY))));
    }

    @Override
    public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
        GuideBook guide = GuideBook.get(detectLanguage(sender));
        Set<String> options = new LinkedHashSet<>();
        String previous = args.length > 1 ? args[args.length - 2] : null;
        Optional<Topic> previousTopic = guide.findTopic(previous);

        if (previousTopic.isPresent() && previousTopic.get().id().equalsIgnoreCase(previous)) {
            for (int page = 1; page <= previousTopic.get().pageCount(); page++) {
                options.add(String.valueOf(page));
            }
        } else if (args.length == 1) {
            for (Topic topic : guide.getTopics()) {
                options.add(topic.id());
            }
            options.add("search");
        }

        for (Language language : Language.values()) {
            options.add(language.getCode());
        }

        String prefix = args.length == 0 ? "" : args[args.length - 1];
        return filterPrefix(sender, options, prefix);
    }
}
