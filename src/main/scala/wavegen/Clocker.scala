package wavegen

import chisel3._
import chisel3.util._

class Clocker(implicit clockFreq: Int) extends Module {
	val width = log2Ceil(clockFreq + 1)

	val io = IO(new Bundle {
		val enable = Input(Bool())
		val freq   = Input(UInt(width.W))
		val tick   = Output(Bool())
		val period = Output(UInt(width.W))
	})

	val period = clockFreq.U / io.freq
	io.period := period
	
	val storedPeriod = Reg(UInt(width.W))
	val counter = RegNext(0.U(width.W))

	io.tick := false.B

	when (period < storedPeriod) {
		storedPeriod := period
		counter := 0.U
	} .elsewhen (storedPeriod < period) {
		storedPeriod := period
	}

	when (io.enable) {
		when (period <= counter + 1.U) {
			counter := 0.U
			io.tick := true.B
		} .otherwise {
			counter := counter + 1.U
		}
	}
}
