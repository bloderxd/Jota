import com.bloder.jota.When

class Sample {

    fun sample() {
        val action: Action = Action.ParsingError
        handleAction(action)

    }

    fun handle(@When action: Action.Success) {
        println("Success")
    }

    fun handle(@When action: Action.NetworkError) {
        println("NetworkError")
    }
}

sealed class Action {
    object Success : Action()
    object NetworkError : Action()
    object ParsingError : Action()
    object Other : Action()
}