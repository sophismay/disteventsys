# disteventsys

## Synopsis
A Distributed Event System for complex event processing with an SQL-like DSL for querying events. Tech used are Akka and Esper Correlation Engine.

## Usage Examples
```<scala>
val dsl = QueryDSL()
val buy = BuyGenerator()
val price = PriceGenerator()
val sell = SellGenerator()
val fields = FieldsGenerator("symbol, price, 100")
    
val query1 = dsl INSERT buy SELECT fields FROM price WHERE { pg: PriceGenerator => pg.price := 5 }
val stream1 = query1.createStream

```
## Running
Import and Run as SBT project

## Future Work
