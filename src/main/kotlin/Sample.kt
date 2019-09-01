import com.bloder.jota.When

class Sample {

    fun sample() {
        val action: Action = Action.Success
        handle(action)
    }

    private fun handle(@When action: Action.Success) {
        println("Success")
    }

    private fun handle(@When action: Action.NetworkError) {
        println("NetworkError")
    }

    private fun handle(@When action: Action.ParsingError) {
        println("ParsingError")
    }
}

sealed class Action {
    object Success : Action()
    object NetworkError : Action()
    object ParsingError : Action()
    object Other : Action()
}