package matcher.adt

import arrow.meta.extensions.MetaComponentRegistrar
import arrow.meta.qq.classOrObject
import matcher.DeclarativePatternMatchingPlugin

internal val MetaComponentRegistrar.adt get() = with(DeclarativePatternMatchingPlugin) {
    meta(
        classOrObject(
            match = { true },
            map = { c ->
                if (sealedVariants.isNotEmpty()) adt.addAll(sealedVariants)
                listOf(c.text)
            }
        )
    )
}