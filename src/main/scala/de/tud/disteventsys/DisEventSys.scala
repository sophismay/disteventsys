package de.tud.disteventsys

import de.tud.disteventsys.common.{BuyGenerator, FieldsGenerator, PriceGenerator, SellGenerator}
import de.tud.disteventsys.dsl.QueryDSL
/**
  * Created by ms on 23.11.16.
  */
object DisEventSys extends App {

  override def main(args: Array[String]) = {
    val dsl = QueryDSL()

    val buy = BuyGenerator()
    val price = PriceGenerator()
    val sell = SellGenerator()
    val fields = FieldsGenerator("symbol, price, 100")

    val query1 = dsl INSERT buy SELECT fields FROM price WHERE { pg: PriceGenerator => pg.price := 5 }
    val query2 = dsl INSERT buy SELECT fields FROM price WHERE { pg: PriceGenerator => pg.price > 4 }
    val query3 = dsl INSERT buy SELECT fields FROM sell WHERE { sg: SellGenerator => sg.symbol := "BP"}
    val stream1 = query2.createStream
    println(s"STREAM RETURNED: ${stream1.statement}")
    // now create stream from existing stream
    // stream1/dsl INSERT buy SELEcT fields FrOM stream1.events(f: Tuple => Boolean, timeout)
    val nextDsl =  dsl INSERT sell SELECT fields FROM stream1
    val stream2 = nextDsl.createStream
    println(s"STREAM 2: ${stream2}")
    
  }
}
