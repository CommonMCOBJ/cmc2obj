# cmc2OBJ

> [!WARNING]
> May be unstable as CommonMCOBJ changes and parts are rewritten in Kotlin

cmc2OBJ is the reference implementation of the [CommonMCOBJ](https://github.com/CommonMCOBJ/CommonMCOBJ) spec. It is based on jmc2OBJ, but using [Kotlin](https://kotlinlang.org) as the language of choice.

Although cmc2OBJ is based off of jmc2OBJ and has many of its features, it's not intended to be used by artists, but rather serve as a reference implementation for developers. 

## Building
cmc2OBJ uses mavon and can be built with the following command:
```sh 
mvn clean compile assembly:single
```
