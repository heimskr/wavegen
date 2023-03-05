package wavegen

import chisel3._
import chisel3.util._
import scala.math.round

class StaticClocker(wantedFrequency: Int, baseClockFreq: Int, moreAccurate: Boolean = false, moduleName: String = "StaticClocker") extends Module {
	override val desiredName = moduleName

	val period =
		if (moreAccurate)
			round(baseClockFreq / wantedFrequency.doubleValue()).toInt
		else
			baseClockFreq / wantedFrequency
	val width  = log2Ceil(period + 1)

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
