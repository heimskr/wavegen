package wavegen.gameboy

import wavegen._
import chisel3._
import chisel3.util._

class GameBoy(data: Seq[Byte])(implicit clockFreq: Int) extends Module {
	val io = IO(new Bundle {

	})

	val clocker = Module(new Clocker)
	clocker.io.enable := true.B
	clocker.io.freq   := 4_194_304.U

	val stateMachine = Module(new StateMachine(data))
	stateMachine.io.tick := clocker.io.tick

	val channel1 = Module(new Channel1)
	channel1.io.tick := clocker.io.tick
	channel1.io.registers := stateMachine.io.registers
}
