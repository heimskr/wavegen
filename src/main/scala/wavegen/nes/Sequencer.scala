package wavegen.nes

import chisel3._
import chisel3.util._

class Sequencer extends Module {
	val io = IO(new Bundle {
		val timerValue = Input(UInt(11.W))
		val ticksIn    = Input(Ticks())
		val tickOut    = Output(Bool())
	})



	val timer = RegInit(0.U(11.W))
	val pulse = timer === 0.U

	when (io.ticksIn.apu) {
		when (pulse) {
			timer := io.timerValue
		} .otherwise {
			timer := timer - 1.U
		}
	}

	// "A period of t < 8, either set explicitly or via a sweep period update, silences the corresponding pulse channel." —NESdev Wiki
	io.tickOut := io.timerValue(10, 3).orR && pulse
	// io.tickOut := 8.U <= io.timerValue && pulse
	// TODO: compare above for area or maybe WNS
}
