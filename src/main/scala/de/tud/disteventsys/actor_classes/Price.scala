package de.tud.disteventsys.actor_classes

import scala.beans.BeanProperty

/**
  * Created by ms on 23.11.16.
  */
case class Price(@BeanProperty symbol: String, @BeanProperty price: Double)
