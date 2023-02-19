package wavegen

import chisel3._
import chisel3.util._

class DynamicClocker(implicit clockFreq: Int) extends Module {
	val width = log2Ceil(clockFreq + 1)

	val io = IO(new Bundle {
		val enable  = Input(Bool())
		val freq    = Flipped(Valid(UInt(width.W)))
		val tick    = Output(Bool())
		val period  = Valid(UInt(width.W))
		val counter = Output(UInt(width.W))
	})

	val divider = Module(new Divider(width))
	divider.io.in.bits.numerator   := clockFreq.U
	divider.io.in.bits.denominator := io.freq.bits
	val period = divider.io.out.bits.quotient
	val started = RegInit(false.B)

	val storedFreq = RegInit(0.U(width.W))
	val storedPeriod = Reg(UInt(width.W))

	// val activateDivider = WireInit(divider.io.out.valid)
	divider.io.in.valid := false.B

	when (io.freq.valid && (storedFreq =/= io.freq.bits || !started)) {
		storedFreq := io.freq.bits
		divider.io.in.valid := true.B
		started := true.B
	}

	io.period.bits := period

	val counter = RegNext(0.U(width.W))

	io.tick := false.B

	when (divider.io.out.valid) {
		when (period < storedPeriod) {
			storedPeriod := period
			counter := 0.U
		} .elsewhen (storedPeriod < period) {
			storedPeriod := period
		}
	}

	when (divider.io.out.valid) {
		when (io.enable) {
			when (period <= counter + 1.U) {
				counter := 0.U
				io.tick := true.B
			} .otherwise {
				counter := counter + 1.U
			}
		}

		io.period.valid := true.B
	} .otherwise {
		io.period.valid := false.B
	}

	io.counter := counter
}
