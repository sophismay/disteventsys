package de.tud.disteventsys.event

import scala.beans.BeanProperty

/**
  * Created by ms on 30.01.17.
  */
object Event {
  case class EsperEvent(eventType: String, underlying: AnyRef)
  case class Price(@BeanProperty symbol: String, @BeanProperty price: Double)
  case class Buy(@BeanProperty symbol: String, @BeanProperty price: Double, @BeanProperty amount: Long)
}
