package net.kryunek.hub.managers.tablist;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kryunek.hub.Celest;
import net.kryunek.hub.managers.module.ModuleService;
import net.kryunek.hub.managers.player.Profile;
import net.kryunek.hub.managers.rank.IRank;
import net.kryunek.hub.utils.CC;
import net.kryunek.hub.utils.FileConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TablistManager {

    private static final LegacyComponentSerializer SERIALIZADOR = LegacyComponentSerializer.legacySection();
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private final Celest plugin;
    private final FileConfig configuracion;
    private final IRank gestorRangos;

    private BukkitTask tarea;

    private final long intervaloActualizacion;
    private final List<String> cabecera;
    private final List<String> pie;
    private final String formatoNombre;
    private final String formatoPrefijoNametag;
    private final String formatoSufijoNametag;
    private final List<String> ordenGrupos;
    private final boolean headerFooterHabilitado;
    private final boolean formatoNombreHabilitado;
    private final boolean mostrarNametag;
    private final boolean ordenarPorGrupo;

    public TablistManager(Celest plugin) {
        this.plugin = plugin;
        this.configuracion = ModuleService.getFileModule().getFile("tab");
        this.gestorRangos = ModuleService.getManagerModule().getRankManager().getRank();
        this.intervaloActualizacion = Math.max(1L, configuracion.getLong("update-interval"));
        this.headerFooterHabilitado = configuracion.getBoolean("header-footer.enabled");
        this.formatoNombreHabilitado = configuracion.getBoolean("tablist-name-formatting.enabled");
        this.cabecera = new ArrayList<>(obtenerListaConFallback("header-footer.header", "header"));
        this.pie = new ArrayList<>(obtenerListaConFallback("header-footer.footer", "footer"));
        this.formatoNombre = obtenerTexto("tablist-name-formatting.format", "{lp_prefix}%player%{lp_suffix}");
        this.formatoPrefijoNametag = obtenerTexto("nametag.prefix", "{lp_prefix}");
        this.formatoSufijoNametag = obtenerTexto("nametag.suffix", "{lp_suffix}");
        this.mostrarNametag = configuracion.getBoolean("nametag.enabled");
        this.ordenarPorGrupo = configuracion.getBoolean("group-sorting.enabled");
        this.ordenGrupos = new ArrayList<>(obtenerLista("group-sorting.groups"));
    }

    public void iniciar() {
        detener();

        this.tarea = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Collection<? extends Player> jugadores = Bukkit.getOnlinePlayers();
            if (jugadores.isEmpty()) {
                return;
            }

            for (Player jugador : jugadores) {
                if (!debeVerTablist(jugador)) {
                    limpiarTablist(jugador);
                } else if (headerFooterHabilitado) {
                    actualizarCabeceraYPie(jugador);
                }

                if (formatoNombreHabilitado) {
                    actualizarNombreEnTab(jugador);
                }
            }

            if (mostrarNametag || ordenarPorGrupo) {
                for (Player visor : jugadores) {
                    sincronizarEquipos(visor, jugadores);
                }
            }
        }, 20L, intervaloActualizacion);
    }

    public void detener() {
        if (tarea != null) {
            tarea.cancel();
            tarea = null;
        }
    }

    private void actualizarCabeceraYPie(Player jugador) {
        Component header = deserializar(unirLineas(cabecera, jugador));
        Component footer = deserializar(unirLineas(pie, jugador));
        jugador.sendPlayerListHeaderAndFooter(header, footer);
    }

    private void actualizarNombreEnTab(Player jugador) {
        String texto = resolverPlaceholders(formatoNombre, jugador);
        jugador.playerListName(deserializar(texto));
    }

    private void restaurarNombreTab(Player jugador) {
        jugador.playerListName(Component.text(jugador.getName()));
    }

    public void limpiarTablist(Player jugador) {
        jugador.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
    }

    public void limpiarVisuales(Player jugador) {
        limpiarTablist(jugador);
        limpiarNametag(jugador);
    }

    public void limpiarNametag(Player jugador) {
        Scoreboard scoreboard = jugador.getScoreboard();
        if (scoreboard == null) {
            return;
        }

        for (Team team : new ArrayList<>(scoreboard.getTeams())) {
            if (team.getName().startsWith("tab")) {
                team.unregister();
            }
        }
    }

    private void sincronizarEquipos(Player visor, Collection<? extends Player> jugadores) {
        Scoreboard scoreboard = obtenerScoreboardSeguro(visor);
        limpiarEquiposTab(scoreboard, jugadores);

        for (Player objetivo : jugadores) {
            String nombreEquipo = construirNombreEquipo(objetivo);
            Team team = scoreboard.getTeam(nombreEquipo);

            if (team == null) {
                team = scoreboard.registerNewTeam(nombreEquipo);
            }

            for (String entry : new ArrayList<>(team.getEntries())) {
                if (!entry.equals(objetivo.getName())) {
                    team.removeEntry(entry);
                }
            }

            if (!team.hasEntry(objetivo.getName())) {
                team.addEntry(objetivo.getName());
            }

            if (mostrarNametag) {
                String prefixTexto = resolverPlaceholders(formatoPrefijoNametag, objetivo, true);
                String suffixTexto = resolverPlaceholders(formatoSufijoNametag, objetivo, true);

                team.prefix(deserializar(prefixTexto));
                team.suffix(deserializar(suffixTexto));
                team.setColor(obtenerColorNombreDesdePrefix(prefixTexto));
            } else {
                team.prefix(Component.empty());
                team.suffix(Component.empty());
                team.setColor(ChatColor.RESET);
            }
        }
    }

    private void limpiarEquiposTab(Scoreboard scoreboard, Collection<? extends Player> jugadores) {
        List<String> nombresActuales = jugadores.stream().map(this::construirNombreEquipo).collect(Collectors.toList());
        for (Team team : new ArrayList<>(scoreboard.getTeams())) {
            if (team.getName().startsWith("tab") && !nombresActuales.contains(team.getName())) {
                team.unregister();
            }
        }
    }

    private Scoreboard obtenerScoreboardSeguro(Player jugador) {
        Scoreboard actual = jugador.getScoreboard();
        if (actual == null || actual == Bukkit.getScoreboardManager().getMainScoreboard()) {
            actual = Bukkit.getScoreboardManager().getNewScoreboard();
            jugador.setScoreboard(actual);
        }
        return actual;
    }

    private String construirNombreEquipo(Player jugador) {
        int prioridad = obtenerPrioridadGrupo(jugador);
        String identificador = jugador.getUniqueId().toString().replace("-", "");
        identificador = identificador.substring(0, 8);
        return String.format("tab%05d%s", prioridad, identificador);
    }

    private int obtenerPrioridadGrupo(Player jugador) {
        if (!ordenarPorGrupo) {
            return 99999;
        }

        String grupo = obtenerGrupo(jugador).toLowerCase(Locale.ROOT);
        for (int i = 0; i < ordenGrupos.size(); i++) {
            if (ordenGrupos.get(i).equalsIgnoreCase(grupo)) {
                return i;
            }
        }

        return 99999;
    }

    private String obtenerGrupo(Player jugador) {
        try {
            return valorSeguro(gestorRangos.getName(jugador.getUniqueId()));
        } catch (Exception exception) {
            return "default";
        }
    }

    private String unirLineas(List<String> lineas, Player jugador) {
        if (lineas.isEmpty()) {
            return "";
        }

        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < lineas.size(); i++) {
            if (i > 0) {
                texto.append('\n');
            }
            texto.append(resolverPlaceholders(lineas.get(i), jugador));
        }
        return texto.toString();
    }

    private String resolverPlaceholders(String texto, Player jugador) {
        return resolverPlaceholders(texto, jugador, false);
    }

    private String resolverPlaceholders(String texto, Player jugador, boolean limpiarResetMeta) {
        String prefijo = "";
        String sufijo = "";
        String rango = "default";

        try {
            rango = valorSeguro(gestorRangos.getName(jugador.getUniqueId()));
            prefijo = valorSeguro(gestorRangos.getPrefix(jugador.getUniqueId()));
            sufijo = valorSeguro(gestorRangos.getSuffix(jugador.getUniqueId()));
        } catch (Exception ex) {
            Bukkit.getLogger().fine("[Celest] Failed to resolve rank placeholders for " + jugador.getName() + ": " + ex.getMessage());
        }

        if (limpiarResetMeta) {
            prefijo = normalizarMetaNametag(prefijo);
            prefijo = mantenerColorParaNombre(prefijo);
            sufijo = limpiarCodigoReset(sufijo);
        }

        return colorearHex(texto
                .replace("%player%", jugador.getName())
                .replace("%displayname%", jugador.getDisplayName())
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%max_online%", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("%rank%", rango)
                .replace("{lp_prefix}", prefijo)
                .replace("{lp_suffix}", sufijo)
                .replace("%prefix%", prefijo)
                .replace("%suffix%", sufijo));
    }

    private String limpiarCodigoReset(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replaceAll("(?i)(?:&|\\u00A7)r", "");
    }

    private String normalizarMetaNametag(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String normalized = text
                .replaceAll("(?i)<\\s*reset\\s*>", "")
                .replaceAll("(?i)<\\s*r\\s*>", "");
        return limpiarCodigoReset(normalized);
    }

    private String mantenerColorParaNombre(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return "";
        }

        String translated = CC.translate(colorearHex(prefix));
        String lastColors = ChatColor.getLastColors(translated);
        if (lastColors == null || lastColors.isEmpty()) {
            return prefix;
        }

        return prefix + lastColors;
    }

    private ChatColor obtenerColorNombreDesdePrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return ChatColor.WHITE;
        }

        String translated = CC.translate(colorearHex(prefix));
        String lastColors = ChatColor.getLastColors(translated);
        if (lastColors == null || lastColors.isEmpty()) {
            return ChatColor.WHITE;
        }

        for (int i = lastColors.length() - 1; i >= 1; i--) {
            if (lastColors.charAt(i - 1) != '\u00A7') {
                continue;
            }

            ChatColor candidate = ChatColor.getByChar(lastColors.charAt(i));
            if (candidate == null) {
                continue;
            }
            if (candidate == ChatColor.RESET) {
                return ChatColor.WHITE;
            }
            if (candidate.isColor()) {
                return candidate;
            }
        }

        return ChatColor.WHITE;
    }

    private String valorSeguro(String texto) {
        return texto == null ? "" : texto;
    }

    private String obtenerTexto(String ruta, String defecto) {
        return configuracion.getString(ruta, defecto, true);
    }

    private List<String> obtenerLista(String ruta) {
        List<String> lista = configuracion.getStringList(ruta);
        if (lista.size() == 1 && "ERROR: STRING LIST NOT FOUND!".equals(lista.get(0))) {
            return List.of();
        }
        return lista;
    }

    private List<String> obtenerListaConFallback(String rutaPrincipal, String rutaFallback) {
        List<String> lista = obtenerLista(rutaPrincipal);
        if (!lista.isEmpty()) {
            return lista;
        }
        return obtenerLista(rutaFallback);
    }

    private String colorearHex(String texto) {
        String traducido = CC.translate(texto == null ? "" : texto);
        Matcher matcher = HEX_PATTERN.matcher(traducido);
        StringBuilder resultado = new StringBuilder();

        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(resultado, Matcher.quoteReplacement(aLegacyHex(hex)));
        }

        matcher.appendTail(resultado);
        return resultado.toString();
    }

    private String aLegacyHex(String hex) {
        StringBuilder builder = new StringBuilder("\u00A7x");
        for (char caracter : hex.toCharArray()) {
            builder.append('\u00A7').append(caracter);
        }
        return builder.toString();
    }

    private boolean debeVerTablist(Player jugador) {
        Profile profile = ModuleService.getManagerModule().getProfileManager().getProfile(jugador.getUniqueId());
        return profile == null || profile.isShowTablist();
    }

    private Component deserializar(String texto) {
        return SERIALIZADOR.deserialize(colorearHex(texto));
    }
}
