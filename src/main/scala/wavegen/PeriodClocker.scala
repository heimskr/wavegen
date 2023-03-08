package wavegen

import chisel3._
import chisel3.util._

class PeriodClocker(width: Int = 16) extends Module {
	override val desiredName = "PeriodClocker" + width + "w"

	val io = IO(new Bundle {
		val tickIn  = Input(Bool())
		val period  = Input(UInt(width.W))
		val tickOut = Output(Bool())
	})

	val counter = RegInit(0.U(width.W))

	io.tickOut := false.B

	when (io.tickIn) {
		when (io.period - 1.U <= counter) {
			counter    := 0.U
			io.tickOut := true.B
		} .otherwise {
			counter := counter + 1.U
		}
	}
}
