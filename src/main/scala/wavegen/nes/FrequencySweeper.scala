package wavegen.nes

import chisel3._
import chisel3.util._

class FrequencySweeper(twosComplement: Boolean) extends Module {
	override val desiredName = "NESFrequencySweeper"

	val io = IO(new Bundle {
		val ticks     = Input(Ticks())
		val register  = Input(UInt(8.W))
		val reload    = Input(Bool())
		val periodIn  = Input(UInt(11.W))
		val periodOut = Valid(UInt(3.W))
		val mute      = Output(Bool())
	})

	val enable = io.register(7)
	val period = io.register(6, 4) + 1.U
	val negate = io.register(3)
	val shift  = io.register(2, 0)

	val divider = RegInit(0.U(4.W)) // TODO: width?
	val enabled = RegInit(false.B)

	io.periodOut.bits  := DontCare
	io.periodOut.valid := false.B

	val target = Wire(UInt(11.W))
	val change = io.periodIn >> shift

	when (shift === 0.U) {
		target := io.periodIn
	} .elsewhen (negate) {
		if (twosComplement) {
			target := io.periodIn - change
		} else {
			target := io.periodIn - (change + 1.U)
		}
	} .otherwise {
		target := io.periodIn + change
	}

	val muting = RegInit(false.B)
	io.mute := muting

	when ("h7fff".U < target) {
		muting  := true.B
		io.mute := true.B
	}

	val reloadReg = RegInit(false.B)
	val reload    = WireInit(reloadReg)

	when (io.ticks.apu && io.reload) {
		reloadReg := true.B
		reload    := true.B
	}

	when (io.ticks.half) {
		val dividerZero = divider === 0.U

		when (dividerZero && enabled && !muting) {
			io.periodOut.bits  := target
			io.periodOut.valid := true.B
		}

		when (dividerZero || reload) {
			divider   := period
			reloadReg := false.B
		} .otherwise {
			divider := divider - 1.U
		}
	}
}
