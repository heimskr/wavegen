package wavegen.gameboy

import chisel3._
import chisel3.util._

class Envelope extends Module {
	val io = IO(new Bundle {
		val tick          = Input(Bool())
		val trigger       = Input(Bool())
		val initialVolume = Input(UInt(4.W))
		val rising        = Input(Bool())
		val period        = Input(UInt(3.W))
		val periodCounter = Output(UInt(8.W))
		val currentVolume = Output(UInt(4.W))
	})

	val periodCounter = RegInit(1.U(8.W))
	val currentVolume = RegInit(0.U(4.W))
	val changeEnable  = RegInit(false.B)

	// when (io.tick) {
	// 	when (io.trigger && io.period =/= 0.U) {
	// 		currentVolume := io.initialVolume

	// 		when (0.U < periodTimer) {
	// 			periodTimer := periodTimer - 1.U
	// 		}

	// 		when (periodTimer === 0.U) {
	// 			periodTimer := io.period

	// 			when (currentVolume < 15.U && io.rising) {
	// 				currentVolume := currentVolume + 1.U
	// 			} .elsewhen (0.U < currentVolume && !io.rising) {
	// 				currentVolume := currentVolume - 1.U
	// 			}
	// 		}
	// 	}
	// }

	when (io.tick && !io.trigger) {
		when (periodCounter < io.period - 1.U) {
			periodCounter := periodCounter + 1.U
		} .otherwise {
			periodCounter := 0.U
		}
	}

	when (io.trigger) {
		currentVolume := io.initialVolume
		changeEnable  := true.B
		periodCounter := Mux(1.U < io.period, 1.U, 0.U)
	}

	when (io.tick && io.period =/= 0.U && changeEnable && periodCounter === 0.U) {
		when (io.rising) {
			when (currentVolume < 15.U) {
				currentVolume := currentVolume + 1.U
			} .otherwise {
				changeEnable := false.B
			}
		} .otherwise {
			when (0.U < currentVolume) {
				currentVolume := currentVolume - 1.U
			} .otherwise {
				changeEnable := false.B
			}
		}
	}

	io.periodCounter := periodCounter
	io.currentVolume := currentVolume
}
