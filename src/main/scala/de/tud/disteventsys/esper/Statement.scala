package de.tud.disteventsys.esper

import de.tud.disteventsys.event.Event.{Buy, Price, Sell}

/**
  * Created by ms on 02.01.17.
  */
class Statement {
  var eplStatement: String = ""
  var eplEvents: List[String] = List.empty
  // first event usually ...
  var firstEvent: String = ""
  private val classMappings: Map[String, Class[_]] = Map(
    "Buy" -> classOf[Buy],
    "Price" -> classOf[Price],
    "Sell" -> classOf[Sell]
  )
  private val allEvents = List("Buy", "Sell", "Price")
  //TODO: infer Responsible event/class from StringBuilder
  def initEpl(eplStringBuilder: StringBuilder) = {
    // populate events from StringBuilder, in order
    eplStatement = eplStringBuilder.mkString
    eplEvents = eplStatement.split("\n").flatMap(p => p.split(" ")).filter(p => allEvents.contains(p)).toList
    firstEvent = eplEvents(0)
    println(s"EVENTS IN EPLSTRING: ${eplEvents}")
  }

  // a query from Stream basically only needs to have the Event it writes to
  // together with the data (fields)
  def getFirstEventWithFields = {
    //(firstEvent, )
  }

  def getAllEvents: Map[String, Class[_]] = {
    classMappings filterKeys { clz => eplEvents.contains(clz) }
  }
  def getEplStatement = eplStatement
  // get the event to be associated with actor
  def getResponsibleEvent = {
    firstEvent
  }
}