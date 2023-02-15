package wavegen.gameboy

import wavegen._
import chisel3._
import chisel3.util._

class GBEnvelope extends Module {
	val io = IO(new Bundle {
		val trigger       = Input(Bool())
		val initialVolume = Input(UInt(4.W))
		val rising        = Input(Bool())
		val period        = Input(UInt(3.W))
		val periodTimer   = Output(UInt(3.W))
		val currentVolume = Output(UInt(4.W))
	})

	val periodTimer   = RegInit(0.U(3.W))
	val currentVolume = RegInit(0.U(4.W))

	when (io.trigger && io.period =/= 0.U) {
		when (0.U < periodTimer) {
			periodTimer := periodTimer - 1.U
		}

		when (periodTimer === 0.U) {
			periodTimer := io.period

			when (currentVolume < 15.U && io.rising) {
				currentVolume := currentVolume + 1.U
			} .elsewhen (0.U < currentVolume && !io.rising) {
				currentVolume := currentVolume - 1.U
			}
		}
	}

	io.periodTimer   := periodTimer
	io.currentVolume := currentVolume
}
