# Jota
#### A way to uncouple algebraic data type checks

This project is still in beta and is being tested and improved.

```kotlin
sealed class Action {
  object Success
  object NetworkError
  object ParsingError
}

fun run(action: Action) = handleAction(action)
 
private fun handleAction(@When action: Action.Success) = println("Success")
private fun handleAction(@When action: Action.NetworkError) = println("NetworkError")
private fun handleAction(@When action: Action.ParsingError) = println("ParsingError")

run(Action.Success) // prints Success
run(Action.NetworkError) // prints NetworkError
run(Action.ParsingError) // prints ParsingError
```

Simply by using `@When` provided annotation in a type function parameter you can create a solution that implicitly infers the type and you don't need to worry about creating more code only to check them.

## Motivation

In Kotlin development is common to explicitly check types but sometimes a simple solution with that can transform the code in a really verbose one, let's create a simple example using `when` with the code above:

```kotlin
...

fun run(action: Action) {
  when(action) {
    is Action.Success -> handleAction(action)
    is Action.NetworkError -> handleAction(action)
    is Action.ParsingError -> handleAction(action)
    ...
    else -> {}
  }
}
```

Here we have the same behavior that we've created with Jota but with explicity type checking, why should we check every single type only to do the same thing? Well, we can say in this simple example we're actually writing a verbose code that check different types and create its handling, but this isn't just a simple useless code, this solution breaks a very important best practice principle.

## The problem

As said before this kind of explicitly type checking solution breaks a very important best practice, the `Open Closed Principle` that tell us that our code `should be opened for extension but closed for modification` and that means that we shouldn't have to modify our `run` function with something like `is Action.Type -> {}` every time we create more types but have a solution that we just care about extension like create new functions.

## Jota solution

Jota provides a `@When` annotation that implicitly infers types for you then you can just worry about create extensible solutions:

```kotlin
fun run(action: Action) = handleAction(action)

private fun handleAction(@When action: Action.Success) = println("Success")
private fun handleAction(@When action: Action.NetworkError) = println("NetworkError")
private fun handleAction(@When action: Action.ParsingError) = println("ParsingError")

run(Action.Success) // prints Success
run(Action.NetworkError) // prints NetworkError
run(Action.ParsingError) // prints ParsingError
```

then now if you have more types, you just need to worry about create new functions with its handling and not worry about modify any code only to add one more type checking, respecting the open closed principle:

```kotlin
...

private fun handleAction(@When action: Action.JsonFieldError) = println("JsonFieldError")

run(Action.JsonFieldError) // prints JsonFieldError
```

## Internals

Jota uses a mocked abstraction of [Arrow Meta Prototype](https://github.com/47deg/arrow-meta-prototype) to resolve compiler plugin internals and will be correctly imported when it's released.

## Contributions

Contributions are welcome, bugs and features should be reported by issues in this repository.

## Import

In your buildScript dependency config add:

```groovy
dependencies {
  classpath "com.bloder:jota-plugin:0.0.1"
}
```
And in your build gradle dependency 
```groovy
apply plugin: "jota-plugin"

dependencies {
  implementation "com.bloder:jota:0.0.1"
}
```
