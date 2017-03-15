# disteventsys #

## Synopsis ##
An event system for complex event processing with an SQL-like DSL for querying events. Tech used are Akka and Esper Correlation Engine.

## Usage Example ##
```<scala>
val dsl = QueryDSL()

// access event generators safely
val buy = BuyGenerator()
val price = PriceGenerator()
val sell = SellGenerator()
val fields = FieldsGenerator("symbol, price, 100")
    
val query1 = dsl INSERT buy SELECT fields FROM price WHERE { pg: PriceGenerator => pg.price := 5 }
val stream1 = query1.createStream

//create a query based on an existing query
// meaning when query1 events are fired, query2 will follow
val query2 = dsl INSERT buy SELECT fields FROM stream1
query2.createStream

```
## Getting Started ##
Import and Run as SBT project

## Features ##
* Esper Correlation Engine for Event Processing
* Actors handling fired Events
* SQL-like DSL 
```<scala>
 val query = dsl INSERT buy SELECT fields FROM price WHERE { g: SellGenerator => g.amount > 50 }
 query.createStream
 ```
## Future Work ##
* Extend DSL to incorporate joins and windows
* Capture failed events based on a timeout, store them on a backlog(storage) and replay when service is idle
