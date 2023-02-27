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
		shadowFreq    := io.frequencyIn
		periodCounter := 0.U
		sweepEnabled  := (io.period =/= 0.U) || (io.shift =/= 0.U)
		io.nr13Out.bits  := io.frequencyIn(7, 0)
		io.nr14Out.bits  := Cat(0.U(1.W), io.nr14In(6, 3), io.frequencyIn(10, 8))
		io.nr13Out.valid := true.B
		io.nr14Out.valid := true.B
		io.out := io.frequencyIn
	}

	when (io.tick) {
		val isZero = WireInit(false.B)

		when ((0.U < io.period) && (periodCounter < io.period - 1.U)) {
			periodCounter := periodCounter + 1.U
		} .otherwise {
			periodCounter := 0.U
			isZero := true.B
		}

		// TODO: investigate behavior with registers/wires
		when (isZero && sweepEnabled) {
			io.nr13Out.bits  := newFrequency(7, 0)
			io.nr14Out.bits  := Cat(io.nr14In(7, 3), newFrequency(10, 8))
			io.nr13Out.valid := true.B
			io.nr14Out.valid := true.B
			shadowFreq := newFrequency
			io.out := newFrequency
		}
	}

	// def calcFrequency(writeBack: Boolean): UInt = {
	// 	val newFrequency = Wire(UInt(12.W))

	// 	when (io.negate) {
	// 		newFrequency := shadowFreq - (shadowFreq >> io.shift)
	// 	} .otherwise {
	// 		newFrequency := shadowFreq + (shadowFreq >> io.shift)
	// 	}

	// 	when (2047.U < newFrequency) {
	// 		sweepEnabled := false.B
	// 	}

	// 	if (writeBack) {
	// 		io.nr13Out.bits  := newFrequency(7, 0)
	// 		io.nr14Out.bits  := Cat(io.nr14In(7, 3), newFrequency(10, 8))
	// 		io.nr13Out.valid := true.B
	// 		io.nr14Out.valid := true.B
	// 	}

	// 	newFrequency
	// }

	// val newInfo = Wire(UInt(8.W))

	// newInfo := 1.U

	// when (io.tick) {
	// 	newInfo := 2.U
	// 	when (0.U < sweepTimer) {
	// 		newInfo := 3.U
	// 		sweepTimer := sweepTimer - 1.U
	// 	}

	// 	when (sweepTimer === 0.U) {
	// 		newInfo := 4.U
	// 		when (0.U < io.period) {
	// 			newInfo := 5.U
	// 			sweepTimer := io.period
	// 		} .otherwise {
	// 			newInfo := 6.U
	// 			sweepTimer := 8.U
	// 		}

	// 		when (sweepEnabled && 0.U < io.period) {
	// 			newInfo := 7.U
	// 			val newFrequency = calcFrequency(true)
	// 			when (newFrequency <= 2047.U && 0.U < io.shift) {
	// 				newInfo := 8.U
	// 				frequency := newFrequency
	// 				shadowFreq := newFrequency
	// 				calcFrequency(false) // For overflow check
	// 			}
	// 		}
	// 	}
	// }

	// when (io.trigger) {
	// 	newInfo := 9.U
	// 	shadowFreq := io.frequencyIn

	// 	when (io.period === 0.U) {
	// 		newInfo := 10.U
	// 		sweepTimer := 8.U
	// 	} .otherwise {
	// 		newInfo := 11.U
	// 		sweepTimer := io.period
	// 	}

	// 	when (io.period =/= 0.U || io.shift =/= 0.U) {
	// 		newInfo := 12.U
	// 		sweepEnabled := true.B
	// 	}

	// 	when (io.shift =/= 0.U) {
	// 		newInfo := 13.U
	// 		calcFrequency(true)
	// 	}
	// }

	// when (newInfo =/= info) {
		// printf(cf"FrequencySweeper info: $info -> $newInfo\n")
	// 	info := newInfo
	// }

	// io.out := frequency
	// io.info := newInfo

	io.info := 0.U
}
