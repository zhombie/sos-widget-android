package kz.gov.mia.sos.widget.utils

internal fun formatEachWithIndex(vararg args: String): String =
    args.mapIndexed { index, s -> "${(index + 1)}. $s" }
        .joinToString(separator = "\n")