package de.tud.disteventsys.event

import scala.beans.BeanProperty

/**
  * Created by ms on 23.11.16.
  */
case class Buy(@BeanProperty symbol: String, @BeanProperty price: Double, @BeanProperty amount: Long)