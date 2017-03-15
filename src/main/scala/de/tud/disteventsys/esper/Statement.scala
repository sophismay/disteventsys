package de.tud.disteventsys.esper

import de.tud.disteventsys.event.Event.{Buy, Price, Sell}

/**
  * Created by ms on 02.01.17.
  */
// Represents the epl String and methods to obtain information from the epl string
// such as events in the string
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
  def initEpl(eplStringBuilder: StringBuilder) = {
    // populate events from StringBuilder, in order
    eplStatement = eplStringBuilder.mkString
    eplEvents = (allEvents filter { evtStr => eplStatement.contains(evtStr)}).toList
    firstEvent = eplEvents(0)
  }

  // get a map of all events in the epl statement string
  def getAllEvents: Map[String, Class[_]] = {
    classMappings filterKeys { clz => eplEvents.contains(clz) }
  }
  def getEventsList = eplEvents
  def getEplStatement = eplStatement
  // get the event to be associated with actor
  // the event that has to be acted on
  // eg. Insert into Buy. Buy is the first(responsible) event
  def getResponsibleEvent = {
    firstEvent
  }
}