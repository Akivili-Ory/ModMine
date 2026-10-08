package com.example.examplemod;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;

/*
 * Filtro usado pelo /leilao procurar.
 *
 * 1) Se a busca for uma categoria (ex.: "madeira"),
 *    mostra todos os itens dessa categoria.
 *
 * 2) Senão, procura o texto dentro do ID do item
 *    (ex.: "log" encontra oak_log, birch_log...)
 *    ou dentro do nome do item.
 */
public class LeilaoFiltro {

    /*
     * Categorias em português.
     * Para criar outra, é só adicionar uma linha aqui.
     */
    private static final Map<String, Predicate<ItemStack>> CATEGORIAS = new LinkedHashMap<>();

    static {

        CATEGORIAS.put("madeira",
                s -> s.is(ItemTags.LOGS) || s.is(ItemTags.PLANKS));

        CATEGORIAS.put("minerio",
                s -> s.is(Tags.Items.ORES));

        CATEGORIAS.put("ferramentas",
                s -> s.is(ItemTags.SWORDS)
                        || s.is(ItemTags.PICKAXES)
                        || s.is(ItemTags.AXES)
                        || s.is(ItemTags.SHOVELS)
                        || s.is(ItemTags.HOES));

        CATEGORIAS.put("armaduras",
                s -> s.is(ItemTags.HEAD_ARMOR)
                        || s.is(ItemTags.CHEST_ARMOR)
                        || s.is(ItemTags.LEG_ARMOR)
                        || s.is(ItemTags.FOOT_ARMOR));

        CATEGORIAS.put("comida",
                s -> s.has(DataComponents.FOOD));
    }

    private final String descricao;
    private final Predicate<ItemStack> predicado;

    private LeilaoFiltro(String descricao, Predicate<ItemStack> predicado) {
        this.descricao = descricao;
        this.predicado = predicado;
    }

    /*
     * Cria o filtro a partir do que o jogador digitou.
     */
    public static LeilaoFiltro de(String entrada) {

        String texto = normalizar(entrada);

        // Aceita "minecraft:oak_log" também
        if (texto.contains(":")) {
            texto = texto.substring(texto.lastIndexOf(':') + 1);
        }

        Predicate<ItemStack> categoria = CATEGORIAS.get(texto);

        if (categoria != null) {
            return new LeilaoFiltro(entrada.trim(), categoria);
        }

        final String busca = texto;
        final String buscaId = texto.replace(' ', '_');

        return new LeilaoFiltro(entrada.trim(), stack -> {

            String id = BuiltInRegistries.ITEM
                    .getKey(stack.getItem())
                    .getPath();

            return id.contains(buscaId)
                    || normalizar(stack.getHoverName().getString()).contains(busca);
        });
    }

    public boolean aceita(ItemStack stack) {
        return predicado.test(stack);
    }

    public String getDescricao() {
        return descricao;
    }

    /*
     * Nomes das categorias (usado nas sugestões do comando).
     */
    public static List<String> categorias() {
        return new ArrayList<>(CATEGORIAS.keySet());
    }

    /*
     * Deixa tudo minúsculo e sem acentos:
     * "Minério" -> "minerio"
     */
    private static String normalizar(String texto) {

        return Normalizer
                .normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}