// Credit for OldDebouncer: https://github.com/schoeberl/chisel-examples/blob/master/src/main/scala/util/Debounce.scala

package wavegen

import chisel3._
import chisel3.util._

class PBDebouncer extends BlackBox {
	val io = IO(new Bundle {
		val clk   = Input(Clock())
		val btn   = Input(Bool())
		val rst   = Input(Reset())
		val state = Output(Bool())
		val down  = Output(Bool())
		val up    = Output(Bool())
	})
}

class Debouncer(buttonCount: Int = 1) extends Module {
	override val desiredName = "Debouncer" + buttonCount

	val io = IO(new Bundle {
		val in  = Input(Vec(buttonCount, Bool()))
		val out = Output(Vec(buttonCount, Bool()))
	})

	val pulse = Reg(Vec(buttonCount, Bool()))

	io.in.zipWithIndex.foreach { case (btn, i) =>
		val pb = Module(new PBDebouncer)
		pb.io.clk := clock
		pb.io.rst := reset
		pb.io.btn := !btn
		io.out(i) := false.B

		when (pb.io.state) {
			when (!pulse(i)) {
				io.out(i) := true.B
				pulse(i) := true.B
			}
		} .otherwise {
			pulse(i) := false.B
		}
	}
}
