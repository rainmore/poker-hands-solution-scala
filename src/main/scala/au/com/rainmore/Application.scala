package au.com.rainmore

import scala.io.StdIn
import scala.collection.mutable.ListBuffer

object Application extends App {

    private var player1: Int = 0
    private var player2: Int = 0

    readLines.foreach(line => {
        val cards = line.split(" ").map(el => {
            val num: Int = el.take(1) match {
                case "T" => 10
                case "J" => 11
                case "Q" => 12
                case "K" => 13
                case "A" => 14
                case other => other.toInt
            }

            Card(num, el.charAt(1))
        })

        val result = Hand(cards.take(5).sorted).compare(Hand(cards.takeRight(5).sorted))

        if (result > 0 ) player1 += 1
        else if (result < 0) player2 += 1
    })

    println("Player 1: %d hands".format(player1))
    println("Player 2: %d hands".format(player2))

    private def readLines: List[String] = {
        val arr = ListBuffer[String]()
        var ok = true
        while (ok) {
            val ln = StdIn.readLine()
            ok = Option(ln).isDefined
            if (ok) {
                arr += ln
            }
        }
        arr.toList
    }
}

case class Card(number: Int, suit: Char) extends Ordered[Card] {
    override def compare(that: Card): Int = this.number - that.number
}

case class Hand(cards: Seq[Card]) extends Ordered[Hand] {
    require(cards.size == 5)

    val numberGroup: Map[Int, Seq[Card]] = cards.groupBy(_.number)

    private val isStraight: Boolean = numberGroup.keys.sliding(2).map(_.reduceRight(_ - _)).forall(_ == -1)
    private val isFlush: Boolean = cards.groupBy(_.suit).keys.size == 1

    val rank: Rank.Value = {
        if (numberGroup.size == 2) {
            if (numberGroup.values.map(_.size).max == 4) Rank.FourOfAKind
            else Rank.FullHouse
        }
        else if (numberGroup.size == 3) {
            if (numberGroup.values.map(_.size).max == 3) Rank.ThreeOfAKind
            else Rank.TwoPairs
        }
        else if (numberGroup.size == 4) {
            Rank.Pair
        }
        else {
            if (isStraight && isFlush) {
                if (cards.head.number == 10) Rank.RoyalFlush
                else Rank.StraightFlush
            }
            else if (isStraight) Rank.Straight
            else if (isFlush) Rank.Flush
            else Rank.HighCard
        }
    }

    override def compare(that: Hand): Int = {
        val result = this.rank.compare(that.rank)
        if (result == 0) {

            this.rank match {
                case Rank.Straight | Rank.StraightFlush =>
                    this.numberGroup.keys.max - that.numberGroup.keys.max
                case Rank.FourOfAKind | Rank.ThreeOfAKind | Rank.FullHouse =>
                    this.numberGroup.maxBy(_._2.length)._1 - that.numberGroup.maxBy(_._2.length)._1
                case Rank.Pair | Rank.TwoPairs =>
                    val thisPairs = this.numberGroup.filter(_._2.length == 2).keys
                    val thatPairs = that.numberGroup.filter(_._2.length == 2).keys
                    val maxResult = thisPairs.max - thatPairs.max
                    val minResult = thisPairs.min - thatPairs.min
                    if (maxResult != 0) maxResult
                    else if (minResult != 0) minResult
                    else {
                        (for ((n, m) <- this.numberGroup.filter(_._2.length < 2).keys.toSeq.sorted
                            zip that.numberGroup.filter(_._2.length < 2).keys.toSeq.sorted)
                            yield n - m).filter(_ != 0).last
                    }
                case Rank.HighCard | Rank.Flush =>
                    (for ((n, m) <- this.cards.map(_.number) zip that.cards.map(_.number)) yield n - m).filter(_ != 0).last

            }
        }
        else result
    }
}

object Rank extends Enumeration {
    type Rank = Value
    val HighCard, Pair, TwoPairs, ThreeOfAKind, Straight, Flush, FullHouse, FourOfAKind, StraightFlush, RoyalFlush = Value
}
