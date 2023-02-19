package wavegen

import chisel3._
import chisel3.util._

class StaticClocker(wantedFrequency: Int, baseClockFreq: Int) extends Module {
	val period  = baseClockFreq / wantedFrequency
	val width = log2Ceil(period + 1)

	val io = IO(new Bundle {
		val enable  = Input(Bool())
		val tick    = Output(Bool())
		val counter = Output(UInt(width.W))
	})

	val counter = RegNext(0.U(width.W))

	io.tick := false.B

	when (io.enable) {
		when ((period - 1).U <= counter) {
			counter := 0.U
			io.tick := true.B
		} .otherwise {
			counter := counter + 1.U
		}
	}

	io.counter := counter
}
