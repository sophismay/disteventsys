package de.tud.disteventsys.esper

/**
  * Created by ms on 02.01.17.
  */
trait Statement {
  var eplString: String
  var eplEvents: List[String] = List.empty
  // first event usually ...
  var firstEvent: String = ""
  private val allEvents = List("Buy", "Sell", "Price")
  //TODO: infer Responsible event/class from StringBuilder
  def initStatement(eplStringBuilder: StringBuilder) = {
    // populate events from StringBuilder, in order
    eplString = eplStringBuilder.mkString
    eplEvents = eplString.split("\n").flatMap(p => p.split(" ")).filter(p => allEvents.contains(p)).toList
    firstEvent = eplEvents(0)
    println(s"EVENTS IN EPLSTRING: ${eplEvents}")
  }
  // get the event to be associated with actor
  def getResponsibleEvent = {
    firstEvent
  }
}