/**
  * Created by ms on 16.11.16.
  */

/*
Parsing command line options
 */
case class Config(option1: String)

object DisEventSys extends App{

  val optionsParser = new scopt.OptionParser[Config]("diseventsys"){
    head("diseventsys", "1.0")

    opt[String]('o', "option1").action( (x, c) =>
      c.copy(option1 = x) ).text("Option 1 is ....")

    help("help").text("prints this usage text")

    //cmd("update").action( (_, c) => c.copy(mode = "update") ).
    //  text("update is a command.").children(???)

  }

  override def main(args: Array[String]) = {
    optionsParser.parse(args, Config(???)) match {
      case Some(config) => ???
      case None         => ???
    }
  }
}
