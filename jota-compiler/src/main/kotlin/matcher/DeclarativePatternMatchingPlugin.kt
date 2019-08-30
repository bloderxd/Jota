package matcher

import arrow.meta.autofold.SealedSubclass
import matcher.patternmatching.PhaseFunctionOccurrence

internal object DeclarativePatternMatchingPlugin {
    val adt: MutableList<SealedSubclass> = mutableListOf()
    val functionOccurrences = mutableMapOf<String, PhaseFunctionOccurrence>()
}