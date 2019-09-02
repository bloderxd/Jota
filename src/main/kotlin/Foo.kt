import com.bloder.jota.When

sealed class Foo {
    object Foo1 : Foo()
    object Foo2 : Foo()
    object Foo3 : Foo()
}

fun handleAction(@When action: Action.Success) {
    println("Success")
}

fun handleAction(@When action: Action.NetworkError) {
    println("NetworkError")
}

fun handleAction(@When action: Action.ParsingError) {
    println("ParsingError")
}