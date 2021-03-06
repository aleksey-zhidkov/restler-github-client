package org.restler.github_client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.module.paranamer.ParanamerModule
import org.restler.Restler
import org.restler.spring.mvc.SpringMvcSupport
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.async.DeferredResult

class UserRepo @JsonCreator constructor(
        val name: String,
        val fork: Boolean,
        val language: String?)

@RestController
open class GitHub {

    @RequestMapping("/users/{userName}/repos")
    open fun userRepos(@PathVariable userName: String): List<UserRepo> = emptyList()

    @RequestMapping("/repos/{userName}/{repo}/languages")
    open fun repoLanguages(@PathVariable userName: String, @PathVariable repo: String): DeferredResult<Map<String, Int>> =
            DeferredResult()
}

@Suppress("UNCHECKED_CAST")
fun main(args: Array<String>) {
    val springMvcSupport = SpringMvcSupport().
            addJacksonModule(ParanamerModule())
    val github = Restler("https://api.github.com/", springMvcSupport).
            build().
            produceClient(GitHub::class.java)

    val userName = if (args.size() > 0) args[0] else "aleksey-zhidkov"
    val userRepos = github.userRepos(userName)

    val userLngs = userRepos.
            filter { it.language != null && !it.fork }.
            map { Pair(it.name, github.repoLanguages(userName, it.name)) }.
            flatMap {
                val (name, res) = it
                while (!res.hasResult()) Thread.sleep(100)
                println("$name: ${res.result}")
                (res.result as Map<String, Int>).entrySet() }.
            groupBy { it.key }.
            map { Pair(it.key, it.value.sumBy { it.value }) }.
            sortedByDescending { it.second }

    println("User languages: $userLngs")
}