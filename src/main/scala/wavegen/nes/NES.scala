package wavegen.nes

import wavegen._
import chisel3._
import chisel3.util._

object NES {
	val cpuFreq = 1_789_772
	val simulationFreq = 25_000_000
}

class NES(addressWidth: Int, romWidth: Int)(implicit clockFreq: Int, inSimulator: Boolean) extends Module {
	val slowFreq = if (inSimulator) NES.simulationFreq else NES.cpuFreq

	val io = IO(new Bundle {
		val start   = Input(Bool())
		val rom     = Input(UInt(romWidth.W))
		val sw      = Input(UInt(8.W))
		val buttonU = Input(Bool())
		val buttonR = Input(Bool())
		val buttonD = Input(Bool())
		val buttonL = Input(Bool())
		val buttonC = Input(Bool())
		val outL    = Output(UInt(10.W))
		val outR    = Output(UInt(10.W))
		val addr    = Output(UInt(addressWidth.W))
		val leds    = Output(UInt(8.W))
		val error   = Output(UInt(4.W))
	})

	val cpuClocker   = Module(new StaticClocker(slowFreq, clockFreq))
	val frameCounter = Module(new FrameCounter)
	val stateMachine = Module(new NESStateMachine(addressWidth, romWidth))
	val channel1     = Module(new Channel1)
	// val channel2     = Module(new Channel2)
	// val channel3     = Module(new Channel3)
	// val channel4     = Module(new Channel4)
	// val channel5     = Module(new Channel5)

	val ticks = frameCounter.io.ticks

	channel1.io.ticks     := ticks
	channel1.io.registers := stateMachine.io.registers
}
