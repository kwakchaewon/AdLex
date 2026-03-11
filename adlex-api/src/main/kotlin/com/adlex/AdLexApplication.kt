package com.adlex

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AdLexApplication

fun main(args: Array<String>) {
    runApplication<AdLexApplication>(*args)
}
