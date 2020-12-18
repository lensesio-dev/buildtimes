package io.lenses.buildmetrics

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {
  def run(args: List[String]) =
    IO(println("hi!")).as(ExitCode.Success)
}
