package wavegen.nes

import wavegen._
import chisel3._
import chisel3.util._

object NES {
	val cpuFreq = 1_789_772
	val simulationFreq = 25_000_000
}

class NES(addressWidth: Int, romWidth: Int, useInternalClock: Boolean = true)(implicit clockFreq: Int, inSimulator: Boolean) extends Module {
	val slowFreq = if (inSimulator) NES.simulationFreq else NES.cpuFreq

	val io = IO(new Bundle {
		val tick   = Input(Bool())
		val start  = Input(Bool())
		val rom    = Input(UInt(romWidth.W))
		val sw     = Input(UInt(8.W))
		val pulseU = Input(Bool())
		val pulseR = Input(Bool())
		val pulseD = Input(Bool())
		val pulseL = Input(Bool())
		val pulseC = Input(Bool())
		val outL   = Output(UInt(10.W))
		val outR   = Output(UInt(10.W))
		val addr   = Output(UInt(addressWidth.W))
		val leds   = Output(UInt(8.W))
	})

	val frameCounter = Module(new FrameCounter)
	val stateMachine = Module(new NESStateMachine(addressWidth, romWidth))
	val channel1     = Module(new Channel1)
	// val channel2     = Module(new Channel2)
	// val channel3     = Module(new Channel3)
	// val channel4     = Module(new Channel4)
	// val channel5     = Module(new Channel5)

	val cpuTick = if (useInternalClock) {
		val cpuClocker = Module(new StaticClocker(slowFreq, clockFreq, false, "NESCPUClocker"))
		cpuClocker.io.enable := true.B
		cpuClocker.io.tick
	} else io.tick

	stateMachine.io.start := io.pulseC
	stateMachine.io.tick  := cpuTick
	stateMachine.io.rom   := io.rom

	frameCounter.io.reload   := stateMachine.io.reloadFC
	frameCounter.io.cpuTick  := cpuTick
	frameCounter.io.register := stateMachine.io.registers.$4017

	val ticks = frameCounter.io.ticks

	channel1.io.ticks     := ticks
	channel1.io.registers := stateMachine.io.registers
	channel1.io.writes    := stateMachine.io.pulse1Writes

	io.leds := stateMachine.io.state
	io.outL := channel1.io.out
	io.outR := channel1.io.out
	io.addr := stateMachine.io.addr
}
