package com.datn.pc_store.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtils {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern MULTIDASH = Pattern.compile("-+");

    private SlugUtils() {
    }

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD);
        String noAccents = normalized.replaceAll("\\p{M}", "");

        // Xử lý riêng ký tự Đ/đ trong tiếng Việt
        noAccents = noAccents.replace('đ', 'd').replace('Đ', 'D');

        String nowhitespace = WHITESPACE.matcher(noAccents).replaceAll("-");
        String slug = NONLATIN.matcher(nowhitespace).replaceAll("");
        slug = MULTIDASH.matcher(slug).replaceAll("-");

        return slug.toLowerCase(Locale.ROOT).replaceAll("^-|-$", "");
    }
}
