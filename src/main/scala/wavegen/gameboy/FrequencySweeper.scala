package wavegen.gameboy

import chisel3._
import chisel3.util._

class FrequencySweeper extends Module {
	val io = IO(new Bundle {
		val tick        = Input(Bool())
		val trigger     = Input(Bool())
		val period      = Input(UInt(3.W))
		val negate      = Input(Bool())
		val shift       = Input(UInt(3.W))
		val frequencyIn = Input(UInt(11.W))
		val out         = Output(UInt(11.W))
		val info        = Output(UInt(8.W))
		val nr14In      = Input(UInt(8.W))
		val nr13Out     = Valid(UInt(8.W))
		val nr14Out     = Valid(UInt(8.W))
	})

	val storedTrigger = RegInit(false.B)
	val sweepEnabled  = RegInit(false.B)
	val shadowFreq    = RegInit(0.U(11.W))
	val frequency     = RegInit(0.U(11.W))
	val sweepTimer    = RegInit(0.U(4.W))
	val periodCounter = RegInit(0.U(3.W))

	val info = RegInit(0.U(8.W))

	io.nr13Out.valid := false.B
	io.nr14Out.valid := false.B
	io.nr13Out.bits  := DontCare
	io.nr14Out.bits  := DontCare

	io.out := shadowFreq

	val newFrequency = Wire(UInt(11.W))

	when (io.negate) {
		newFrequency := shadowFreq - (shadowFreq >> io.shift)
	} .otherwise {
		newFrequency := shadowFreq + (shadowFreq >> io.shift)
	}

	when (io.trigger) {
		storedTrigger := true.B
	}

	when (io.tick) {
		when (io.trigger || storedTrigger) {
			shadowFreq    := io.frequencyIn
			periodCounter := 0.U
			sweepEnabled  := (io.period =/= 0.U) || (io.shift =/= 0.U)
			io.nr13Out.bits  := io.frequencyIn(7, 0)
			io.nr14Out.bits  := Cat(0.U(1.W), io.nr14In(6, 3), io.frequencyIn(10, 8))
			io.nr13Out.valid := true.B
			io.nr14Out.valid := true.B
			io.out := io.frequencyIn
			storedTrigger := false.B
		}

		val isZero = WireInit(false.B)

		when ((0.U < io.period) && (periodCounter < io.period - 1.U)) {
			periodCounter := periodCounter + 1.U
		} .otherwise {
			periodCounter := 0.U
			isZero := true.B
		}

		when (isZero && sweepEnabled) {
			io.nr13Out.bits  := newFrequency(7, 0)
			io.nr14Out.bits  := Cat(0.U(1.W), io.nr14In(6, 3), newFrequency(10, 8))
			io.nr13Out.valid := true.B
			io.nr14Out.valid := true.B
			shadowFreq := newFrequency
			io.out := newFrequency
		}
	}

	io.info := 0.U
}
