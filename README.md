# Weather Simulator

## Design Overview

This is a simple weather simulator that generates weather data continuously for a set of random locations in Earth.

The locations are selected randomly from the world cities dataset (included file [locations2.csv](data/locations2.csv)). 
The default is 20 locations, but it can be specified as execution argument to any number up to 10,533 (dataset size) 

A network of **WeatherSensor** Akka Actors, created for each location, generates weather events, and publish them to the weather
event bus (EventStream). The sensors report events at random intervals of time. They can also subscribe to nearby sensors and react
to their events, and trigger other events.
 
The **WeatherSensor** Actors uses a specific implementation of a **weathersim.WeatherEstimator** trait to estimate the weather variables.
There are two of such implementations: A basic one **weathersim.SuperSimpleWeatherEstimator**, and another **ImprovedWeatherEstimator** 
that extends the previous one with additional effects. They are mostly driven by some heuristics and random numbers.
  
The weather events are consumed by **weathersim.WeatherEventCollector** that simply prints to the standard output in the required format.


## How to run

`sbt run [number of locations]`
