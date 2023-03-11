package wavegen

import chisel3._
import chisel3.util._

class Pulser extends Module {
	val io = IO(new Bundle {
		val in  = Input(Bool())
		val out = Output(Bool())
	})

	val pb = Module(new PBDebouncer)
	pb.io.clk := clock
	pb.io.rst := reset
	pb.io.btn := io.in

	val pulse = RegInit(false.B)

	io.out := false.B

	when (pb.io.state) {
		when (!pulse) {
			io.out := true.B
			pulse  := true.B
		}
	} .otherwise {
		pulse := false.B
	}
}

object Pulser {
	def apply(value: Bool): Bool = {
		val pulser = Module(new Pulser)
		pulser.io.in := value
		pulser.io.out
	}
}
