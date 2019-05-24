package de.torbilicious

import org.http4k.client.ApacheClient
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.filter.ClientFilters.BasicAuth
import org.http4k.format.Jackson.auto
import kotlin.system.measureTimeMillis


class Item(
    val id: String,
    val name: String
) {
    override fun toString(): String {
        return "name=$name"
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            null -> false
            !is Item -> false
            else -> this.id == other.id
        }
    }
}

data class HeroInventory(
    val head: Item?,
    val neck: Item?,
    val torso: Item?,
    val shoulders: Item?,
    val legs: Item?,
    val waist: Item?,
    val hands: Item?,
    val bracers: Item?,
    val feet: Item?,
    val leftFinger: Item?,
    val rightFinger: Item?,
    val mainHand: Item?,
    val offHand: Item?
) {
    val items = listOf(
        head,
        neck,
        torso,
        shoulders,
        legs,
        waist,
        hands,
        bracers,
        feet,
        leftFinger,
        rightFinger,
        mainHand,
        offHand
    )
}

data class Player(val key: String, val data: List<DataEntry>)
data class DataEntry(val id: String, val number: Int?, val string: String?)
data class Row(
    val player: List<Player>,
    val data: List<DataEntry>,
    val order: Int
) {
    val riftLevel: Int by lazy { this.data.find { it.id == "RiftLevel" }?.number!! }
    private val realPlayer: Player by lazy { this.player.first() }
    val battleTag: String by lazy { this.realPlayer.data.find { it.id == "HeroBattleTag" }?.string!!.replace("#", "-") }
    val heroId: Int by lazy { this.realPlayer.data.find { it.id == "HeroId" }?.number!! }

    private val heroInfoUrl: String get() = "https://eu.api.blizzard.com/d3/profile/${this.battleTag}/hero/${this.heroId}/items?locale=en_US&access_token=$token"
    val items by lazy {
        val heroInventoryLens = Body.auto<HeroInventory>().toLens()
        val response = client(Request(GET, heroInfoUrl))
        heroInventoryLens(response).items
    }
}

data class LeaderboardResponse(val row: List<Row>)

data class TokenResonse(val access_token: String)

private val client = ApacheClient()

class GearMatcher {
    private val leaderboardUrl: String =
        "https://eu.api.blizzard.com/data/d3/season/17/leaderboard/rift-barbarian?access_token=$token"

    private val rowsLens = Body.auto<LeaderboardResponse>().toLens()

    init {
        println("Token: $token\n\n")

        val leaderboard = getLeaderboard()
        val firstPlayer = leaderboard.first()

        leaderboard.parallelStream().forEach {
            println("Tag: ${it.battleTag} | heroId: ${it.heroId}\n    ${it.items == firstPlayer.items}")
        }

        println(leaderboard.sumBy { it.items.size })


//        leaderboard.forEach {
//            val tag = it.battleTag
//            val heroId = it.heroId
//
//
//            println("Tag: $tag | heroId: $heroId")
//            println(it.items)
//            println()
//        }
    }

    private fun getLeaderboard(): List<Row> {
        val response = client(Request(GET, leaderboardUrl))
        val rows = rowsLens(response).row

        return rows.sortedBy { it.order }//.take(10)
    }
}


fun main() {
    val time = measureTimeMillis {
        GearMatcher()
    }

    println("\n\n\nTime taken: $time")
}

val token: String by lazy {
    val id = System.getenv("API_ID")
    val secret = System.getenv("API_SECRET")

    val tokenClientInfoStatus = ApacheClient().with(BasicAuth(id, secret))

    val tokenLens = Body.auto<TokenResonse>().toLens()
    val reponse = tokenClientInfoStatus(
        Request(POST, "https://us.battle.net/oauth/token").query(
            "grant_type",
            "client_credentials"
        )
    )
    tokenLens(reponse).access_token
}
