class Sample {

    fun sample() {
        val action: Bloder = Bloder.Bloder1
        print("")
        handle(action)
    }

    private fun handle(@When action: Bloder.Bloder1) {
        println("Foo2")
    }

    private fun handle(@When action: Bloder.Bloder2) {
        println("Foo1")
    }
}

sealed class Bloder {
    object Bloder1 : Bloder()
    object Bloder2 : Bloder()
}