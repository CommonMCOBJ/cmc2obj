# cmc2OBJ

> [!WARNING]
> May be unstable as CommonMCOBJ changes

cmc2OBJ is the reference implementation of the [CommonMCOBJ](https://github.com/CommonMCOBJ/CommonMCOBJ) spec. It is based on jmc2OBJ and uses both Java and Kotlin (although we are moving towards Kotlin to make it easier for developers to use).

Although cmc2OBJ is based off of jmc2OBJ and has many of its features, it's not intended to be a stable exporter, but rather serve as a reference implementation for developers.

## Building
cmc2OBJ uses mavon and can be built with the following command:
```sh 
mvn clean compile assembly:single
```
