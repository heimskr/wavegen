package wavegen.gameboy

import chisel3._
import chisel3.util._

class PeriodClocker(width: Int = 16) extends Module {
	val io = IO(new Bundle {
		val tickIn  = Input(Bool())
		val period  = Flipped(Valid(UInt(width.W)))
		val tickOut = Output(Bool())
	})

	val periodReg = RegInit(0.U(width.W))
	val counter   = RegInit(0.U(width.W))

	when (io.period.valid && io.period.bits =/= periodReg) {
		when (io.period.bits <= counter) {
			counter := 0.U
		}

		periodReg := io.period.bits
	}

	io.tickOut := false.B

	when (counter === periodReg - 1.U) {
		counter    := 0.U
		io.tickOut := true.B
	}
}
